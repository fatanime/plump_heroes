package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.i18n.GameLocaleState
import com.example.i18n.getColiseumTutorialFrameEn
import com.example.i18n.getTutorialFrameEn
import com.example.i18n.localizedDisplayName
import com.example.i18n.tr
import com.example.ui.theme.GameGold
import com.example.ui.theme.GameGoldLight
import kotlinx.coroutines.delay

import com.example.ui.screens.HubBattleSection

enum class FairySpeaker(val displayName: String, val spriteRes: Int, val badgeColor: Color) {
    GREENPIE("Гринпай", R.drawable.fairy_greenpie, Color(0xFF2E6B34)),
    BLUESWEET("Блюсвит", R.drawable.fairy_bluesweet, Color(0xFF5D203B))
}

data class TutorialFrameData(
    val step: Int,
    val speaker: FairySpeaker,
    val targetTab: Int, // 0: Герои, 1: Кузница, 2: Хаб, 3: Сундуки, 4: Трофеи
    val isChoice: Boolean = false,
    val textTemplate: String,
    val targetHubSection: HubBattleSection? = null
)

@Composable
fun TutorialOverlay(
    step: Int,
    playerName: String,
    onAdvanceStep: () -> Unit,
    onSkipToFinal: () -> Unit,
    onStartFromFrame5: () -> Unit,
    onFinishTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frameData = remember(step, playerName, GameLocaleState.language) {
        getTutorialFrame(step, playerName)
    }
    val fullText = frameData.textTemplate

    var visibleChars by remember(step) { mutableIntStateOf(0) }
    val isTypingComplete = visibleChars >= fullText.length

    // Fast and smooth typewriter animation (approx 22ms per character)
    LaunchedEffect(step, fullText) {
        visibleChars = 0
        while (visibleChars < fullText.length) {
            delay(22)
            visibleChars++
        }
    }

    // Full screen blocking container that intercepts all taps outside the dialogue
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("tutorial_overlay")
    ) {
        val fairyWidth = (maxWidth * 0.28f).coerceIn(96.dp, 138.dp)
        val fairyHeight = (fairyWidth * 1.32f).coerceAtMost(maxHeight * 0.34f)
        val fairyEndMargin = 4.dp
        val textEndPadding = fairyWidth
        val maxDialogTextHeight = (maxHeight * 0.36f).coerceAtLeast(136.dp)

        // Subtle dim overlay to enhance dialogue readability while keeping current screen clearly visible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x55090414))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isTypingComplete) {
                        visibleChars = fullText.length
                    }
                }
        )

        // Dialogue Box at the bottom of the screen (zIndex = 2f)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .zIndex(2f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isTypingComplete) {
                            visibleChars = fullText.length
                        } else if (!frameData.isChoice) {
                            if (step >= 28) {
                                onFinishTutorial()
                            } else {
                                onAdvanceStep()
                            }
                        }
                    }
                    .testTag("tutorial_dialog_box"),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xF2180F2A),
                border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
                shadowElevation = 10.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 20.dp, bottom = 12.dp, end = textEndPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxDialogTextHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Full text without line truncation so it is always completely visible
                        Text(
                            text = fullText.take(visibleChars),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFF9E6),
                            lineHeight = 16.5.sp,
                            modifier = Modifier.testTag("tutorial_dialog_text")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Flashing tap indicator when text is complete and not a choice step
                        if (isTypingComplete && !frameData.isChoice) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr("Нажмите для продолжения ▼", "Tap to continue ▼"),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GameGoldLight
                                )
                            }
                        }
                    }
                }
            }

            // Speaker Name Badge (Positioned at top-left of the box, as in artefact.jpg)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 12.dp, y = (-10).dp),
                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 4.dp, bottomStart = 4.dp),
                color = frameData.speaker.badgeColor,
                border = BorderStroke(1.2.dp, Color(0xFFFFD54F)),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = frameData.speaker.localizedDisplayName(),
                    color = Color(0xFFFFE082),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }

        // Choice Buttons in the screen center for Frame 4 and Frame 20
        if (frameData.isChoice && isTypingComplete) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF51E1333),
                    border = BorderStroke(1.5.dp, GameGold),
                    shadowElevation = 14.dp,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (step == 4) {
                                tr("Начать обучение?", "Start tutorial?")
                            } else {
                                tr("Всё ли понятно?", "Is everything clear?")
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GameGoldLight
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Green Button: "Да" / "Yes"
                            Button(
                                onClick = {
                                    if (step == 4) {
                                        onAdvanceStep() // Go to frame 5
                                    } else {
                                        onAdvanceStep() // Step 26 -> Go to frame 27
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .widthIn(min = 100.dp)
                                    .height(44.dp)
                                    .testTag("tutorial_choice_yes")
                            ) {
                                Text(
                                    text = tr("Да", "Yes"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Red Button: "Нет" / "No"
                            Button(
                                onClick = {
                                    if (step == 4) {
                                        onSkipToFinal() // Skip directly to frame 28
                                    } else {
                                        onStartFromFrame5() // Repeat tutorial from frame 5
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFC62828),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .widthIn(min = 100.dp)
                                    .height(44.dp)
                                    .testTag("tutorial_choice_no")
                            ) {
                                Text(
                                    text = tr("Нет", "No"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fairy Sprite emerging from bottom-right with small edge margin and zero text overlap (zIndex = 10f)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = fairyEndMargin, bottom = 8.dp)
                .zIndex(10f)
        ) {
            Image(
                painter = painterResource(id = frameData.speaker.spriteRes),
                contentDescription = frameData.speaker.localizedDisplayName(),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(fairyHeight)
                    .width(fairyWidth)
                    .testTag("tutorial_fairy_sprite")
            )
        }
    }
}

fun getTutorialFrame(step: Int, playerName: String): TutorialFrameData {
    if (GameLocaleState.isEnglish) {
        return getTutorialFrameEn(step, playerName)
    }
    return when (step) {
        1 -> TutorialFrameData(
            step = 1,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 0,
            textTemplate = "Приветствую, $playerName! Добро пожаловать в Аврезию, мир где царит магия и полнота!"
        )
        2 -> TutorialFrameData(
            step = 2,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "Сестра, это же просто очередной человек, что скачал себе на телефон эту игру!"
        )
        3 -> TutorialFrameData(
            step = 3,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "Ты же здесь чтобы на полненьких девушек посмотреть, да? Раскусила?"
        )
        4 -> TutorialFrameData(
            step = 4,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 0,
            isChoice = true,
            textTemplate = "Простите мою сестру, она вечно все портит. Начнём обучение?"
        )
        5 -> TutorialFrameData(
            step = 5,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 0,
            textTemplate = "Это наши герои. Точнее героини. Из них состоит ваша боевая колода."
        )
        6 -> TutorialFrameData(
            step = 6,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 0,
            textTemplate = "Сейчас у вас лишь 4 девушки, но потом у вас их будет гораздо больше!"
        )
        7 -> TutorialFrameData(
            step = 7,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "Скука! Из-за таких как ты, сестра обучение и не проходят. Учись!"
        )
        8 -> TutorialFrameData(
            step = 8,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "Главное - у каждой красотки свои характеристики и способности. Все в их карточках."
        )
        9 -> TutorialFrameData(
            step = 9,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "Находи артефакты, собирай частицы и девушки станут сильнее!"
        )
        10 -> TutorialFrameData(
            step = 10,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 0,
            textTemplate = "А еще, в нашем мире чем сильнее ты, тем полнее!"
        )
        11 -> TutorialFrameData(
            step = 11,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 0,
            textTemplate = "Поэтому, повышая уровень девушки становятся толще!"
        )
        12 -> TutorialFrameData(
            step = 12,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 1, // Кузница
            textTemplate = "В карьере вы получаете артефакты. Если у вас их много, можете создать из них новый."
        )
        13 -> TutorialFrameData(
            step = 13,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 1, // Кузница
            textTemplate = "Новый артефакт сильнее старых. Но от уровня карты зависит какой артефакт ей подойдёт!"
        )
        14 -> TutorialFrameData(
            step = 14,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 1, // Кузница
            textTemplate = "Прокачивай уровень и вам откроются способности и возможность надеть артефакт посильнее!"
        )
        15 -> TutorialFrameData(
            step = 15,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2, // Хаб (Цитадель Аврезии)
            textTemplate = "А здесь у нас Цитадель Аврезии или же Хаб."
        )
        16 -> TutorialFrameData(
            step = 16,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2, // Хаб
            textTemplate = "Здесь находятся все режимы игры."
        )
        17 -> TutorialFrameData(
            step = 17,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2, // Хаб
            textTemplate = "Их тут много. Давай я расскажу о главных, сестра!"
        )
        18 -> TutorialFrameData(
            step = 18,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2, // Хаб -> Карьера
            targetHubSection = HubBattleSection.CAREER,
            textTemplate = "Тут карьера. Точнее её жалкое подобие. Проходи уровни, бей боссов, открывай новых дамочек!"
        )
        19 -> TutorialFrameData(
            step = 19,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2, // Хаб -> Карьера
            targetHubSection = HubBattleSection.CAREER,
            textTemplate = "На каждом уровне вас ждёт отряд врагов. Сила отряда написана в каждом уровне."
        )
        20 -> TutorialFrameData(
            step = 20,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2, // Хаб -> Карьера
            targetHubSection = HubBattleSection.CAREER,
            textTemplate = "Карты трех типов: Ближний бой, дальний и Смешанный. Каждый бьёт свою линию."
        )
        21 -> TutorialFrameData(
            step = 21,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2, // Хаб -> Арена
            targetHubSection = HubBattleSection.ARENA,
            textTemplate = "Тут в боях с ботами копим валюту. Правильно, сестра?"
        )
        22 -> TutorialFrameData(
            step = 22,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2, // Хаб -> Арена
            targetHubSection = HubBattleSection.ARENA,
            textTemplate = "У вас есть пять ключей. Каждый заход на арену стоит 1 ключ."
        )
        23 -> TutorialFrameData(
            step = 23,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2, // Хаб -> Арена
            targetHubSection = HubBattleSection.ARENA,
            textTemplate = "Но не переживай, $playerName! Ключи восстановятся со временем."
        )
        24 -> TutorialFrameData(
            step = 24,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2, // Хаб
            textTemplate = "Остальные режимы доступны лишь опытным воинам."
        )
        25 -> TutorialFrameData(
            step = 25,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 3, // Сундуки
            textTemplate = "За валюту открывайте сундуки, получая частицы карт или артефакты! На этом все."
        )
        26 -> TutorialFrameData(
            step = 26,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 3, // Сундуки
            isChoice = true,
            textTemplate = "Все ли понятно, сладкий пирожочек?"
        )
        27 -> TutorialFrameData(
            step = 27,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2, // Хаб
            textTemplate = "Если захочешь сохранить прогресс, нажми вверху на шестеренку, а там уже сохрани."
        )
        else -> TutorialFrameData(
            step = 28,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2, // Хаб
            textTemplate = "Монет много, а алмазов ноль? Нажми на иконку алмазов или монет сверху и конвентируй валюту. Чао!"
        )
    }
}

fun getColiseumTutorialFrame(step: Int): TutorialFrameData {
    if (GameLocaleState.isEnglish) {
        return getColiseumTutorialFrameEn(step)
    }
    return when (step) {
        1 -> TutorialFrameData(
            step = 1,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            textTemplate = "Это Колизей. Здесь арены идут друг за дружкой."
        )
        2 -> TutorialFrameData(
            step = 2,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            textTemplate = "Так что прокачай своих дамочек прежде чем сюда идти!"
        )
        else -> TutorialFrameData(
            step = 3,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            textTemplate = "Чем дальше пройдешь - тем сложнее и больше приз!"
        )
    }
}

@Composable
fun ColiseumTutorialOverlay(
    step: Int,
    onAdvanceStep: () -> Unit,
    onFinishTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frameData = remember(step, GameLocaleState.language) {
        getColiseumTutorialFrame(step)
    }
    val fullText = frameData.textTemplate

    var visibleChars by remember(step) { mutableIntStateOf(0) }
    val isTypingComplete = visibleChars >= fullText.length

    LaunchedEffect(step, fullText) {
        visibleChars = 0
        while (visibleChars < fullText.length) {
            delay(22)
            visibleChars++
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("coliseum_tutorial_overlay")
    ) {
        val fairyWidth = (maxWidth * 0.28f).coerceIn(96.dp, 138.dp)
        val fairyHeight = (fairyWidth * 1.32f).coerceAtMost(maxHeight * 0.34f)
        val fairyEndMargin = 4.dp
        val textEndPadding = fairyWidth
        val maxDialogTextHeight = (maxHeight * 0.36f).coerceAtLeast(136.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66090414))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isTypingComplete) {
                        visibleChars = fullText.length
                    } else if (step >= 3) {
                        onFinishTutorial()
                    } else {
                        onAdvanceStep()
                    }
                }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .zIndex(2f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isTypingComplete) {
                            visibleChars = fullText.length
                        } else if (step >= 3) {
                            onFinishTutorial()
                        } else {
                            onAdvanceStep()
                        }
                    }
                    .testTag("coliseum_tutorial_dialog_box"),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xF2180F2A),
                border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
                shadowElevation = 10.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 20.dp, bottom = 12.dp, end = textEndPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxDialogTextHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = fullText.take(visibleChars),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFF9E6),
                            lineHeight = 16.5.sp,
                            modifier = Modifier.testTag("coliseum_tutorial_dialog_text")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (isTypingComplete) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr("Нажмите для продолжения ▼", "Tap to continue ▼"),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GameGoldLight
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 12.dp, y = (-10).dp),
                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 4.dp, bottomStart = 4.dp),
                color = frameData.speaker.badgeColor,
                border = BorderStroke(1.2.dp, Color(0xFFFFD54F)),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = frameData.speaker.localizedDisplayName(),
                    color = Color(0xFFFFE082),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = fairyEndMargin, bottom = 8.dp)
                .zIndex(10f)
        ) {
            Image(
                painter = painterResource(id = frameData.speaker.spriteRes),
                contentDescription = frameData.speaker.localizedDisplayName(),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(fairyHeight)
                    .width(fairyWidth)
                    .testTag("coliseum_tutorial_fairy_sprite")
            )
        }
    }
}

