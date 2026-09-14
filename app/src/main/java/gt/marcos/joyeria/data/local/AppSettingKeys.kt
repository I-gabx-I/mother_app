package gt.marcos.joyeria.data.local

// Claves de app_setting (ESQUEMA.md). Los valores se parsean siempre a Long
// o Int, nunca a un tipo de punto flotante (D-010) — value es String solo
// porque la tabla es clave/valor genérica. default_markup_bp reemplaza a
// default_markup_percent (D-014): mismo propósito, fórmula y valor por
// defecto distintos (ver fix/seed-markup-bp).
object AppSettingKeys {
    const val DEFAULT_MARKUP_BP = "default_markup_bp"
    const val PRICE_ROUNDING_STEP_CENTS = "price_rounding_step_cents"
    const val LOW_STOCK_THRESHOLD = "low_stock_threshold"
    const val STALE_STOCK_DAYS = "stale_stock_days"
    const val NEXT_PRODUCT_UID_SEQ = "next_product_uid_seq"
    const val OWNER_NAME = "owner_name"
}
