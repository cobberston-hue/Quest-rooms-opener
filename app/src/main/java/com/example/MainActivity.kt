package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElegantBackground
import com.example.ui.theme.ElegantBorder
import com.example.ui.theme.ElegantError
import com.example.ui.theme.ElegantErrorContainer
import com.example.ui.theme.ElegantOnPrimary
import com.example.ui.theme.ElegantOnPrimaryContainer
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.ElegantPrimaryContainer
import com.example.ui.theme.ElegantSuccess
import com.example.ui.theme.ElegantSuccessContainer
import com.example.ui.theme.ElegantSurface
import com.example.ui.theme.ElegantSurfaceVariant
import com.example.ui.theme.ElegantTextMuted
import com.example.ui.theme.ElegantTextPrimary
import com.example.ui.theme.ElegantTextSecondary
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val prefs = getSharedPreferences("quest_room_launcher_prefs", Context.MODE_PRIVATE)
    val initialAutoLaunch = prefs.getBoolean("auto_launch", false)

    setContent {
      MyApplicationTheme {
        RoomLauncherApp(
          initialAutoLaunch = initialAutoLaunch,
          onSaveAutoLaunch = { enabled ->
            prefs.edit().putBoolean("auto_launch", enabled).apply()
          }
        )
      }
    }
  }
}

enum class LaunchStatusType {
  IDLE,
  SUCCESS,
  ERROR
}

data class LaunchStatus(
  val type: LaunchStatusType = LaunchStatusType.IDLE,
  val message: String = "",
  val detail: String = ""
)

