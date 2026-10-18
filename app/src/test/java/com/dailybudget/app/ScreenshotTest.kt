package com.dailybudget.app

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dailybudget.app.ui.BudgetApp
import com.dailybudget.app.ui.BudgetTheme
import com.dailybudget.app.ui.LocalAutoFocus
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId

/**
 * Génère les captures d'écran du README (docs/screenshots) à partir de données de démo.
 * Lancé par la CI avec les autres tests unitaires ; les images sont écrites dans app/build/screenshots.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@OptIn(ExperimentalRoborazziApi::class)
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun launch() {
        val store = Store(ApplicationProvider.getApplicationContext<Context>())
        val today = LocalDate.now()
        fun at(date: LocalDate, hour: Int, minute: Int = 0) =
            date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Jour de paie ~18 jours plus tard pour un écran représentatif quelle que soit la date.
        store.saveSettings(Settings(payday = today.plusDays(18).dayOfMonth, reserveCents = 50000, currency = "MAD"))
        store.saveEntries(
            listOf(
                Entry(1, EntryType.INCOME, 1100000, at(today.minusDays(2), 9), category = null),
                Entry(2, EntryType.BALANCE, 1245000, at(today.minusDays(1), 21)),
                Entry(3, EntryType.EXPENSE, 350000, at(today, 8), category = "rent", excluded = true),
                Entry(4, EntryType.EXPENSE, 12000, at(today, 10, 15), category = "groceries"),
                Entry(5, EntryType.EXPENSE, 3500, at(today, 13, 40), category = "food"),
            )
        )
        compose.setContent {
            CompositionLocalProvider(LocalAutoFocus provides false) { BudgetTheme { BudgetApp(store) } }
        }
        compose.waitForIdle()
        // Horloge manuelle : le curseur clignotant des champs ne laisse jamais l'UI « idle » sinon.
        compose.mainClock.autoAdvance = false
        settle()
    }

    private fun settle() = compose.mainClock.advanceTimeBy(2_000)

    private fun capture(name: String) {
        settle()
        captureScreenRoboImage("build/screenshots/$name.png")
    }

    @Test
    @Config(qualifiers = "fr-rFR-w400dp-h880dp-xxhdpi")
    fun homeFr() {
        launch()
        capture("1-home-fr")
    }

    @Test
    @Config(qualifiers = "fr-rFR-w400dp-h880dp-xxhdpi")
    fun expenseFr() {
        launch()
        compose.onNodeWithText("Dépense").performClick()
        settle()
        compose.onNodeWithText("Voiture", substring = true).performClick()
        capture("2-expense-fr")
    }

    @Test
    @Config(qualifiers = "en-rUS-w400dp-h880dp-night-xxhdpi")
    fun homeEnDark() {
        launch()
        capture("3-home-en-dark")
    }

    @Test
    @Config(qualifiers = "en-rUS-w400dp-h880dp-xxhdpi")
    fun settingsEn() {
        launch()
        compose.onNodeWithContentDescription("Settings").performClick()
        capture("4-settings-en")
    }
}
