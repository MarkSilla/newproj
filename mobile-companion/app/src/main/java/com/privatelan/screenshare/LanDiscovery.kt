package com.privatelan.screenshare

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.util.UUID

class LanDiscovery(
    context: Context,
    private val deviceName: String,
    private val onDevicesChanged: (List<Device>) -> Unit
) {

    data class Device(
        val id: String,
        val name: String,
        val host: String,
        val port: Int,
        val type: String,
        val sharing: Boolean
    )

    private val nsd =
        context.getSystemService(NsdManager::class.java)

    private val devices =
        linkedMapOf<String, Device>()

    private val serviceName =
        "PrivateLAN-${UUID.randomUUID().toString().take(8)}"

    private val registrationListener =
        object : NsdManager.RegistrationListener {

            override fun onServiceRegistered(
                info: NsdServiceInfo
            ) = Unit

            override fun onRegistrationFailed(
                info: NsdServiceInfo,
                errorCode: Int
            ) = Unit

            override fun onServiceUnregistered(
                info: NsdServiceInfo
            ) = Unit

            override fun onUnregistrationFailed(
                info: NsdServiceInfo,
                errorCode: Int
            ) = Unit
        }

    private val discoveryListener =
        object : NsdManager.DiscoveryListener {

            override fun onDiscoveryStarted(type: String) = Unit

            override fun onDiscoveryStopped(type: String) = Unit

            override fun onStartDiscoveryFailed(
                type: String,
                errorCode: Int
            ) = Unit

            override fun onStopDiscoveryFailed(
                type: String,
                errorCode: Int
            ) = Unit

            override fun onServiceFound(
                info: NsdServiceInfo
            ) {
                nsd.resolveService(info, resolver)
            }

            override fun onServiceLost(
                info: NsdServiceInfo
            ) {
                devices.remove(info.serviceName)
                publish()
            }
        }

    private val resolver =
        object : NsdManager.ResolveListener {

            override fun onResolveFailed(
                info: NsdServiceInfo,
                errorCode: Int
            ) = Unit

            override fun onServiceResolved(
                info: NsdServiceInfo
            ) {

                val host =
                    info.host?.hostAddress ?: return

                val id =
                    info.attributes["id"]
                        ?.toString(Charsets.UTF_8)
                        ?: info.serviceName

                val type =
                    info.attributes["type"]
                        ?.toString(Charsets.UTF_8)
                        ?: "unknown"

                val sharing =
                    info.attributes["sharing"]
                        ?.toString(Charsets.UTF_8) == "1"

                devices[id] = Device(
                    id = id,
                    name = info.serviceName,
                    host = host,
                    port = info.port,
                    type = type,
                    sharing = sharing
                )

                publish()
            }
        }

    fun start(serverPort: Int) {

        val info =
            NsdServiceInfo().apply {

                serviceName = this@LanDiscovery.serviceName
                serviceType = LanProtocol.serviceType
                port = serverPort

                setAttribute(
                    "id",
                    this@LanDiscovery.serviceName
                )

                setAttribute(
                    "type",
                    "android"
                )

                setAttribute(
                    "sharing",
                    "0"
                )
            }

        nsd.registerService(
            info,
            NsdManager.PROTOCOL_DNS_SD,
            registrationListener
        )

        nsd.discoverServices(
            LanProtocol.serviceType,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun stop() {

        runCatching {
            nsd.stopServiceDiscovery(
                discoveryListener
            )
        }

        runCatching {
            nsd.unregisterService(
                registrationListener
            )
        }
    }

    private fun publish() {
        onDevicesChanged(
            devices.values.toList()
        )
    }
}
