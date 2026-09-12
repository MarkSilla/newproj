package com.privatelan.screenshare

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val deviceId =
        UUID.randomUUID().toString()

    private val server =
        LanHttpServer(
            0,
            deviceId,
            "Android device"
        )

    private lateinit var discovery: LanDiscovery

    private val nearby =
        mutableStateOf(
            emptyList<LanDiscovery.Device>()
        )

    private val captureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (
                result.resultCode == RESULT_OK &&
                result.data != null
            ) {

                val intent =
                    Intent(
                        this,
                        ScreenCaptureService::class.java
                    ).apply {

                        putExtra(
                            "resultCode",
                            result.resultCode
                        )

                        putExtra(
                            "data",
                            result.data
                        )
                    }

                androidx.core.content.ContextCompat
                    .startForegroundService(
                        this,
                        intent
                    )

                server.sharing = true
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        server.start()

        val actualPort =
            server.listeningPort

        discovery =
            LanDiscovery(
                this,
                "Android device"
            ) {
                nearby.value =
                    it.filter { device ->
                        device.id != deviceId
                    }
            }

        discovery.start(actualPort)

        setContent {

            val devices =
                remember {
                    nearby
                }

            DisposableEffect(Unit) {

                onDispose {

                    discovery.stop()
                    server.stop()
                }
            }

            MaterialTheme {

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {

                    Text(
                        "Private LAN Screen Share",
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )

                    Text(
                        "Same Wi-Fi • no codes • no QR • no manual IP"
                    )

                    Button(
                        onClick = {

                            val manager =
                                getSystemService(
                                    MEDIA_PROJECTION_SERVICE
                                ) as MediaProjectionManager

                            captureLauncher.launch(
                                manager.createScreenCaptureIntent()
                            )
                        }
                    ) {
                        Text("Share my full screen")
                    }

                    Text(
                        "Nearby Devices",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )

                    LazyColumn(
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        items(
                            devices.value
                        ) { device ->

                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Viewer connection
                                        // will be added later.
                                    }
                            ) {

                                Row(
                                    Modifier.padding(16.dp),
                                    horizontalArrangement =
                                        Arrangement.SpaceBetween
                                ) {

                                    Column {

                                        Text(
                                            device.name
                                        )

                                        Text(
                                            "${device.type} • ${device.host}:${device.port}"
                                        )
                                    }

                                    Text(
                                        if (device.sharing)
                                            "Sharing"
                                        else
                                            "Ready"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
