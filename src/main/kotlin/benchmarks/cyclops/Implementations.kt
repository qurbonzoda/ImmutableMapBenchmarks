package benchmarks.cyclops

import com.aol.cyclops.clojure.collections.ClojureHashPMap
import com.aol.cyclops.javaslang.collections.JavaSlangHashPMap
import com.aol.cyclops.javaslang.collections.JavaSlangTreePMap
import com.aol.cyclops.scala.collections.ScalaHashPMap
import com.aol.cyclops.scala.collections.ScalaTreePMap
import org.pcollections.PMap

const val CLOJURE_HASH_MAP      = "CLOJURE_HASH_MAP"
const val SCALA_HASH_MAP        = "SCALA_HASH_MAP"
const val SCALA_TREE_MAP        = "SCALA_TREE_MAP"
const val JAVASLANG_HASH_MAP    = "JAVASLANG_HASH_MAP"
const val JAVASLANG_TREE_MAP    = "JAVASLANG_TREE_MAP"
const val PCOLLECTIONS          = "PCOLLECTIONS"

fun <K: Comparable<K>, V> emptyPMap(implementation: String): PMap<K, V> {
    return when(implementation) {
        CLOJURE_HASH_MAP        -> ClojureHashPMap.empty()
        SCALA_HASH_MAP          -> ScalaHashPMap.empty()
        SCALA_TREE_MAP          -> ScalaTreePMap.empty()
        JAVASLANG_HASH_MAP      -> JavaSlangHashPMap.empty()
        JAVASLANG_TREE_MAP      -> JavaSlangTreePMap.empty()
        PCOLLECTIONS            -> org.pcollections.HashTreePMap.empty()
        else -> throw AssertionError("Unknown implementation: $implementation")
    }
}