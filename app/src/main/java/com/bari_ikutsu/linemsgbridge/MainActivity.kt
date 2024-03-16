package com.bari_ikutsu.linemsgbridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.bari_ikutsu.linemsgbridge.data.PrefStore
import com.bari_ikutsu.linemsgbridge.ui.theme.LINEMsgBridgeTheme
import com.bari_ikutsu.linemsgbridge.utils.AutoConnectionDetector
import com.bari_ikutsu.linemsgbridge.utils.Consts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val carConnectionState = mutableStateOf(false)
    private lateinit var autoConnectionListener: AutoConnectionDetector.OnCarConnectionStateListener
    private lateinit var autoDetector: AutoConnectionDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LINEMsgBridgeTheme {
                Page(
                    carConnectionState = carConnectionState,
                    tryToGetPermission = { tryToGetPermission() },
                    versionName = packageManager.getPackageInfo(packageName, 0).versionName
                )
            }
        }

        // permissions to request
        // add POST_NOTIFICATIONS permission if needed for Android 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0
            )
        }
        // Prepare notification channels
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannel(
                Consts.NOTIFICATION_CHANNEL_ID,
                Consts.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        // subscribe to Android Auto connection state
        autoConnectionListener = object : AutoConnectionDetector.OnCarConnectionStateListener {
            override fun onCarConnected() {
                carConnectionState.value = true
            }

            override fun onCarDisconnected() {
                carConnectionState.value = false
            }
        }
        autoDetector = AutoConnectionDetector(applicationContext)
        autoDetector.setListener(autoConnectionListener)
        autoDetector.registerCarConnectionReceiver()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    override fun onDestroy() {
        // unsubscribe from Android Auto connection state
        autoDetector.unRegisterCarConnectionReceiver()
        super.onDestroy()
    }

    private fun checkPermissions() {
        val enabled =
            NotificationManagerCompat.getEnabledListenerPackages(this@MainActivity).contains(
                packageName
            )

        val prefStore = PrefStore(this)
        CoroutineScope(Dispatchers.IO).launch {
            prefStore.saveNotificationAccess(enabled)
        }
    }

    private fun tryToGetPermission() {
        val enabled =
            NotificationManagerCompat.getEnabledListenerPackages(this@MainActivity).contains(
                packageName
            )

        if (!enabled) {
            ActivityCompat.startActivityForResult(
                this,
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                0,
                null
            )
        }
    }
}

@Composable
fun Page(carConnectionState: State<Boolean>, tryToGetPermission: () -> Unit, versionName: String) {
    val prefStore = PrefStore(LocalContext.current)
    val isNotificationTimeoutEnabled =
        prefStore.getNotificationTimeoutEnabled.collectAsState(initial = false)

    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TitleBar(title = stringResource(id = R.string.app_name))
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                ) {
                    FunctionHeader(title = stringResource(id = R.string.system_settings))
                    NotificationReadPermitted(tryToGetPermission)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                ) {
                    FunctionHeader(title = stringResource(id = R.string.car_connection_state))
                    CarConnectionState(carConnectionState = carConnectionState)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                ) {
                    FunctionHeader(title = stringResource(id = R.string.notification_timeout))
                    SwitchSetting(
                        checked = isNotificationTimeoutEnabled,
                        text = stringResource(id = R.string.notification_timeout_enabled),
                        onCheckedChange = {
                            CoroutineScope(Dispatchers.IO).launch {
                                prefStore.saveNotificationTimeoutEnabled(it)
                            }
                        }
                    )
                    NotificationTimeOut(
                        enabled = isNotificationTimeoutEnabled
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                ) {
                    FunctionHeader(title = stringResource(id = R.string.about_this_app))
                    VersionAndCopyright(versionName = versionName)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleBar(title: String) {
    Surface(color = Color.Red) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = contentColorFor(MaterialTheme.colorScheme.primary)
            ),
            title = {
                Text(
                    text = title
                )
            }
        )
    }
}

@Composable
fun FunctionHeader(title: String) {
    Text(
        title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
    )
}


@Composable
fun SwitchSetting(checked: State<Boolean>, text: String, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
    ) {
        Switch(
            checked = checked.value,
            onCheckedChange = onCheckedChange
        )
        Text(
            text,
            modifier = Modifier.padding(start = 8.dp, end = 12.dp)
        )
    }
}

@Composable
fun NotificationReadPermitted(tryToGetPermission: () -> Unit) {
    val prefStore = PrefStore(LocalContext.current)
    val isPermitted = prefStore.getNotificationAccess.collectAsState(initial = false)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
    ) {
        Switch(
            checked = isPermitted.value,
            onCheckedChange = {
                tryToGetPermission()
            }
        )
        Text(
            stringResource(id = R.string.notification_read_permitted),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun CarConnectionState(carConnectionState: State<Boolean>) {
    if (carConnectionState.value) {
        Text(
            stringResource(id = R.string.car_connected),
            modifier = Modifier.padding(start = 12.dp)
        )
    } else {
        Text(
            stringResource(id = R.string.car_disconnected),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun NotificationTimeOut(enabled: State<Boolean>) {
    val prefStore = PrefStore(LocalContext.current)
    val sliderValue = prefStore.getNotificationTimeout.collectAsState(initial = 3.0f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp)
    ) {
        Slider(
            enabled = enabled.value,
            value = sliderValue.value,
            onValueChange = {
                CoroutineScope(Dispatchers.IO).launch {
                    prefStore.saveNotificationTimeout(it)
                }
            },
            valueRange = 1f..10f
        )
        Text(
            "%.1f ".format(sliderValue.value) + stringResource(id = R.string.seconds)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun VersionAndCopyright(versionName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, bottom = 8.dp)
    ) {
        Text("Version $versionName")
        Text(stringResource(id = R.string.copyright))
    }
}

@Preview(showBackground = true)
@Composable
fun PagePreview() {
    LINEMsgBridgeTheme {
        Page(
            carConnectionState = remember{
                mutableStateOf(true)
            },
            tryToGetPermission = {},
            versionName = "0.1"
        )
    }
}
