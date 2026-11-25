package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.i18n.GameLocaleState
import com.example.i18n.tr
import com.example.network.RemoteAppVersionInfo
import com.example.ui.theme.GameGold
import com.example.ui.theme.GameGoldLight
import com.example.ui.theme.GameSurfaceDark

@Composable
fun AppUpdateDialog(
    updateInfo: RemoteAppVersionInfo,
    onDismissLater: () -> Unit,
    onSkipVersion: () -> Unit
) {
    val context = LocalContext.current
    val changelogText = rememberChangelogText(updateInfo)

    AlertDialog(
        onDismissRequest = onDismissLater,
        modifier = Modifier.testTag("app_update_dialog"),
        containerColor = GameSurfaceDark,
        shape = RoundedCornerShape(18.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tr("🆕 Доступно обновление", "🆕 Update Available"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GameGoldLight
                )
                Text(
                    text = tr(
                        "Версия ${BuildConfig.VERSION_NAME} → ${updateInfo.versionName}",
                        "Version ${BuildConfig.VERSION_NAME} → ${updateInfo.versionName}"
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr(
                        "Вышла новая сборка игры на GitHub. Нажмите «Скачать APK», чтобы загрузить обновление напрямую.",
                        "A new game build is available on GitHub. Tap Download APK to get the update directly."
                    ),
                    fontSize = 12.sp,
                    color = Color(0xFFE6DCF8),
                    lineHeight = 16.sp
                )

                if (changelogText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF181028),
                        border = BorderStroke(1.dp, Color(0xFF4B3A6E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tr("Что нового:", "What's new:"),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GameGold
                            )
                            Text(
                                text = changelogText,
                                fontSize = 11.5.sp,
                                color = Color(0xFFDDD2F0),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val launched = runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.apkUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }.isSuccess
                        if (!launched) {
                            Toast.makeText(
                                context,
                                tr(
                                    "Не удалось открыть браузер для загрузки",
                                    "Could not open browser to download APK"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        onDismissLater()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("update_download_button")
                ) {
                    Text(
                        text = tr("⬇️ Скачать APK", "⬇️ Download APK"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSkipVersion,
                        border = BorderStroke(1.dp, Color(0xFF564677)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("update_skip_button")
                    ) {
                        Text(
                            text = tr("Пропустить", "Skip Version"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFAFA2C6),
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onDismissLater,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34264F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("update_later_button")
                    ) {
                        Text(
                            text = tr("Позже", "Later"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    )
}

@androidx.compose.runtime.Composable
private fun rememberChangelogText(info: RemoteAppVersionInfo): String {
    return if (GameLocaleState.isEnglish) {
        info.changelogEn.ifBlank { info.changelogRu }
    } else {
        info.changelogRu.ifBlank { info.changelogEn }
    }
}