fun getTowerTutorialFrame(step: Int): TutorialFrameData {
    if (GameLocaleState.isEnglish) {
        return com.example.i18n.getTowerTutorialFrameEn(step)
    }
    return when (step) {
        1 -> TutorialFrameData(
            step = 1,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "Добро пожаловать в Башню страданий! Вуаха!"
        )
        2 -> TutorialFrameData(
            step = 2,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "Сестра! Не выдумывай! Это Башня испытаний!"
        )
        3 -> TutorialFrameData(
            step = 3,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "В зависимости от дня недели здесь разные правила."
        )
        4 -> TutorialFrameData(
            step = 4,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "А иногда бывают и тематические дни. Не пропусти!"
        )
        5 -> TutorialFrameData(
            step = 5,
            speaker = FairySpeaker.GREENPIE,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "В отличие от Колизея прогресс сохраняется до конца дня."
        )
        else -> TutorialFrameData(
            step = 6,
            speaker = FairySpeaker.BLUESWEET,
            targetTab = 2,
            targetHubSection = HubBattleSection.TOWER,
            textTemplate = "Но у вас только 2 попытки. Удачи!"
        )
    }
}

@Composable
fun TowerTutorialOverlay(
    step: Int,
    onAdvanceStep: () -> Unit,
    onFinishTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frameData = remember(step, GameLocaleState.language) {
        getTowerTutorialFrame(step)
    }
    val fullText = frameData.textTemplate

    var visibleChars by remember(step) { mutableIntStateOf(0) }
    val isTypingComplete = visibleChars >= fullText.length

    LaunchedEffect(step, fullText) {
        visibleChars = 0
        while (visibleChars < fullText.length) {
            delay(22)
            visibleChars++
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("tower_tutorial_overlay")
    ) {
        val fairyWidth = (maxWidth * 0.28f).coerceIn(96.dp, 138.dp)
        val fairyHeight = (fairyWidth * 1.32f).coerceAtMost(maxHeight * 0.34f)
        val fairyEndMargin = 4.dp
        val textEndPadding = fairyWidth
        val maxDialogTextHeight = (maxHeight * 0.36f).coerceAtLeast(136.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66090414))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isTypingComplete) {
                        visibleChars = fullText.length
                    } else if (step >= 6) {
                        onFinishTutorial()
                    } else {
                        onAdvanceStep()
                    }
                }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .zIndex(2f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isTypingComplete) {
                            visibleChars = fullText.length
                        } else if (step >= 6) {
                            onFinishTutorial()
                        } else {
                            onAdvanceStep()
                        }
                    }
                    .testTag("tower_tutorial_dialog_box"),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xF2180F2A),
                border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
                shadowElevation = 10.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 20.dp, bottom = 12.dp, end = textEndPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxDialogTextHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = fullText.take(visibleChars),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFF9E6),
                            lineHeight = 16.5.sp,
                            modifier = Modifier.testTag("tower_tutorial_dialog_text")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (isTypingComplete) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr("Нажмите для продолжения ▼", "Tap to continue ▼"),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GameGoldLight
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 12.dp, y = (-10).dp),
                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 4.dp, bottomStart = 4.dp),
                color = frameData.speaker.badgeColor,
                border = BorderStroke(1.2.dp, Color(0xFFFFD54F)),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = frameData.speaker.localizedDisplayName(),
                    color = Color(0xFFFFE082),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = fairyEndMargin, bottom = 8.dp)
                .zIndex(10f)
        ) {
            Image(
                painter = painterResource(id = frameData.speaker.spriteRes),
                contentDescription = frameData.speaker.localizedDisplayName(),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(fairyHeight)
                    .width(fairyWidth)
                    .testTag("tower_tutorial_fairy_sprite")
            )
        }
    }
}