data class PresetUri(
  val label: String,
  val uri: String,
  val icon: ImageVector,
  val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomLauncherApp(
  initialAutoLaunch: Boolean = false,
  onSaveAutoLaunch: (Boolean) -> Unit = {}
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var selectedUri by remember { mutableStateOf("/home") }
  var isAutoLaunchEnabled by remember { mutableStateOf(initialAutoLaunch) }
  var countdownSeconds by remember { mutableIntStateOf(if (initialAutoLaunch) 3 else 0) }
  var isAutoLaunching by remember { mutableStateOf(initialAutoLaunch) }
  var launchStatus by remember { mutableStateOf(LaunchStatus()) }

  val presets = remember {
    listOf(
      PresetUri(
        label = "Room / Home (/home)",
        uri = "/home",
        icon = Icons.Default.Home,
        description = "Secret menu for changing Quest virtual room environment"
      ),
      PresetUri(
        label = "Experimental (/experimental)",
        uri = "/experimental",
        icon = Icons.Default.Science,
        description = "Quest experimental features and beta settings"
      ),
      PresetUri(
        label = "Guardian (/guardian)",
        uri = "/guardian",
        icon = Icons.Default.Security,
        description = "Boundary and room tracking configuration"
      ),
      PresetUri(
        label = "Display (/display)",
        uri = "/display",
        icon = Icons.Default.Tv,
        description = "Brightness, refresh rate, and display preferences"
      )
    )
  }

  fun triggerLaunch(uri: String) {
    val result = executeOculusCommand(context, uri)
    launchStatus = if (result.isSuccess) {
      LaunchStatus(
        type = LaunchStatusType.SUCCESS,
        message = context.getString(R.string.status_success),
        detail = "Launched com.oculus.panelapp.settings/.SettingsActivity with uri \"$uri\""
      )
    } else {
      val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
      LaunchStatus(
        type = LaunchStatusType.ERROR,
        message = "Could not launch activity directly",
        detail = errorMsg
      )
    }
  }

  // Handle countdown if auto-launch is enabled
  LaunchedEffect(isAutoLaunching) {
    if (isAutoLaunching && countdownSeconds > 0) {
      while (countdownSeconds > 0 && isAutoLaunching) {
        delay(1000L)
        countdownSeconds -= 1
      }
      if (isAutoLaunching) {
        triggerLaunch(selectedUri)
        isAutoLaunching = false
      }
    }
  }

  val commandText = "am start -n com.oculus.panelapp.settings/.SettingsActivity --es \"uri\" \"$selectedUri\""

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing),
    containerColor = ElegantBackground,
    topBar = {
      TopAppBar(
        title = {
          Row(
            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = ElegantTextPrimary
              )
              Text(
                text = "Advanced Settings Utility",
                style = MaterialTheme.typography.bodySmall,
                color = ElegantTextSecondary
              )
            }
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ElegantSurface)
                .border(BorderStroke(1.dp, ElegantBorder), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = ElegantPrimary,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = ElegantBackground
        )
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentAlignment = Alignment.TopCenter
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 640.dp)
          .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 36.dp, top = 8.dp)
      ) {
        // Auto-Launch banner if active
        if (isAutoLaunching && countdownSeconds > 0) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(24.dp),
              colors = CardDefaults.cardColors(containerColor = ElegantSurface),
              border = BorderStroke(1.dp, ElegantPrimary)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Auto-launching in $countdownSeconds…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElegantPrimary
                  )
                  Text(
                    text = "Opening room settings menu automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElegantTextSecondary
                  )
                }
                Button(
                  onClick = { isAutoLaunching = false },
                  colors = ButtonDefaults.buttonColors(containerColor = ElegantErrorContainer, contentColor = ElegantError),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text("Cancel")
                }
              }
            }
          }
        }

        // Hero Action Card
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            border = BorderStroke(1.dp, ElegantBorder)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              // Target Intent Header
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElegantPrimaryContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = ElegantOnPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Column {
                  Text(
                    text = "TARGET INTENT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = ElegantPrimary
                  )
                  Text(
                    text = "com.oculus.panelapp.settings",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = ElegantTextPrimary
                  )
                }
              }

              // Command snippet box
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = ElegantBackground,
                border = BorderStroke(1.dp, ElegantBorder)
              ) {
                Text(
                  text = commandText,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 12.sp,
                  color = ElegantTextSecondary,
                  lineHeight = 18.sp,
                  modifier = Modifier.padding(14.dp)
                )
              }

              // Primary Launch Button
              Button(
                onClick = {
                  isAutoLaunching = false
                  triggerLaunch(selectedUri)
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(56.dp)
                  .testTag("launch_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                  containerColor = ElegantPrimary,
                  contentColor = ElegantOnPrimary
                )
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = null,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "OPEN ROOM EDITOR",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  letterSpacing = 0.8.sp
                )
              }
            }
          }
        }

        // Quick Stats / Mode Tiles Row
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Card(
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(24.dp),
              colors = CardDefaults.cardColors(containerColor = ElegantSurface),
              border = BorderStroke(1.dp, ElegantBorder)
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = ElegantPrimary,
                  modifier = Modifier.size(24.dp)
                )
                Text(
                  text = "Auto-Launch",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = ElegantTextPrimary
                )
                Text(
                  text = if (isAutoLaunchEnabled) "Enabled on boot" else "Off by default",
                  style = MaterialTheme.typography.bodySmall,
                  color = ElegantTextMuted
                )
              }
            }

            Card(
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(24.dp),
              colors = CardDefaults.cardColors(containerColor = ElegantSurface),
              border = BorderStroke(1.dp, ElegantBorder)
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Home,
                  contentDescription = null,
                  tint = ElegantPrimary,
                  modifier = Modifier.size(24.dp)
                )
                Text(
                  text = "Target URI",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = ElegantTextPrimary
                )
                Text(
                  text = selectedUri,
                  style = MaterialTheme.typography.bodySmall,
                  fontFamily = FontFamily.Monospace,
                  color = ElegantTextMuted
                )
              }
            }
          }
        }

        // Status Card (if triggered)
        if (launchStatus.type != LaunchStatusType.IDLE) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(24.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (launchStatus.type == LaunchStatusType.SUCCESS)
                  ElegantSuccessContainer.copy(alpha = 0.35f)
                else
                  ElegantSurfaceVariant
              ),
              border = BorderStroke(
                1.dp,
                if (launchStatus.type == LaunchStatusType.SUCCESS)
                  ElegantSuccess.copy(alpha = 0.5f)
                else
                  ElegantBorder
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                Icon(
                  imageVector = if (launchStatus.type == LaunchStatusType.SUCCESS)
                    Icons.Default.CheckCircle
                  else
                    Icons.Default.Info,
                  contentDescription = null,
                  tint = if (launchStatus.type == LaunchStatusType.SUCCESS)
                    ElegantSuccess
                  else
                    ElegantPrimary,
                  modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = launchStatus.message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElegantTextPrimary
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = launchStatus.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = ElegantTextSecondary
                  )
                  if (launchStatus.type == LaunchStatusType.ERROR) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = "Note: Oculus OS system activities exist natively on Meta Quest headsets.",
                      style = MaterialTheme.typography.labelSmall,
                      color = ElegantPrimary
                    )
                  }
                }
              }
            }
          }
        }

        // Target URI Presets & Custom Input
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            border = BorderStroke(1.dp, ElegantBorder)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text(
                text = stringResource(R.string.quick_actions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ElegantTextPrimary
              )

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                  val isSelected = selectedUri == preset.uri
                  Surface(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .clickable { selectedUri = preset.uri },
                    color = if (isSelected) ElegantPrimaryContainer.copy(alpha = 0.35f) else ElegantBackground,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                      1.dp,
                      if (isSelected) ElegantPrimary else ElegantBorder
                    )
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                      Icon(
                        imageVector = preset.icon,
                        contentDescription = null,
                        tint = if (isSelected) ElegantPrimary else ElegantTextMuted,
                        modifier = Modifier.size(22.dp)
                      )
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = preset.label,
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                          color = if (isSelected) ElegantTextPrimary else ElegantTextSecondary
                        )
                        Text(
                          text = preset.description,
                          style = MaterialTheme.typography.labelSmall,
                          color = ElegantTextMuted
                        )
                      }
                      if (isSelected) {
                        Box(
                          modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ElegantPrimary)
                        )
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(4.dp))

              OutlinedTextField(
                value = selectedUri,
                onValueChange = { selectedUri = it },
                label = { Text("Custom URI parameter (--es uri)") },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("custom_uri_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = ElegantPrimary,
                  unfocusedBorderColor = ElegantBorder,
                  focusedTextColor = ElegantTextPrimary,
                  unfocusedTextColor = ElegantTextPrimary,
                  cursorColor = ElegantPrimary,
                  focusedLabelColor = ElegantPrimary,
                  unfocusedLabelColor = ElegantTextSecondary,
                  focusedContainerColor = ElegantBackground,
                  unfocusedContainerColor = ElegantBackground
                )
              )
            }
          }
        }

        // Direct ADB Command Card
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            border = BorderStroke(1.dp, ElegantBorder)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = stringResource(R.string.adb_command_title),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = ElegantTextPrimary
                )

                IconButton(
                  onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("ADB Command", commandText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.adb_command_copied), Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.testTag("copy_adb_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy command",
                    tint = ElegantPrimary
                  )
                }
              }

              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ElegantBackground,
                border = BorderStroke(1.dp, ElegantBorder)
              ) {
                Text(
                  text = commandText,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 12.sp,
                  color = ElegantTextSecondary,
                  lineHeight = 18.sp,
                  modifier = Modifier.padding(14.dp)
                )
              }

              Text(
                text = "Target: com.oculus.panelapp.settings/.SettingsActivity",
                style = MaterialTheme.typography.labelSmall,
                color = ElegantTextMuted
              )
            }
          }
        }

        // Auto-Launch Setting Switch Card
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            border = BorderStroke(1.dp, ElegantBorder)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                  text = stringResource(R.string.launch_auto_label),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = ElegantTextPrimary
                )
                Text(
                  text = stringResource(R.string.launch_auto_description),
                  style = MaterialTheme.typography.bodySmall,
                  color = ElegantTextSecondary
                )
              }

              Switch(
                checked = isAutoLaunchEnabled,
                onCheckedChange = { checked ->
                  isAutoLaunchEnabled = checked
                  onSaveAutoLaunch(checked)
                },
                modifier = Modifier.testTag("auto_launch_switch"),
                colors = SwitchDefaults.colors(
                  checkedThumbColor = ElegantOnPrimary,
                  checkedTrackColor = ElegantPrimary,
                  uncheckedThumbColor = ElegantTextMuted,
                  uncheckedTrackColor = ElegantBackground,
                  uncheckedBorderColor = ElegantBorder
                )
              )
            }
          }
        }

        // Bottom Status Indicator Card (from Elegant Dark design)
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = ElegantPrimaryContainer.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, ElegantPrimaryContainer.copy(alpha = 0.35f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ElegantPrimary)
                )
                Text(
                  text = "Service ready",
                  style = MaterialTheme.typography.bodySmall,
                  color = ElegantTextPrimary
                )
              }
              Text(
                text = "QUEST ADAPTER ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = ElegantPrimary
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Executes the Meta Quest command:
 * am start -n com.oculus.panelapp.settings/.SettingsActivity --es "uri" "/home"
 *
 * It first attempts via Android Intent (the native and standard method within an app).
 * If that fails, it falls back to shell am start execution.
 */
fun executeOculusCommand(context: Context, uri: String = "/home"): Result<String> {
  return try {
    val intent = Intent().apply {
      component = ComponentName(
        "com.oculus.panelapp.settings",
        "com.oculus.panelapp.settings.SettingsActivity"
      )
      putExtra("uri", uri)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
    Result.success("Intent sent to Oculus SettingsActivity with uri=$uri")
  } catch (e: Exception) {
    // Fallback: Attempt shell execution
    try {
      val process = Runtime.getRuntime().exec(
        arrayOf(
          "am",
          "start",
          "-n",
          "com.oculus.panelapp.settings/.SettingsActivity",
          "--es",
          "uri",
          uri
        )
      )
      val exitCode = process.waitFor()
      if (exitCode == 0) {
        Result.success("Shell command completed with exitCode 0")
      } else {
        val errorText = process.errorStream.bufferedReader().readText()
        Result.failure(Exception("Activity launch error: ${e.message ?: "ActivityNotFound"}. Shell exit $exitCode: $errorText"))
      }
    } catch (shellEx: Exception) {
      Result.failure(Exception(e.message ?: "Failed to launch com.oculus.panelapp.settings"))
    }
  }
}
