package com.github.damontecres.wholphin.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * Records the code of the paths viewers use most, in every home screen style: Home, the library pages (Library,
 * Movies, TV shows, catalogues, Favorites), a few titles of each, their details pages and playing a movie and a
 * series. Search, Settings and the sort, filter and view menus are not browsed, Settings is only used to change the
 * style.
 *
 * Run once per home screen style with `am instrument` on a connected Android TV (API 33+), the app signed in to a
 * server, and merge the profiles: docs/ai/build-and-development.md has the steps. Clicking a card must open its
 * details (the default), not play it. A movie and an episode are played for a few seconds, which leaves them in
 * progress.
 *
 * The screens are driven with the D-pad, apart from taps on the rail and top bar labels. The navigation is found by
 * its labels (English or Romanian), so the app language must be one of those.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(
            packageName = PACKAGE_NAME,
            maxIterations = 1,
            stableIterations = 1,
            includeInStartupProfile = true,
        ) {
            // The TV can have gone to its own screensaver while nothing was pressed, which the first key only closes
            device.wakeUp()
            pressHome()
            startActivityAndWait()

            // The home rows have loaded once a card has focus
            check(device.wait(Until.hasObject(By.focused(true)), 30_000)) {
                "Nothing focused, is the app signed in to a server?"
            }
            device.waitForIdle()
            Thread.sleep(3_000)

            showNavigation()
            var chrome = detectChrome()
            // The app can open on any page it was left on (Search, Settings), the run starts from Home
            openEntry(chrome, HOME)
            for (look in looksToRun()) {
                // With `-e apply false` the style is the one already set, changed beforehand (one run per style)
                chrome =
                    if (InstrumentationRegistry.getArguments().getString("apply") == "false") detectChrome() else applyLook(chrome, look)
                if (look.rail) browseRail(look) else browseTopBar(chrome)
            }
        }

    /**
     * All of them, only the rail or only the top bar ones with `-e looks rail` or `-e looks bar`, or a single one with
     * `-e look <home screen style number>`. One style per run is the most reliable, the profiles of the runs are merged.
     */
    private fun looksToRun(): List<Look> {
        val arguments = InstrumentationRegistry.getArguments()
        arguments.getString("look")?.toIntOrNull()?.let { style -> return LOOKS.filter { it.homeStyle == style } }
        return when (arguments.getString("looks")) {
            "rail" -> LOOKS.filter { it.rail }
            "bar" -> LOOKS.filter { !it.rail }
            else -> LOOKS
        }
    }

    /** What to move with to reach the home, library and settings entries */
    private enum class Chrome { CLASSIC_BAR, COMPACT_BAR, RAIL }

    private enum class Key { UP, DOWN, LEFT, RIGHT, CENTER, BACK }

    /** A home screen style, the top bar ones keep the top bar style that is set */
    private data class Look(
        val homeStyle: Int,
        val rail: Boolean,
        val minimalistList: Boolean = false,
    )

    private fun MacrobenchmarkScope.press(
        key: Key,
        times: Int = 1,
        pause: Long = 400,
    ) = repeat(times) {
        // Search is not part of the run: enter there opens its keyboard, which then takes every key
        if (key == Key.CENTER && focusedHas(SEARCH)) {
            actions += "CENTER skipped on Search"
            return@repeat
        }
        when (key) {
            Key.UP -> device.pressDPadUp()
            Key.DOWN -> device.pressDPadDown()
            Key.LEFT -> device.pressDPadLeft()
            Key.RIGHT -> device.pressDPadRight()
            Key.CENTER -> device.pressDPadCenter()
            Key.BACK -> device.pressBack()
        }
        Thread.sleep(pause)
        actions += key.name
        // The package can briefly be another one while the page changes, so look again before calling it a failure
        if (key == Key.BACK && device.currentPackageName != PACKAGE_NAME &&
            run {
                Thread.sleep(3_000)
                device.currentPackageName != PACKAGE_NAME
            }
        ) {
            // A back on the first page of the app leaves it, open it again where it was
            actions += "left the app"
            startActivityAndWait()
            Thread.sleep(4_000)
        }
    }

    /** The keys pressed so far, to find out what a failure came after */
    private val actions = mutableListOf<String>()

    /** The larger steps (looks applied, navigation found), kept for the whole run */
    private val milestones = mutableListOf<String>()

    /** The page keeps changing, an entry that went away before it was read counts as nothing focused */
    private fun MacrobenchmarkScope.focusedBounds() =
        try {
            device.findObject(By.focused(true))?.visibleBounds
        } catch (e: StaleObjectException) {
            null
        }

    /** The labels are not nested in the focused entry in the accessibility tree, so look at where they are drawn */
    private fun MacrobenchmarkScope.focusedHas(label: Pattern): Boolean {
        val focused = settledFocusBounds() ?: return false
        return try {
            device.findObjects(By.text(label)).any { focused.contains(it.visibleBounds) }
        } catch (e: StaleObjectException) {
            false
        }
    }

    /** Presses [key] until the focused entry has [label], at most [max] times */
    private fun MacrobenchmarkScope.seek(
        label: Pattern,
        key: Key,
        max: Int,
    ): Boolean {
        repeat(max) {
            if (focusedHas(label)) return true
            press(key, pause = 350)
        }
        return focusedHas(label)
    }

    /** Closes the keyboard and the Search page if a stray key took the run there */
    private fun MacrobenchmarkScope.closeSearch() {
        repeat(2) {
            if (!device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")) return
            actions += "keyboard open"
            press(Key.BACK, pause = 1_500)
        }
    }

    /**
     * The app opens on the page it was left on and a page opened from a title has no navigation, go back until one
     * with the navigation shows. Too many backs end up on the server and user pages, which are answered with enter.
     */
    private fun MacrobenchmarkScope.showNavigation() {
        closeSearch()
        repeat(5) {
            if (device.hasObject(By.text(SETUP_SCREEN))) {
                press(Key.CENTER, pause = 4_000)
                device.waitForIdle()
                return@repeat
            }
            if (device.hasObject(By.text(HOME))) return
            press(Key.BACK, pause = 2_000)
        }
    }

    /** Looks at where the Settings entry is: on the rail, in the top bar, or not shown (icon only top bar) */
    private fun MacrobenchmarkScope.detectChrome(): Chrome {
        // The navigation can take a moment to be drawn after a page changed, only the compact bar has no Settings label
        device.wait(Until.hasObject(By.text(HOME)), 15_000)
        device.waitForIdle()
        Thread.sleep(1_500)
        val bounds = device.findObject(By.text(SETTINGS))?.visibleBounds
        milestones += "detect settings=$bounds"
        return when {
            bounds == null -> Chrome.COMPACT_BAR
            bounds.top < NAV_TOP_BAR_BELOW -> Chrome.CLASSIC_BAR
            else -> Chrome.RAIL
        }
    }

    /** The focus is briefly missing while the page redraws, wait for it before deciding there is none */
    private fun MacrobenchmarkScope.settledFocusBounds(): android.graphics.Rect? {
        repeat(5) {
            focusedBounds()?.let { return it }
            Thread.sleep(500)
        }
        return null
    }

    /**
     * Moves the focus onto the rail or the top bar from wherever it is on the page. Some pages do not let the key
     * through (and a back would leave the app), so then the app is started again, on Home the way is short.
     */
    private fun MacrobenchmarkScope.focusNav(chrome: Chrome) {
        leavePlayer()
        showNavigation()
        var backs = 0
        val trail = mutableListOf<String>()
        repeat(2) { attempt ->
            if (attempt > 0) restartApp()
            // A page can have many rows to go up through, a focus that keeps coming back to the same entry is stuck
            val visits = mutableMapOf<android.graphics.Rect, Int>()
            repeat(25) {
                val bounds = settledFocusBounds()
                trail += "$bounds"
                if (bounds != null) {
                    if (chrome == Chrome.RAIL && bounds.right < NAV_RAIL_BELOW) return
                    if (chrome != Chrome.RAIL && bounds.top < NAV_TOP_BAR_BELOW) return
                    val seen = (visits[bounds] ?: 0) + 1
                    visits[bounds] = seen
                    if (seen >= 4 && backs++ < 2) {
                        // Some pages (a catalogue grid) have no way up, going back leads to one that has
                        press(Key.BACK, pause = 2_500)
                        visits.clear()
                    } else {
                        press(if (chrome == Chrome.RAIL) Key.LEFT else Key.UP, pause = 0)
                        waitForFocusChange(bounds)
                    }
                } else {
                    // Nothing has focus after a page changed, any key gives it to the first entry
                    press(Key.DOWN, pause = 800)
                }
            }
        }
        error("Could not reach the navigation ($chrome), focus trail ${trail.takeLast(6)}, keys ${actions.takeLast(40)}")
    }

    private fun MacrobenchmarkScope.restartApp() {
        killProcess()
        startActivityAndWait()
        check(device.wait(Until.hasObject(By.focused(true)), 30_000)) { "Nothing focused after starting the app" }
        device.waitForIdle()
        Thread.sleep(4_000)
        // The app comes back on the page it was left on, Home is where the way to the navigation is short
        showNavigation()
        try {
            device
                .findObjects(By.text(HOME))
                .firstOrNull {
                    it.visibleBounds.top < NAV_TOP_BAR_BELOW ||
                        it.visibleBounds.right < NAV_RAIL_BELOW
                }?.click()
        } catch (e: StaleObjectException) {
            // The page changed while it was looked at, the next attempt starts from wherever it is
        }
        Thread.sleep(3_000)
        device.waitForIdle()
    }

    /** What is on the screen, for the messages of failures */
    private fun MacrobenchmarkScope.screenTexts() =
        device
            .findObjects(By.text(Pattern.compile(".+")))
            .take(25)
            .joinToString(" | ") {
                try {
                    "${it.text}@${it.visibleBounds}"
                } catch (e: StaleObjectException) {
                    "?"
                }
            }

    /** Focuses the entry with [label] of the rail or the top bar */
    private fun MacrobenchmarkScope.focusEntry(
        chrome: Chrome,
        label: Pattern,
    ) {
        focusNav(chrome)
        val found =
            if (chrome == Chrome.RAIL) {
                moveToRailEntry(label) || correctRailFocus(label)
            } else {
                seekInBar(chrome, label)
            }
        check(found) { "No navigation entry for $label, milestones $milestones, steps ${actions.takeLast(30)}" }
    }

    /** The rail lists its entries in this order, below the profile at the top */
    private fun railIndex(label: Pattern): Int? =
        when (label) {
            HOME -> 1
            MOVIES -> 2
            SERIES -> 3
            FAVORITES -> 4
            SETTINGS -> 6
            else -> null
        }

    /** Goes to the top of the rail and counts down to the entry, which is checked by its label */
    private fun MacrobenchmarkScope.moveToRailEntry(label: Pattern): Boolean {
        val index = railIndex(label) ?: return false
        press(Key.UP, 8, 300)
        press(Key.DOWN, index, 600)
        return focusedHas(label)
    }

    /** Presses up or down, whichever is towards the label, until the focused entry is the one with it */
    private fun MacrobenchmarkScope.correctRailFocus(label: Pattern): Boolean {
        repeat(10) {
            if (focusedHas(label)) return true
            val focus = settledFocusBounds() ?: return@repeat
            val text =
                try {
                    device.findObjects(By.text(label)).firstOrNull()?.visibleBounds
                } catch (e: StaleObjectException) {
                    null
                } ?: return false
            actions += "fix $label focus $focus text $text"
            press(if (focus.centerY() < text.centerY()) Key.DOWN else Key.UP, pause = 1_200)
        }
        return focusedHas(label)
    }

    /** To the top of the rail, then down one entry at a time, waiting for the focus to move after each key */
    private fun MacrobenchmarkScope.seekInRail(label: Pattern): Boolean {
        for (key in listOf(Key.UP, Key.DOWN)) {
            var misses = 0
            for (step in 0 until 14) {
                if (key == Key.DOWN && focusedHas(label)) return true
                val before = settledFocusBounds() ?: continue
                press(key, pause = 0)
                val after = waitForFocusChange(before)
                actions += "rail $key $before -> $after"
                if (after == before) {
                    if (++misses > 1) break
                } else {
                    misses = 0
                }
            }
        }
        return focusedHas(label)
    }

    /**
     * Right then left along the bar, one press at a time and waiting for the focus to move, so a slow key is never
     * answered by a second one that would skip the entry. A direction ends at the end of the bar.
     */
    private fun MacrobenchmarkScope.seekInBar(
        chrome: Chrome,
        label: Pattern,
    ): Boolean {
        // Left again from the end, the first entry can be skipped when moving left, but not when moving right from the profile
        for (key in listOf(Key.RIGHT, Key.LEFT, Key.RIGHT)) {
            var misses = 0
            for (step in 0 until 8) {
                if (focusedHas(label)) return true
                val before = settledFocusBounds()
                if (before == null || before.top >= NAV_TOP_BAR_BELOW) {
                    focusNav(chrome)
                    continue
                }
                press(key, pause = 0)
                val after = waitForFocusChange(before)
                actions += "seek $label $key $before -> $after"
                if (after == null || after.top >= NAV_TOP_BAR_BELOW) {
                    focusNav(chrome)
                } else if (after == before) {
                    // A key is sometimes lost, two in a row are the end of the bar
                    if (++misses > 1) break
                } else {
                    misses = 0
                }
            }
        }
        return focusedHas(label)
    }

    /** Waits until the focus is somewhere else than [before], for at most 1.5 seconds */
    private fun MacrobenchmarkScope.waitForFocusChange(before: android.graphics.Rect): android.graphics.Rect? {
        repeat(10) {
            Thread.sleep(150)
            val now = focusedBounds()
            if (now != null && now != before) return now
        }
        return focusedBounds()
    }

    /**
     * Opens an entry of the rail or the top bar by tapping its label, which does not depend on where the focus is
     * (moving there with the D-pad jumps to the selected entry and loses keys). Without the label it moves there.
     */
    private fun MacrobenchmarkScope.openEntry(
        chrome: Chrome,
        label: Pattern,
    ) {
        showNavigation()
        val entry =
            try {
                device.findObjects(By.text(label)).firstOrNull {
                    val bounds = it.visibleBounds
                    if (chrome == Chrome.RAIL) bounds.right < NAV_RAIL_BELOW else bounds.top < NAV_TOP_BAR_BELOW
                }
            } catch (e: StaleObjectException) {
                null
            }
        if (entry != null) {
            entry.click()
            Thread.sleep(3_000)
            device.waitForIdle()
            // The focus stays on the bar, the page below it is where the keys should go (Settings starts focused itself)
            val focus = settledFocusBounds()
            if (chrome != Chrome.RAIL && label != SETTINGS && (focus == null || focus.top < NAV_TOP_BAR_BELOW)) {
                press(Key.DOWN, pause = 800)
            }
        } else {
            focusEntry(chrome, label)
            press(Key.CENTER, pause = 3_000)
            device.waitForIdle()
        }
    }

    private fun MacrobenchmarkScope.openSettings(chrome: Chrome) {
        when (chrome) {
            Chrome.COMPACT_BAR -> {
                // Settings only has an icon there, it is the entry before the profile at the end of the bar
                focusNav(chrome)
                var misses = 0
                while (misses < 2) {
                    val before = settledFocusBounds() ?: break
                    press(Key.RIGHT, pause = 0)
                    if (waitForFocusChange(before) == before) misses++
                }
                val end = settledFocusBounds()
                press(Key.LEFT, pause = 0)
                end?.let { waitForFocusChange(it) }
                press(Key.CENTER, pause = 3_000)
            }

            else -> {
                openEntry(chrome, SETTINGS)
            }
        }
        device.waitForIdle()
    }

    /** Whether the setting titled [title] shows [value] right below its title, wherever the focus is */
    private fun MacrobenchmarkScope.rowShows(
        title: Pattern?,
        value: Pattern,
    ): Boolean {
        if (title == null) return focusedHas(value)
        return try {
            val values = device.findObjects(By.text(value)).map { it.visibleBounds }
            device.findObjects(By.text(title)).any { node ->
                val t = node.visibleBounds
                values.any { v -> kotlin.math.abs(v.left - t.left) < 24 && v.top - t.bottom in 0..48 }
            }
        } catch (e: StaleObjectException) {
            false
        }
    }

    /** Changes one choice of the Appearance settings and goes back, counted down from the first option of its dialog */
    private fun MacrobenchmarkScope.choosePreference(
        chrome: Chrome,
        row: Int,
        option: Int,
        options: Int,
        expected: Pattern? = null,
        title: Pattern? = null,
    ) {
        openSettings(chrome)
        // The categories come first (Profile, Appearance), then their settings
        press(Key.DOWN, pause = 800)
        press(Key.RIGHT, pause = 800)
        press(Key.DOWN, row, 500)
        press(Key.CENTER, pause = 1_000)
        // The dialog opens on its first option (not the chosen one), so it is counted from there. An up press on the
        // first option leaves the dialog for the page below it.
        press(Key.DOWN, option, 600)
        press(Key.CENTER, pause = 2_000)
        if (expected != null && !rowShows(title, expected)) milestones += "$expected may not be chosen"
        // A back first returns from the setting to its category, then leaves the page
        var backs = 0
        do {
            press(Key.BACK, pause = 2_500)
        } while (device.hasObject(By.text(APPEARANCE)) && ++backs < 4)
        device.waitForIdle()
        // A new style recomposes the whole app, keys pressed meanwhile are lost
        Thread.sleep(5_000)
    }

    /** Sets the home screen style and returns the navigation the app has with it */
    private fun MacrobenchmarkScope.applyLook(
        chrome: Chrome,
        look: Look,
    ): Chrome {
        milestones += "apply home style ${look.homeStyle} from $chrome"
        choosePreference(chrome, PREF_HOME_STYLE, look.homeStyle, HOME_STYLES, STYLE_NAMES[look.homeStyle], HOME_STYLE_TITLE)
        // The page still has the previous navigation for a moment
        var current = detectChrome()
        repeat(8) {
            if ((current == Chrome.RAIL) == look.rail) return@repeat
            Thread.sleep(1_500)
            current = detectChrome()
        }
        check((current == Chrome.RAIL) == look.rail) {
            "The style did not apply, expected rail ${look.rail} but the navigation looks like $current, keys ${actions.takeLast(30)}"
        }
        return current
    }

    /** Along a row and back, then down to the next one, like a viewer would */
    private fun MacrobenchmarkScope.browseRows() {
        repeat(3) {
            press(Key.RIGHT, 6, 400)
            press(Key.LEFT, 6, 200)
            press(Key.DOWN, pause = 800)
        }
        press(Key.UP, 3, 500)
    }

    /** Down and along the grid of a library page, never up, where its sort, filter and view buttons are */
    private fun MacrobenchmarkScope.browseGrid() {
        press(Key.DOWN, 2, 600)
        press(Key.RIGHT, 2, 300)
        press(Key.DOWN, pause = 600)
        press(Key.LEFT, pause = 300)
    }

    /** Opens the focused title, goes down through its details page and back, and optionally plays it */
    private fun MacrobenchmarkScope.openDetails(play: Boolean = false) {
        press(Key.CENTER, pause = 5_000)
        device.waitForIdle()
        // Some cards, like the ones of Continue watching, play right away
        if (inPlayer()) {
            Thread.sleep(10_000)
            leavePlayer()
            return
        }
        // The page loads its details, then its people and similar titles
        repeat(6) {
            press(Key.DOWN, pause = 900)
            press(Key.RIGHT, 3, 300)
        }
        press(Key.UP, 8, 400)
        if (play && seek(PLAY, Key.LEFT, 5)) playMovie()
        press(Key.BACK, pause = 2_000)
        device.waitForIdle()
    }

    /** The player has a view over the whole screen, no page has an entry that big */
    private fun MacrobenchmarkScope.inPlayer(): Boolean {
        val bounds = focusedBounds() ?: return false
        // A page without entries can have a whole-screen focus too, but it still shows the navigation
        return bounds.width() >= 1800 && bounds.height() >= 1000 && !device.hasObject(By.text(HOME))
    }

    /** The first back may only hide the controls */
    private fun MacrobenchmarkScope.leavePlayer() {
        repeat(3) {
            if (!inPlayer()) return
            press(Key.BACK, pause = 2_500)
        }
    }

    /** Starts the focused Play button, uses the controls for a moment and returns to the details page */
    private fun MacrobenchmarkScope.playMovie() {
        press(Key.CENTER, pause = 15_000)
        press(Key.RIGHT, 2, 1_000)
        press(Key.CENTER, 2, 1_500)
        Thread.sleep(3_000)
        leavePlayer()
    }

    /** Home, the Library pages and Favorites of a top bar */
    private fun MacrobenchmarkScope.browseTopBar(chrome: Chrome) {
        openEntry(chrome, HOME)
        browseRows()
        // Two movies of the second row, one after the other
        press(Key.DOWN, pause = 800)
        repeat(2) { index ->
            press(Key.RIGHT, index, 400)
            openDetails()
        }

        // The Library page has the catalogues, then Movies and TV shows next to each other
        openEntry(chrome, LIBRARY)
        press(Key.RIGHT, pause = 600)
        press(Key.CENTER, pause = 4_000)
        browseGrid()
        openDetails(play = true)

        openEntry(chrome, LIBRARY)
        press(Key.RIGHT, 2, 600)
        press(Key.CENTER, pause = 4_000)
        browseGrid()
        // Play on a series starts its next episode
        openDetails(play = true)

        openEntry(chrome, LIBRARY)
        press(Key.CENTER, pause = 4_000)
        browseGrid()
        // A catalogue
        press(Key.CENTER, pause = 4_000)
        browseGrid()

        openEntry(chrome, FAVORITES)
        browseGrid()
    }

    /** Home, Movies, TV shows and Favorites of the rail, each with its catalogues, posters and details */
    private fun MacrobenchmarkScope.browseRail(look: Look) {
        openEntry(Chrome.RAIL, HOME)
        browseLayout(look)

        openEntry(Chrome.RAIL, MOVIES)
        browseLayout(look, play = true)
        // Another catalogue of the same kind
        press(Key.LEFT, 3, 300)
        press(Key.DOWN, pause = 800)
        browseLayout(look)

        openEntry(Chrome.RAIL, SERIES)
        // Play on a series starts its next episode
        browseLayout(look, play = true)

        openEntry(Chrome.RAIL, FAVORITES)
        browseLayout(look)
    }

    /**
     * The catalogues on the left, then the titles, then one of them opened. The focus starts on the rail or the
     * catalogues, a title is never reached with a press that could land on the Play button of the details panel.
     */
    private fun MacrobenchmarkScope.browseLayout(
        look: Look,
        play: Boolean = false,
    ) {
        val bounds = focusedBounds()
        if (bounds != null && bounds.right < NAV_RAIL_BELOW) press(Key.RIGHT, pause = 800)
        // Down the catalogues only, the filter button is above the first one
        press(Key.DOWN, 3, 800)
        // Into the titles
        press(Key.RIGHT, pause = 1_000)
        if (look.minimalistList) {
            press(Key.DOWN, 4, 500)
            press(Key.UP, 2, 500)
        } else {
            press(Key.RIGHT, pause = 400)
            press(Key.DOWN, 2, 600)
            press(Key.LEFT, pause = 400)
        }
        openDetails(play)
        press(Key.LEFT, 3, 300)
    }

    companion object {
        private const val PACKAGE_NAME = "io.github.maddinu93.thorphin"

        // Rows of the Appearance settings and the number of choices they have
        private const val PREF_HOME_STYLE = 1
        private const val HOME_STYLES = 6

        // The top bar is above this, the rail is left of it (pixels of a 1080p screen)
        private const val NAV_TOP_BAR_BELOW = 150
        private const val NAV_RAIL_BELOW = 160

        private val HOME = Pattern.compile("Home|Acasă")
        private val LIBRARY = Pattern.compile("Library|Librărie")
        private val MOVIES = Pattern.compile("Movies|Filme")
        private val SERIES = Pattern.compile("Series|Seriale")
        private val FAVORITES = Pattern.compile("Favorites|Favorite")
        private val SETTINGS = Pattern.compile("Settings|Setări")

        // A category of the Settings page, which nothing else shows
        private val APPEARANCE = Pattern.compile("Appearance|Aspect")
        private val PLAY = Pattern.compile("Play|Resume|Redă|Reia")

        // The pages the app goes to when it is taken back past Home
        // How the options of the style settings are shown on their row, in the order of their choices
        private val STYLE_NAMES =
            listOf(
                Pattern.compile("Classic|Clasic"),
                Pattern.compile("Modern"),
                Pattern.compile("Squared|Pătrat"),
                Pattern.compile("Thorphin"),
                Pattern.compile("Thorphin Minimalist List|Thorphin minimalist - listă"),
                Pattern.compile("Thorphin Minimalist Cards|Thorphin minimalist - carduri"),
            )

        // The title of the style setting, its value is shown right below it
        private val HOME_STYLE_TITLE = Pattern.compile("Home screen style|Stilul ecranului principal")

        private val SEARCH = Pattern.compile("Search|Caută")

        private val SETUP_SCREEN = Pattern.compile("Select Server|Select User|Alege server|Alege utilizator")

        // Ends on the Thorphin style
        private val LOOKS =
            listOf(
                Look(homeStyle = 2, rail = false),
                Look(homeStyle = 1, rail = false),
                Look(homeStyle = 0, rail = false),
                Look(homeStyle = 4, rail = true, minimalistList = true),
                Look(homeStyle = 5, rail = true),
                Look(homeStyle = 3, rail = true),
            )
    }
}
