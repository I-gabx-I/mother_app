package gt.marcos.joyeria.domain.barcode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ScanResolverTest {

    private val table = mapOf(
        "XP-000042" to UidLookup(productId = 7, archived = false, stockQty = 3),
        "XP-000043" to UidLookup(productId = 8, archived = true, stockQty = 2),
        "XP-000044" to UidLookup(productId = 9, archived = false, stockQty = 0),
    )
    private val find: suspend (ProductUid) -> UidLookup? = { table[it.value] }

    @Test
    fun resolve_knownUid_returnsFoundWithItsIdAndStock() = runTest {
        assertThat(ScanResolver.resolve("XP-000042", find)).isEqualTo(ScanResult.Found(productId = 7, stockQty = 3))
    }

    @Test
    fun resolve_knownUidWithoutStock_stillReturnsFoundSoTheCallerDecides() = runTest {
        assertThat(ScanResolver.resolve("XP-000044", find)).isEqualTo(ScanResult.Found(productId = 9, stockQty = 0))
    }

    @Test
    fun resolve_unknownUid_returnsNotFoundWithThatUid() = runTest {
        val result = ScanResolver.resolve("XP-999999", find)
        assertThat(result).isEqualTo(ScanResult.NotFound(ProductUid.parse("XP-999999")!!))
    }

    @Test
    fun resolve_foreignBarcode_returnsNotAPieceWithoutLookingItUp() = runTest {
        var lookups = 0
        val result = ScanResolver.resolve("7501234567890") { lookups++; null }
        assertThat(result).isEqualTo(ScanResult.NotAPiece)
        assertThat(lookups).isEqualTo(0)
    }

    @Test
    fun resolve_archivedPiece_returnsArchived() = runTest {
        assertThat(ScanResolver.resolve("XP-000043", find)).isEqualTo(ScanResult.Archived)
    }

    @Test
    fun resolve_nullOrEmptyText_returnsNotAPiece() = runTest {
        assertThat(ScanResolver.resolve(null, find)).isEqualTo(ScanResult.NotAPiece)
        assertThat(ScanResolver.resolve("", find)).isEqualTo(ScanResult.NotAPiece)
    }
}
