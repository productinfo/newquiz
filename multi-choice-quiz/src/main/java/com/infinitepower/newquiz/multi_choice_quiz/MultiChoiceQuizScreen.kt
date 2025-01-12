package com.infinitepower.newquiz.multi_choice_quiz

import android.content.res.Configuration
import androidx.annotation.Keep
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.infinitepower.newquiz.core.analytics.logging.rememberCoreLoggingAnalytics
import com.infinitepower.newquiz.core.common.annotation.compose.PreviewNightLight
import com.infinitepower.newquiz.core.common.viewmodel.NavEvent
import com.infinitepower.newquiz.core.multi_choice_quiz.MultiChoiceQuizType
import com.infinitepower.newquiz.core.theme.NewQuizTheme
import com.infinitepower.newquiz.core.theme.spacing
import com.infinitepower.newquiz.core.ui.ads.admob.BannerAd
import com.infinitepower.newquiz.model.multi_choice_quiz.MultiChoiceQuestion
import com.infinitepower.newquiz.model.multi_choice_quiz.MultiChoiceQuestionStep
import com.infinitepower.newquiz.model.multi_choice_quiz.SelectedAnswer
import com.infinitepower.newquiz.model.multi_choice_quiz.getBasicMultiChoiceQuestion
import com.infinitepower.newquiz.multi_choice_quiz.components.CardQuestionAnswers
import com.infinitepower.newquiz.multi_choice_quiz.components.QuizStepViewRow
import com.infinitepower.newquiz.multi_choice_quiz.components.QuizTopBar
import com.infinitepower.newquiz.core.R as CoreR
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.navigate

@Keep
data class MultiChoiceQuizScreenNavArg(
    val initialQuestions: ArrayList<MultiChoiceQuestion> = arrayListOf(),
    val category: Int = -1,
    val difficulty: String? = null,
    val type: MultiChoiceQuizType = MultiChoiceQuizType.NORMAL,
    val mazeItemId: String? = null
)

@Composable
@Destination(
    navArgsDelegate = MultiChoiceQuizScreenNavArg::class,
    deepLinks = [
        DeepLink(uriPattern = "newquiz://quickquiz")
    ]
)
fun MultiChoiceQuizScreen(
    navigator: NavController,
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    viewModel: QuizScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel
            .navEvent
            .collect { event ->
                when (event) {
                    is NavEvent.Navigate -> {
                        navigator.navigate(event.direction) {
                            navigator.currentDestination?.route?.let { route ->
                                launchSingleTop = true
                                popUpTo(route) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
    }

    val coreLoggingAnalytics = rememberCoreLoggingAnalytics()
    LaunchedEffect(key1 = true) {
        coreLoggingAnalytics.logScreenView("MultiChoiceScreen")
    }

    MultiChoiceQuizScreenImpl(
        onBackClick = navigator::popBackStack,
        windowWidthSizeClass = windowWidthSizeClass,
        windowHeightSizeClass = windowHeightSizeClass,
        uiState = uiState,
        onEvent = viewModel::onEvent
    )

    if (uiState.userDiamonds == 0) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onEvent(MultiChoiceQuizScreenUiEvent.CleanUserSkipQuestionDiamonds)
            },
            title = {
                Text(text = stringResource(id = CoreR.string.no_diamonds))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(MultiChoiceQuizScreenUiEvent.CleanUserSkipQuestionDiamonds)
                    }
                ) {
                    Text(text = stringResource(id = CoreR.string.close))
                }
            }
        )
    } else if (uiState.userDiamonds > 0) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onEvent(MultiChoiceQuizScreenUiEvent.CleanUserSkipQuestionDiamonds)
            },
            title = {
                Text(text = stringResource(id = CoreR.string.skip_question_q))
            },
            text = {
                Text(text = stringResource(id = CoreR.string.you_have_n_diamonds_skip_question_q, uiState.userDiamonds))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(MultiChoiceQuizScreenUiEvent.SkipQuestion)
                    }
                ) {
                    Text(text = stringResource(id = CoreR.string.skip))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(MultiChoiceQuizScreenUiEvent.CleanUserSkipQuestionDiamonds)
                    }
                ) {
                    Text(text = stringResource(id = CoreR.string.close))
                }
            }
        )
    }
}

