package com.privatelan.screenshare

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

class LanHttpServer(
    private val port: Int,
    private val deviceId: String,
    private val deviceName: String
) : NanoHTTPD(port) {

    @Volatile
    var sharing: Boolean = false

    override fun serve(session: IHTTPSession): Response {

        return when (session.uri) {

            "/hello" -> {
                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    LanProtocol.hello(
                        deviceId,
                        deviceName,
                        "android",
                        sharing
                    ).toString()
                )
            }

            "/health" -> {
                newFixedLengthResponse(
                    Response.Status.OK,
                    "text/plain",
                    "ok"
                )
            }

            else -> {
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "application/json",
                    JSONObject()
                        .put("error", "not_found")
                        .toString()
                )
            }
        }
    }
}
