package gt.marcos.joyeria.domain.backup

/**
 * El contador de códigos de pieza (`next_product_uid_seq`) después de importar: **el mayor entre el del archivo y
 * 1 + el mayor código numérico que haya**. Si no, la próxima pieza repetiría un código que ya está pegado en una
 * etiqueta de una pieza real.
 */
object UidSequence {
    private val NUMERIC = Regex("XP-([0-9]+)")

    fun nextAfterImport(fileSeq: Long?, uids: Collection<String>): Long {
        val highest = uids.mapNotNull { NUMERIC.matchEntire(it)?.groupValues?.get(1)?.toLongOrNull() }.maxOrNull() ?: 0L
        return maxOf(fileSeq ?: 1L, highest + 1)
    }
}
