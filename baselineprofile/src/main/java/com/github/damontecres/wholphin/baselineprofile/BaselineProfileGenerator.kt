package com.github.damontecres.wholphin.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code used when starting the app, browsing the home page and opening movies, so it is compiled ahead of
 * time on install
 *
 * Run with `./gradlew :app:generateBaselineProfile` while an Android TV (API 33+) is connected. The app must already
 * be signed in to a server: sign in once in the release build this installs, then run it again. Clicking a card must
 * open its details (the default), not play it.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(
            packageName = PACKAGE_NAME,
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()

            // The home rows have loaded once a card has focus
            check(device.wait(Until.hasObject(By.focused(true)), 30_000)) {
                "Nothing focused, is the app signed in to a server?"
            }
            device.waitForIdle()

            browseHome()

            // Two movies of the second row, one after the other
            device.pressDPadDown()
            Thread.sleep(800)
            repeat(2) { index ->
                repeat(index) {
                    device.pressDPadRight()
                    Thread.sleep(400)
                }
                openDetails()
            }
        }

    /** Along a row and back, then down to the next one, like a viewer would */
    private fun MacrobenchmarkScope.browseHome() {
        repeat(3) {
            repeat(6) {
                device.pressDPadRight()
                Thread.sleep(400)
            }
            repeat(6) {
                device.pressDPadLeft()
                Thread.sleep(200)
            }
            device.pressDPadDown()
            Thread.sleep(800)
        }
        repeat(3) {
            device.pressDPadUp()
            Thread.sleep(500)
        }
    }

    /** Opens the focused card, goes down through its details page, along its rows, and back home */
    private fun MacrobenchmarkScope.openDetails() {
        device.pressDPadCenter()
        // The page loads its details, then its people and similar titles
        Thread.sleep(5_000)
        device.waitForIdle()
        repeat(6) {
            device.pressDPadDown()
            Thread.sleep(900)
            repeat(3) {
                device.pressDPadRight()
                Thread.sleep(300)
            }
        }
        repeat(6) {
            device.pressDPadUp()
            Thread.sleep(400)
        }
        device.pressBack()
        Thread.sleep(2_000)
        device.waitForIdle()
    }

    companion object {
        private const val PACKAGE_NAME = "io.github.maddinu93.thorphin"
    }
}
