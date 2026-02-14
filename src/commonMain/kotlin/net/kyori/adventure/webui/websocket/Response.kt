package net.kyori.adventure.webui.websocket

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

/** The server -> client response. */
@Serializable
@JsonSchema.Title("Response")
@JsonSchema.Description("A parse response.")
public data class Response(public val parseResult: ParseResult? = null)

/** The result of a parse. */
@Serializable
@JsonSchema.Title("ParseResult")
public data class ParseResult(
    /** If the parse was a success. */
    @JsonSchema.Description("If the parse was a success.")
    public val success: Boolean,
    /** The result of the conversion, only if it was a [success]. */
    @JsonSchema.Description("The result of the conversion, only if it was a success.")
    public val dom: String? = null,
    /** The error message, if it wasn't a [success]. */
    @JsonSchema.Description("The error message, if it wasn't a success.")
    public val errorMessage: String? = null
)
