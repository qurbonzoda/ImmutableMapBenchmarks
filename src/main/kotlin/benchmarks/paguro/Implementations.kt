package benchmarks.paguro

import org.organicdesign.fp.collections.BaseMap

const val PAGURO_HASH_MAP        = "PAGURO_HASH_MAP"
const val PAGURO_TREE_MAP        = "PAGURO_TREE_MAP"

fun <K: Comparable<K>, V> emptyPMap(implementation: String): BaseMap<K, V> {
    return when(implementation) {
        PAGURO_HASH_MAP          -> org.organicdesign.fp.collections.PersistentHashMap.empty()
        PAGURO_TREE_MAP          -> org.organicdesign.fp.collections.PersistentTreeMap.empty()
        else -> throw AssertionError("Unknown implementation: $implementation")
    }
}