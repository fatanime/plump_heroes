package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.GameLocaleState
import com.example.i18n.tr
import com.example.model.PlayerProfile
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: PlayerProfile,
    onUpdateName: (String) -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Boolean,
    onBack: () -> Unit,
    onToggleGoogleSync: ((Boolean, String?) -> Unit)? = null,
    onToggleSoundMuted: () -> Unit = {},
    isCheckingForUpdate: Boolean = false,
    updateStatusMessage: String? = null,
    onCheckForUpdates: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage = GameLocaleState.language
    val appVersionName = com.example.BuildConfig.VERSION_NAME
    val saveFileName = "plump_heroes_save_${appVersionName.replace(" ", "_")}.json"
    var editingName by remember { mutableStateOf(profile.name) }
    var isEditingNameOpen by remember { mutableStateOf(false) }
    var jsonExportString by remember { mutableStateOf<String?>(null) }
    var isChangelogOpen by remember { mutableStateOf(false) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    // Real JSON file export launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val jsonToWrite = pendingExportJson ?: onExportJson()
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonToWrite.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                Toast.makeText(
                    context,
                    tr(
                        "Файл сохранения .json успешно создан и сохранен!",
                        "Save file .json created and saved successfully!"
                    ),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    tr("Ошибка записи файла: ${e.message}", "Failed to write file: ${e.message}"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Real JSON file import launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonContent = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!jsonContent.isNullOrBlank()) {
                    val success = onImportJson(jsonContent)
                    if (success) {
                        Toast.makeText(
                            context,
                            tr("Сохранение успешно загружено из файла .json!", "Save successfully loaded from .json file!"),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            tr("Ошибка: неверная структура данных в .json!", "Error: invalid data structure in .json!"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        tr("Выбран пустой файл", "Selected file is empty"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    tr("Не удалось прочитать файл: ${e.message}", "Could not read file: ${e.message}"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GameBgDark)
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .testTag("settings_back_button")
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("Назад", "Back"),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tr("Настройки аккаунта", "Account Settings"),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 1: Player Profile, Battle Stats, Amulets & Language Switcher
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF43355F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row with Nickname and Edit Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Имя профиля", "Profile Name"),
                            fontSize = 12.sp,
                            color = Color(0xFFAFA2C6)
                        )
                        val displayProfileName = if (profile.name == "Хранительница" && GameLocaleState.isEnglish) {
                            "Guardian"
                        } else {
                            profile.name
                        }
                        Text(
                            text = displayProfileName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GameGoldLight
                        )
                    }

                    Button(
                        onClick = { isEditingNameOpen = !isEditingNameOpen },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF382A52),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("edit_name_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEditingNameOpen) tr("Закрыть", "Close") else tr("Изменить", "Edit"),
                            fontSize = 11.sp
                        )
                    }
                }

                // Statistics below nickname taking full available width
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val totalBattles = profile.battlesWon + profile.battlesLost
                    val winPct = if (totalBattles > 0) ((profile.battlesWon * 100.0) / totalBattles).toInt() else 0
                    val lossPct = if (totalBattles > 0) ((profile.battlesLost * 100.0) / totalBattles).toInt() else 0

                    Text(
                        text = tr(
                            "Количество выигранных боев: ${profile.battlesWon} ($winPct%)",
                            "Battles Won: ${profile.battlesWon} ($winPct%)"
                        ),
                        fontSize = 12.sp,
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = tr(
                            "Количество проигранных боев: ${profile.battlesLost} ($lossPct%)",
                            "Battles Lost: ${profile.battlesLost} ($lossPct%)"
                        ),
                        fontSize = 12.sp,
                        color = Color(0xFFE57373),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = tr(
                            "Колизей — побед: ${profile.coliseumWon} • поражений: ${profile.coliseumLost}",
                            "Coliseum — wins: ${profile.coliseumWon} • losses: ${profile.coliseumLost}"
                        ),
                        fontSize = 12.sp,
                        color = GameGoldLight,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("settings_coliseum_stats")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tr("Язык игры:", "Game Language:"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GameGoldLight
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isRu = currentLanguage == AppLanguage.RU
                        val isEn = currentLanguage == AppLanguage.EN

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isRu) Color(0xFF3E2D60) else Color(0xFF211734),
                            border = BorderStroke(
                                width = if (isRu) 1.6.dp else 1.dp,
                                color = if (isRu) GameGold else Color(0xFF4C3C6C)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    GameLocaleState.setLanguage(context, AppLanguage.RU)
                                }
                                .testTag("settings_lang_ru")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "🇷🇺 Русский",
                                    fontSize = 13.sp,
                                    fontWeight = if (isRu) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isRu) GameGoldLight else Color(0xFFCCC2DF),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isEn) Color(0xFF3E2D60) else Color(0xFF211734),
                            border = BorderStroke(
                                width = if (isEn) 1.6.dp else 1.dp,
                                color = if (isEn) GameGold else Color(0xFF4C3C6C)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    GameLocaleState.setLanguage(context, AppLanguage.EN)
                                }
                                .testTag("settings_lang_en")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "🇬🇧English",
                                    fontSize = 13.sp,
                                    fontWeight = if (isEn) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isEn) GameGoldLight else Color(0xFFCCC2DF),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tr("Звук и музыка:", "Sound & Music:"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GameGoldLight
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!profile.isSoundMuted) Color(0xFF3E2D60) else Color(0xFF211734),
                        border = BorderStroke(
                            width = if (!profile.isSoundMuted) 1.5.dp else 1.2.dp,
                            color = if (!profile.isSoundMuted) GameGold else Color(0xFFE57373)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleSoundMuted() }
                            .testTag("settings_sound_toggle")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (profile.isSoundMuted) {
                                    tr("🔇 Звук выключен", "🔇 Sound Off")
                                } else {
                                    tr("🔊 Звук включён", "🔊 Sound On")
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (profile.isSoundMuted) Color(0xFFFFAB91) else GameGoldLight
                            )
                            Text(
                                text = if (profile.isSoundMuted) {
                                    tr("Включить", "Turn On")
                                } else {
                                    tr("Выключить", "Turn Off")
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (profile.isSoundMuted) Color(0xFF81C784) else Color(0xFFD5CBE9)
                            )
                        }
                    }
                }

                if (isEditingNameOpen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editingName,
                            onValueChange = { editingName = it },
                            label = { Text(tr("Новое имя", "New Name"), fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GameGold,
                                unfocusedBorderColor = Color(0xFF564775),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_name_input")
                        )

                        Button(
                            onClick = {
                                onUpdateName(editingName)
                                isEditingNameOpen = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GameGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("save_player_name_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION: Progress Persistence (Save & Restore)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF43355F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr("Сохранение и восстановление прогресса", "Save & Restore Progress"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = tr(
                        "Данные хранятся в формате .json на устройствах пользователя. При обновлении приложения данные сохраняются, но при полной перестановке данные необходимо восстановить из файла",
                        "Your progress is stored in a .json file on your device. Updates keep your data intact, but after a fresh reinstall you will need to restore it from your save file."
                    ),
                    fontSize = 11.sp,
                    color = Color(0xFFAFA2C6),
                    lineHeight = 15.sp
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val json = onExportJson()
                                pendingExportJson = json
                                jsonExportString = json
                                createDocumentLauncher.launch(saveFileName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button")
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_file_upload), contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("Сохранить прогресс", "Save Progress"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_json_button")
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_file_download), contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("Восстановить прогресс", "Restore Progress"),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (jsonExportString != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1B2C24),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✅", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        "Файл сохранения $saveFileName готов!",
                                        "Save file $saveFileName is ready!"
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA5D6A7)
                                )
                                Text(
                                    text = tr(
                                        "Все герои, артефакты, прогресс карьеры и арены сохранены.",
                                        "All heroines, artifacts, Career and Arena progress have been saved."
                                    ),
                                    fontSize = 9.sp,
                                    color = Color(0xFFE8F5E9)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 4: App Version & Expandable Changelog
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF43355F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tr("Пышные героини (Plump Heroes)", "Plump Heroes"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameGoldLight
                )

                // Clickable Version Row
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isChangelogOpen) Color(0xFF382956) else Color(0xFF221A33),
                    border = BorderStroke(1.dp, if (isChangelogOpen) GameGold else Color(0xFF483A64)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isChangelogOpen = !isChangelogOpen }
                        .testTag("version_changelog_toggle")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tr("Версия приложения: $appVersionName", "App Version: $appVersionName"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isChangelogOpen) {
                                    tr("Нажмите, чтобы скрыть список изменений ▲", "Tap to hide changelog ▲")
                                } else {
                                    tr("Нажмите, чтобы узнать, что нового в $appVersionName ▼", "Tap to see what's new in $appVersionName ▼")
                                },
                                fontSize = 10.sp,
                                color = GameGold
                            )
                        }
                        Text(
                            text = if (isChangelogOpen) "▲" else "▼",
                            color = GameGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Expanded Changelog
                if (isChangelogOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF140D22), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF3B2A59), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tr("🌟 Что нового в версии $appVersionName:", "🌟 What's new in $appVersionName:"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GameGoldLight
                        )

                        Text(
                            text = tr(
                                "Добавлена новая героиня Лея со своими способностями, лесным фоном и появлением в Карьере, Арене, Колизее и Башне. Усилена базовая атака Ми и Ны с игнорированием щита. Также добавлены боевые диалоги и портреты лиц для новых героинь.",
                                "Added a new heroine, Leya, with her skills, forest background, and appearances in Career, Arena, Coliseum, and Tower. Buffed Mi & Na's basic attack with shield bypass. Also added battle dialogues and dialogue face portraits for new heroines."
                            ),
                            fontSize = 11.sp,
                            color = Color(0xFFDDD2F0),
                            lineHeight = 15.sp
                        )
                    }
                }

                Button(
                    onClick = onCheckForUpdates,
                    enabled = !isCheckingForUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3E2D60),
                        contentColor = GameGoldLight
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("settings_check_updates_button")
                ) {
                    if (isCheckingForUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = GameGoldLight,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("Проверяем GitHub...", "Checking GitHub..."),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Проверить обновления", "Check for Updates"),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (!updateStatusMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1F1633),
                        border = BorderStroke(1.dp, Color(0xFF5A4780)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_update_status_banner")
                    ) {
                        Text(
                            text = updateStatusMessage,
                            fontSize = 11.sp,
                            color = Color(0xFFE6DCF8),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 5: Creators & Social Links
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GameSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF43355F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr("Создатели:", "Creators:"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = tr(
                            "Пухляшникофф - лор игры и концепт персонажей",
                            "Puhlyashnikoff - game lore and character concept"
                        ),
                        fontSize = 11.sp,
                        color = Color(0xFFDDD2F0),
                        lineHeight = 15.sp
                    )
                    Text(
                        text = tr(
                            "M.A.D. R.S.D. - вайбкодинг и визуал",
                            "M.A.D. R.S.D. - vibecoding and visuals"
                        ),
                        fontSize = 11.sp,
                        color = Color(0xFFDDD2F0),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tr(
                            "Отдельная благодарность тестировщикам:\n1) Текки\n2) DenpeccuR",
                            "Special thanks to testers:\n1) Tekki\n2) DenpeccuR"
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GameGoldLight,
                        lineHeight = 15.sp
                    )
                }

                HorizontalDivider(color = Color(0xFF3B2A59), thickness = 1.dp)

                // VK Group Link Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2787F5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://vk.com/plump_heroes"))
                            context.startActivity(intent)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🌐", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tr("Официальная группа в Вконтакте", "Official VK Community"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "https://vk.com/plump_heroes",
                                fontSize = 9.sp,
                                color = Color(0xFFE3F2FD),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Telegram Channel Link Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0088CC),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/+DM21Tcs-x4liODcy"))
                            context.startActivity(intent)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✈️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tr("Телеграм канал Пухляшникофф", "Puhlyashnikoff Telegram Channel"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "https://t.me/+DM21Tcs-x4liODcy",
                                fontSize = 9.sp,
                                color = Color(0xFFE1F5FE),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
