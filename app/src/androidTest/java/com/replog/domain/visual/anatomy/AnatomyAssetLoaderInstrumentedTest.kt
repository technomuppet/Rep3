package com.replog.domain.visual.anatomy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnatomyAssetLoaderInstrumentedTest {

    @Test
    fun missingSvgReturnsMissingResult() {
        val loader = AnatomyAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
        val result = loader.load("anatomy/does_not_exist.svg")
        assertTrue(result is SvgLoadResult.Missing)
    }

    @Test
    fun malformedSvgReturnsMalformedResult() {
        val loader = AnatomyAssetLoader(InstrumentationRegistry.getInstrumentation().context)
        val result = loader.load("malformed.svg")
        assertTrue(result is SvgLoadResult.Malformed)
    }

    @Test
    fun everyRegisteredSvgParsesFromPackagedAssets() {
        val loader = AnatomyAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
        val failures = MuscleAssetRegistry.allAssetPaths()
            .map { path -> path to loader.load(path) }
            .filterNot { (_, result) -> result is SvgLoadResult.Success }

        assertTrue("Unparseable registered SVGs: $failures", failures.isEmpty())
    }
}
