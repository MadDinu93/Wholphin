package com.github.damontecres.wholphin.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code used when starting the app and browsing the home page, so it is compiled ahead of time on install
 *
 * Run with `./gradlew :app:generateBaselineProfile` while an Android TV (API 33+) is connected. The app must already
 * be signed in to a server: sign in once in the release build this installs, then run it again.
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

            // Browse the rows like a viewer would: along a row, then down to the next one
            repeat(4) {
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
            repeat(4) {
                device.pressDPadUp()
                Thread.sleep(500)
            }
        }

    companion object {
        private const val PACKAGE_NAME = "io.github.maddinu93.thorphin"
    }
}
