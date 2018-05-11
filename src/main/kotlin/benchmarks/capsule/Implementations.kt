package benchmarks.capsule

import io.usethesource.capsule.Map

const val CAPSULE_TRIE_MAP        = "CAPSULE_TRIE_MAP"

fun <K: Comparable<K>, V> emptyPMap(implementation: String): Map.Immutable<K, V> {
    return when(implementation) {
        CAPSULE_TRIE_MAP          -> io.usethesource.capsule.core.PersistentTrieMap.of()
        else -> throw AssertionError("Unknown implementation: $implementation")
    }
}