package com.replog

import android.app.Application
import dagger.hilt.android.testing.CustomTestApplication

/**
 * Test-only Application registered by Hilt for any test annotated
 * with `@HiltAndroidTest`. Pairs with
 * `app/src/androidTest/java/com/replog/walkthrough/OnboardingWalkthroughTest.kt`.
 *
 * The production `App.class` (`@HiltAndroidApp`) is automatically swapped for
 * this class within instrumented tests, which prevents production-only
 * singletons (notifications, datastore) from binding.
 *
 * No modules need to be added here unless a future test depends on a
 * replacement binding. Keeping this minimal keeps CI fast and matches the
 * default pattern in hilt-android-testing 2.51.1.
 */
@CustomTestApplication
class RepLogTestApp : Application()
