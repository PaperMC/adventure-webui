package net.kyori.adventure.webui.jvm

import io.github.smiley4.ktorredoc.redoc
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.CachingOptions
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.OperationDescribeAttributeKey
import io.ktor.server.routing.openapi.plus
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.websocket.WebSocketDeflateExtension
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

public fun Application.main() {
    install(Compression) {
        gzip()
        deflate()
    }

    install(CachingHeaders) {
        options { _, outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Image.JPEG, ContentType.parse("application/x-font-woff") ->
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 31536000))
                ContentType.Text.CSS, ContentType.Application.JavaScript ->
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 86400))
                else -> null
            }
        }
    }

    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 5.seconds

        extensions { install(WebSocketDeflateExtension) }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        // enable trace routing if in dev mode
        if (developmentMode) {
            trace { route -> this@main.log.debug(route.buildText()) }
        }

        // api docs
        val oaInfo = OpenApiInfo(
            "Adventure web API",
            "Adventure ${getConfigString("miniMessageVersion")}",
            description = "OpenAPI documentation for the Adventure web API.",
            contact = OpenApiInfo.Contact("PaperMC Discord", "https://discord.gg/papermc", ""),
            license = OpenApiInfo.License("The MIT License", "https://github.com/PaperMC/adventure-webui/blob/main/license.txt", "MIT"),
        )
        val oaSource = OpenApiDocSource.Routing {
            routingRoot.descendants().filterNot { it.attributes.getOrNull(OperationDescribeAttributeKey).isNullOrEmpty() }
        }
        get("/api/docs.json") {
            call.respondText(
                Json.encodeToString(OpenApiDoc(info = oaInfo) + oaSource.routes(this@main)),
                ContentType.Application.Json,
            )
        }
        route("/api/redoc") {
            redoc("/api/docs.json") {
                pageTitle = "Adventure web API docs"
            }
        }
        swaggerUI("/api/swagger") {
            info = oaInfo
            source = oaSource
            faviconLocation = "/favicon-32x32.png"
        }
    }
}


/** Reads a string value from the `config` block in `application.conf`. */
public fun Application.getConfigString(key: String): String =
    environment.config.property("ktor.config.$key").getString()