@Composable
private fun MultiChoiceQuizScreenImpl(
    uiState: MultiChoiceQuizScreenUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    onBackClick: () -> Unit,
    onEvent: (event: MultiChoiceQuizScreenUiEvent) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.remainingTime.getRemainingPercent(),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )
    
    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            QuizTopBar(
                progressText = uiState.remainingTime.toMinuteSecond(),
                windowHeightSizeClass = windowHeightSizeClass,
                progressIndicatorValue = animatedProgress,
                userSignedIn = uiState.userSignedIn,
                onBackClick = onBackClick,
                onSkipClick = { onEvent(MultiChoiceQuizScreenUiEvent.GetUserSkipQuestionDiamonds) },
                modifier = Modifier.fillMaxWidth(),
                currentQuestionNull = uiState.currentQuestionStep == null
            )

            if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                QuizContentWidthCompact(uiState = uiState, onEvent = onEvent)
            } else {
                QuizContentWidthMedium(uiState = uiState, onEvent = onEvent)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ColumnScope.QuizContentWidthCompact(
    uiState: MultiChoiceQuizScreenUiState,
    onEvent: (event: MultiChoiceQuizScreenUiEvent) -> Unit
) {
    val spaceMedium = MaterialTheme.spacing.medium
    val spaceLarge = MaterialTheme.spacing.large

    Spacer(modifier = Modifier.height(spaceMedium))
    QuizStepViewRow(
        modifier = Modifier.fillMaxWidth(),
        questionSteps = uiState.questionSteps
    )
    AnimatedVisibility(
        visible = uiState.currentQuestionStep != null
    ) {
        val currentQuestion = uiState.currentQuestionStep?.question

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(spaceLarge))
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = spaceMedium)
                    .fillMaxWidth()
            ) {
                if (currentQuestion != null) {
                    item {
                        Column {
                            Text(
                                text = uiState.getQuestionPositionFormatted(),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(spaceMedium))
                            Text(
                                text = currentQuestion.description,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            // Question image, if exists
                            currentQuestion.imageUrl?.let { imageUrl ->
                                val imageScale = if (currentQuestion.category == "Logo Quiz") {
                                    ContentScale.FillHeight
                                } else ContentScale.Crop

                                Spacer(modifier = Modifier.height(spaceMedium))
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Image",
                                    modifier = Modifier
                                        .aspectRatio(16 / 9f)
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = imageScale
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(spaceLarge))
                    CardQuestionAnswers(
                        answers = currentQuestion?.answers.orEmpty(),
                        selectedAnswer = uiState.selectedAnswer,
                        onOptionClick = { answer ->
                            onEvent(MultiChoiceQuizScreenUiEvent.SelectAnswer(answer))
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(spaceMedium))
                    RowActionButtons(
                        answerSelected = uiState.selectedAnswer.isSelected,
                        onVerifyQuestionClick = { onEvent(MultiChoiceQuizScreenUiEvent.VerifyAnswer) },
                        onSaveQuestionClick = { onEvent(MultiChoiceQuizScreenUiEvent.SaveQuestion) }
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            BannerAd(adId = "ca-app-pub-1923025671607389/9840723939")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ColumnScope.QuizContentWidthMedium(
    uiState: MultiChoiceQuizScreenUiState,
    onEvent: (event: MultiChoiceQuizScreenUiEvent) -> Unit
) {
    val spaceMedium = MaterialTheme.spacing.medium

    AnimatedVisibility(visible = uiState.currentQuestionStep != null) {
        val currentQuestion = uiState.currentQuestionStep?.question

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spaceMedium),
            ) {
                item {
                    QuizStepViewRow(
                        modifier = Modifier.fillMaxWidth(),
                        questionSteps = uiState.questionSteps
                    )
                    Spacer(modifier = Modifier.height(spaceMedium))
                }
                if (currentQuestion != null) {
                    item {
                        Text(
                            text = uiState.getQuestionPositionFormatted(),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(spaceMedium))
                    }
                    item {
                        Text(
                            text = currentQuestion.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    // Question image, if exists
                    currentQuestion.imageUrl?.let { imageUrl ->
                        val imageScale = if (currentQuestion.category == "Flag Quiz") {
                            ContentScale.FillHeight
                        } else ContentScale.Crop

                        item {
                            Spacer(modifier = Modifier.height(spaceMedium))
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Image",
                                modifier = Modifier
                                    .aspectRatio(16 / 9f)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = imageScale
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(spaceMedium))
                    RowActionButtons(
                        answerSelected = uiState.selectedAnswer.isSelected,
                        onVerifyQuestionClick = { onEvent(MultiChoiceQuizScreenUiEvent.VerifyAnswer) },
                        onSaveQuestionClick = { onEvent(MultiChoiceQuizScreenUiEvent.SaveQuestion) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(spaceMedium))
                    BannerAd(adId = "ca-app-pub-1923025671607389/9840723939")
                }
            }
            CardQuestionAnswers(
                answers = currentQuestion?.answers.orEmpty(),
                selectedAnswer = uiState.selectedAnswer,
                onOptionClick = { answer ->
                    onEvent(MultiChoiceQuizScreenUiEvent.SelectAnswer(answer))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RowActionButtons(
    modifier: Modifier = Modifier,
    answerSelected: Boolean,
    onVerifyQuestionClick: () -> Unit,
    onSaveQuestionClick: () -> Unit,
) {
    val spaceMedium = MaterialTheme.spacing.medium

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spaceMedium, Alignment.CenterHorizontally),
        modifier = modifier.fillMaxWidth()
    ) {
        TextButton(
            onClick = onSaveQuestionClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stringResource(id = CoreR.string.save))
        }
        Button(
            onClick = onVerifyQuestionClick,
            enabled = answerSelected,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stringResource(id = CoreR.string.verify))
        }
    }
}

@Composable
@PreviewNightLight
private fun QuizScreenPreviewWidthCompact() {
    val questionSteps = listOf(
        MultiChoiceQuestionStep.Completed(
            question = getBasicMultiChoiceQuestion(),
            correct = true
        ),
        MultiChoiceQuestionStep.Completed(
            question = getBasicMultiChoiceQuestion(),
            correct = false
        ),
        MultiChoiceQuestionStep.Current(question = getBasicMultiChoiceQuestion()),
        MultiChoiceQuestionStep.NotCurrent(question = getBasicMultiChoiceQuestion()),
    )

    val uiState = remember {
        MultiChoiceQuizScreenUiState(
            questionSteps = questionSteps,
            selectedAnswer = SelectedAnswer.fromIndex((0..3).random()),
            currentQuestionIndex = 2
        )
    }

    NewQuizTheme {
        MultiChoiceQuizScreenImpl(
            uiState = uiState,
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            windowHeightSizeClass = WindowHeightSizeClass.Medium,
            onBackClick = {},
            onEvent = {},
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    device = "spec:shape=Normal,width=674,height=841,unit=dp,dpi=480"
)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:shape=Normal,width=674,height=841,unit=dp,dpi=480"
)
private fun QuizScreenPreviewWidthMedium() {
    val questionSteps = listOf(
        MultiChoiceQuestionStep.Completed(
            question = getBasicMultiChoiceQuestion(),
            correct = true
        ),
        MultiChoiceQuestionStep.Completed(
            question = getBasicMultiChoiceQuestion(),
            correct = false
        ),
        MultiChoiceQuestionStep.Current(question = getBasicMultiChoiceQuestion()),
        MultiChoiceQuestionStep.NotCurrent(question = getBasicMultiChoiceQuestion()),
    )

    val uiState = remember {
        MultiChoiceQuizScreenUiState(
            questionSteps = questionSteps,
            selectedAnswer = SelectedAnswer.fromIndex((0..3).random()),
            currentQuestionIndex = 2
        )
    }

    NewQuizTheme {
        MultiChoiceQuizScreenImpl(
            uiState = uiState,
            windowWidthSizeClass = WindowWidthSizeClass.Medium,
            windowHeightSizeClass = WindowHeightSizeClass.Medium,
            onBackClick = {},
            onEvent = {},
        )
    }
}