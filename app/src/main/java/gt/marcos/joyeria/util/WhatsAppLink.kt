package gt.marcos.joyeria.util

import java.net.URLEncoder

private const val GUATEMALA_COUNTRY_CODE = "502"
private const val LOCAL_DIGIT_COUNT = 8

/**
 * Normaliza y arma el link de "click to chat" de WhatsApp (Fase 10).
 * Sin `Context`, sin Android: toda la lógica de acá es texto puro,
 * testeable con JUnit4 normal, sin Robolectric (mismo criterio que
 * `domain/`, aunque este archivo vive en `util/` porque el resultado es
 * un link, no una regla de negocio -- ver D-012/CLAUDE.md sección 8).
 */
object WhatsAppLink {

    /**
     * Acepta los formatos reales que puede escribir o pegar una clienta
     * guatemalteca (con o sin `+`, con espacios, guiones o paréntesis,
     * con o sin el código de país, con el prefijo de salida `00`) y
     * devuelve el teléfono en el único formato que `wa.me` acepta: solo
     * dígitos, con código de país, sin `+`. Devuelve `null` si no hay
     * forma honesta de interpretar el número -- ver `ESTADO.md`, "Fase
     * 10 -- Plan", punto 2: un número que no normaliza **no** deja el
     * botón sin hacer nada, el llamador decide qué avisar.
     */
    fun normalizePhone(raw: String): String? {
        val digits = raw.filter(Char::isDigit)
        return when {
            digits.length == LOCAL_DIGIT_COUNT ->
                GUATEMALA_COUNTRY_CODE + digits
            digits.length == LOCAL_DIGIT_COUNT + GUATEMALA_COUNTRY_CODE.length &&
                digits.startsWith(GUATEMALA_COUNTRY_CODE) ->
                digits
            digits.length == LOCAL_DIGIT_COUNT + GUATEMALA_COUNTRY_CODE.length + 2 &&
                digits.startsWith("00$GUATEMALA_COUNTRY_CODE") ->
                digits.removePrefix("00")
            else -> null
        }
    }

    /**
     * `normalizedPhone` ya pasó por [normalizePhone]. El mensaje se
     * URL-encodea con `UTF-8` (tildes, "ñ" y saltos de línea incluidos,
     * criterio 3 de `FASES.md` Fase 10) y los espacios, que
     * `URLEncoder` codifica como `+`, se pasan a `%20` -- más explícito
     * en una URL y sin depender de que `wa.me` interprete `+` como
     * espacio en vez de como el signo literal.
     */
    fun buildReminderUrl(normalizedPhone: String, message: String): String {
        val encodedMessage = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
        return "https://wa.me/$normalizedPhone?text=$encodedMessage"
    }
}
