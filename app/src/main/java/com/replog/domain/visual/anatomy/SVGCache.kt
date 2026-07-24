package com.replog.domain.visual.anatomy

import android.content.Context
import com.caverock.androidsvg.SVG
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

/**
 * Process-local LRU cache for parsed complete SVG documents.
 *
 * Both successful parses and failures are cached. Duplicate concurrent requests
 * for the same asset share one in-flight load; requests for different assets do
 * not block on disk/SVG parsing for each other. The cache is intentionally
 * process-lifetime because the assets are packaged application resources.
 */
object SVGCache {

    private const val DEFAULT_MAX_ENTRIES = 64

    private sealed interface CacheEntry {
        data class Success(val svg: SVG) : CacheEntry
        data object PlaceholderSuccess : CacheEntry // JVM-test-only success marker.
        data class Failure(val reason: String) : CacheEntry
    }

    private sealed interface CacheLookup {
        data class Immediate(val entry: CacheEntry) : CacheLookup
        data class Pending(val task: FutureTask<CacheEntry>, val owner: Boolean) : CacheLookup
    }

    private val lock = Any()
    private var maxEntries: Int = DEFAULT_MAX_ENTRIES
    private val inFlightLoads = mutableMapOf<String, FutureTask<CacheEntry>>()

    private val cache = object : LinkedHashMap<String, CacheEntry>(DEFAULT_MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            val shouldEvict = size > maxEntries
            if (shouldEvict && eldest != null) RendererDiagnostics.cacheEviction(eldest.key)
            return shouldEvict
        }
    }

    fun clear() {
        synchronized(lock) {
            cache.clear()
            inFlightLoads.clear()
        }
    }

    fun size(): Int = synchronized(lock) { cache.size }

    fun contains(assetPath: String): Boolean = synchronized(lock) { cache.containsKey(assetPath) }

    fun configureMaxEntries(limit: Int) {
        synchronized(lock) {
            maxEntries = limit.coerceAtLeast(1)
            trimToMaxSizeLocked()
        }
    }

    fun resetMaxEntries() {
        synchronized(lock) {
            maxEntries = DEFAULT_MAX_ENTRIES
            trimToMaxSizeLocked()
        }
    }

    fun get(context: Context, assetPath: String): SVG? {
        val normalizedPath = assetPath.trim()
        if (normalizedPath.isEmpty()) {
            RendererDiagnostics.missingSvgAsset(assetPath, "Blank asset path")
            return null
        }

        val entry = when (val lookup = lookup(normalizedPath) {
            FutureTask<CacheEntry> { loadEntry(context.applicationContext ?: context, normalizedPath) }
        }) {
            is CacheLookup.Immediate -> lookup.entry
            is CacheLookup.Pending -> runTaskAndResolve(normalizedPath, lookup)
        }
        return (entry as? CacheEntry.Success)?.svg
    }

    /** Test-friendly cache path that avoids Android Context and AndroidSVG parsing. */
    fun getOrLoad(assetPath: String, loader: () -> SvgCacheTestResult): Boolean {
        val normalizedPath = assetPath.trim()
        if (normalizedPath.isEmpty()) return false

        val entry = when (val lookup = lookup(normalizedPath) {
            FutureTask<CacheEntry> {
                when (loader()) {
                    SvgCacheTestResult.Success -> CacheEntry.PlaceholderSuccess
                    SvgCacheTestResult.Failure -> CacheEntry.Failure("JVM test failure placeholder")
                }
            }
        }) {
            is CacheLookup.Immediate -> lookup.entry
            is CacheLookup.Pending -> runTaskAndResolve(normalizedPath, lookup)
        }

        return when (entry) {
            is CacheEntry.Success, CacheEntry.PlaceholderSuccess -> true
            is CacheEntry.Failure -> false
        }
    }

    private fun lookup(
        assetPath: String,
        newTaskFactory: () -> FutureTask<CacheEntry>
    ): CacheLookup = synchronized(lock) {
        cache[assetPath]?.let { cached ->
            RendererDiagnostics.cacheHit(assetPath)
            return@synchronized CacheLookup.Immediate(cached)
        }
        inFlightLoads[assetPath]?.let { running ->
            RendererDiagnostics.cacheHit(assetPath)
            return@synchronized CacheLookup.Pending(running, owner = false)
        }
        RendererDiagnostics.cacheMiss(assetPath)
        val newTask = newTaskFactory()
        inFlightLoads[assetPath] = newTask
        CacheLookup.Pending(newTask, owner = true)
    }

    private fun runTaskAndResolve(assetPath: String, lookup: CacheLookup.Pending): CacheEntry {
        if (lookup.owner) lookup.task.run()
        return resolveTask(assetPath, lookup.task, lookup.owner)
    }

    private fun resolveTask(assetPath: String, task: FutureTask<CacheEntry>, owner: Boolean): CacheEntry {
        val entry = try {
            task.get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            RendererDiagnostics.renderFailure(assetPath, e.message ?: "Interrupted while loading SVG")
            CacheEntry.Failure("Interrupted while loading SVG")
        } catch (e: ExecutionException) {
            RendererDiagnostics.renderFailure(assetPath, e.cause?.message ?: e.message)
            CacheEntry.Failure(e.cause?.message ?: e.message ?: "SVG cache load failed")
        }

        if (owner) {
            synchronized(lock) {
                // If clear() was called while this load was in flight, the task
                // will no longer be registered and should not repopulate stale
                // cache state.
                if (inFlightLoads[assetPath] === task) {
                    inFlightLoads.remove(assetPath)
                    cache[assetPath] = entry
                }
            }
        }
        return entry
    }

    private fun loadEntry(context: Context, assetPath: String): CacheEntry {
        return when (val result = AnatomyAssetLoader(context).load(assetPath)) {
            is SvgLoadResult.Success -> CacheEntry.Success(result.svg)
            is SvgLoadResult.Missing -> CacheEntry.Failure(result.reason)
            is SvgLoadResult.Malformed -> CacheEntry.Failure(result.reason)
        }
    }

    private fun trimToMaxSizeLocked() {
        while (cache.size > maxEntries) {
            val eldestKey = cache.entries.firstOrNull()?.key ?: return
            cache.remove(eldestKey)
            RendererDiagnostics.cacheEviction(eldestKey)
        }
    }
}

enum class SvgCacheTestResult {
    Success,
    Failure
}
