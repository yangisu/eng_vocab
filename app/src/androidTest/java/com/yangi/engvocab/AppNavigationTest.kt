package com.yangi.engvocab

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationOpensBooks() {
        composeRule.onNodeWithText("단어장").performClick()
        composeRule.onNodeWithText("내 단어장").assertIsDisplayed()
    }
    @Test
    fun homeShortcutsOpenCollectionAndPhotoImport() {
        composeRule.onNodeWithText("중요 단어 모음").performClick()
        composeRule.onNodeWithText("중요 단어 모음").assertIsDisplayed()

        composeRule.onNodeWithText("뒤로").performClick()
        composeRule.onNodeWithText("사진으로 만들기").performClick()

        composeRule.onNodeWithText("사진으로 단어장 만들기").assertIsDisplayed()
    }
}
