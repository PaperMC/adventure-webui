package net.kyori.adventure.webui.websocket

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** The client -> server call. */
public sealed interface Packet

@Serializable
@SerialName("call")
public data class Call(
    public val miniMessage: String? = null,
    public val isolateNewlines: Boolean = false
) : Packet

@Serializable
@SerialName("placeholders")
@JsonSchema.Description("Placeholders to use when rendering the message.")
public data class Placeholders(
    @JsonSchema.Description("String placeholders to replace in the message.")
    @JsonSchema.Example("{\"test\": \"<red>TEST\"}")
    public val stringPlaceholders: Map<String, String>? = null,
    @JsonSchema.Example("{}")
    public val componentPlaceholders: Map<String, JsonObject>? = null
) : Packet

@Serializable
@JsonSchema.Title("Combined")
@JsonSchema.Description("Multi purpose request object")
public data class Combined(
    @JsonSchema.Description("MiniMessage String")
    @JsonSchema.Example("\"Hello, <red>world!</red>\"")
    public val miniMessage: String? = null,
    @JsonSchema.Description("Placeholders")
    public val placeholders: Placeholders? = null,
    @JsonSchema.Description("The background to render the message on.")
    @JsonSchema.Example("stone")
    public val background: String? = null,
    @JsonSchema.Description("The mode to render the message in.")
    @JsonSchema.Example("chat_open")
    public val mode: String? = null
)

@Serializable
@JsonSchema.Title("InGamePreview")
@JsonSchema.Description("Request to preview a message in-game.")
public data class InGamePreview(
    @JsonSchema.Description("The message to render the message in-game.")
    @JsonSchema.Example("\"Hello, <red>world!</red>\"")
    public val miniMessage: String? = null,
    @JsonSchema.Description("A random key to use as the hostname")
    @JsonSchema.Example("abc123")
    public val key: String? = null
)
