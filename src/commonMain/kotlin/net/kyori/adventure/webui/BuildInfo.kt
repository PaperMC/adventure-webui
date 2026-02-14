package net.kyori.adventure.webui

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@JsonSchema.Title("BuildInfo")
@JsonSchema.Description("Information about the build of the server")
@Serializable
public data class BuildInfo(
    @JsonSchema.Description("The time the server started")
    @JsonSchema.Example("\"2021-08-22T19:20:00Z\"")
    public val startedAt: String,
    @JsonSchema.Description("The version of Adventure used by the server")
    @JsonSchema.Example("4.10.0-SNAPSHOT")
    public val version: String,
    @JsonSchema.Description("The commit hash of code in use by the server")
    @JsonSchema.Example("9f43339123b0ad37cfe210b6562e39b9a3ccf7c7")
    public val commit: String,
    @JsonSchema.Description("The URL of the Bytebin instance used by the server")
    @JsonSchema.Example("\"https://bytebin.lucko.me\"")
    public val bytebinInstance: String
)
