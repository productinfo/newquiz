package com.infinitepower.newquiz.core.navigation

import androidx.navigation.NavController
import com.infinitepower.newquiz.quiz_presentation.QuizType
import com.infinitepower.newquiz.quiz_presentation.destinations.QuizScreenDestination
import com.infinitepower.newquiz.home_presentation.HomeNavigator
import com.ramcosta.composedestinations.navigation.navigate

class CommonNavGraphNavigator(
    private val navController: NavController
) : HomeNavigator {
    override fun navigateToQuickQuiz() {
        navController.navigate(QuizScreenDestination(quizType = QuizType.QUICK_QUIZ))
    }
}