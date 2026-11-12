package dev.localloop.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.localloop.MainActivity
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end through the real app (Room on disk). The database file is deleted before the
 * test class so results do not depend on earlier runs.
 */
@RunWith(AndroidJUnit4::class)
class TasksFlowUiTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun clearDatabase() {
            ApplicationProvider.getApplicationContext<Context>().deleteDatabase("localloop.db")
        }
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createCompleteFilterAndDelete() {
        rule.onNodeWithText("No tasks yet. Add your first one above.").assertIsDisplayed()

        // Empty title is rejected.
        rule.onNodeWithTag("addButton").performClick()
        rule.onNodeWithText("Title can't be empty").assertIsDisplayed()

        rule.onNodeWithTag("titleInput").performTextInput("Buy milk")
        rule.onNodeWithTag("addButton").performClick()
        rule.onNodeWithTag("titleInput").performTextInput("Call mom")
        rule.onNodeWithTag("addButton").performClick()

        rule.onNodeWithTag("check_Buy milk").assertIsOff().performClick()
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("check_Buy milk").assertIsOn() }.isSuccess
        }

        rule.onNodeWithTag("filter_OPEN").performClick()
        rule.onNodeWithTag("task_Call mom").assertIsDisplayed()
        rule.onNodeWithTag("task_Buy milk").assertDoesNotExist()

        rule.onNodeWithTag("filter_DONE").performClick()
        rule.onNodeWithTag("task_Buy milk").assertIsDisplayed()
        rule.onNodeWithTag("task_Call mom").assertDoesNotExist()

        rule.onNodeWithTag("delete_Buy milk").performClick()
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithText("No completed tasks yet.").assertIsDisplayed() }.isSuccess
        }
    }
}
