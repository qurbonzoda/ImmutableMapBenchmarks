package kotlinx.collections.immutable.implementations.immutableMap

internal class PersistentHashMapIterator<out K, out V>(node: TrieNode<K, V>) {
    private val path: Array<TrieNodeIterator<K, V>> = Array(8) {   TrieNodeIterator<K, V>() }
    private var pathLastIndex = 0
    private var hasNext = true

    init {
        path[0].reset(node)
        pathLastIndex = 0
        ensureNextEntryIsReady()
    }

    private fun moveToNextNodeWithData(pathIndex: Int): Int {
        if (path[pathIndex].hashNextEntry()) {
            return pathIndex
        }
        while (path[pathIndex].hashNextNode()) { // requires canonicalization (if)
            path[pathIndex + 1].reset(path[pathIndex].currentNode())
            val result = moveToNextNodeWithData(pathIndex + 1)
            if (result != -1) {
                return result
            }
            path[pathIndex].moveToNextNode()
        }
        return -1
    }

    private fun ensureNextEntryIsReady() {
        if (path[pathLastIndex].hashNextEntry()) {
            return
        }
        for(i in pathLastIndex downTo 0) {
            var result = moveToNextNodeWithData(i)
            while (result == -1 && path[i].hashNextNode()) { // requires canonicalization (if)
                path[i].moveToNextNode()
                result = moveToNextNodeWithData(i)
            }
            if (result != -1) {
                pathLastIndex = result
                return
            }
            if (i > 0) {
                path[i - 1].moveToNextNode()
            }
        }
        hasNext = false
    }

    fun hasNext(): Boolean {
        return hasNext
    }

    fun nextKey(): K {
        assert(hasNext())
        val result = path[pathLastIndex].nextKey()
        ensureNextEntryIsReady()
        return result
    }

    fun nextValue(): V {
        assert(hasNext())
        val result = path[pathLastIndex].nextValue()
        ensureNextEntryIsReady()
        return result
    }

    fun nextEntry(): Map.Entry<K, V> {
        assert(hasNext())
        val result = path[pathLastIndex].nextEntry()
        ensureNextEntryIsReady()
        return result
    }
}

internal class TrieNodeIterator<out K, out V> {
    private var buffer = emptyArray<Any?>()
    private var dataSize = 0
    private var index = 0

    fun reset(node: TrieNode<*, *>) {
        buffer = node.buffer
        index = 0
        dataSize = 2 * Integer.bitCount(node.dataMap)
    }

    fun hashNextEntry(): Boolean {
        return index < dataSize
    }

    fun nextKey(): K {
        assert(hashNextEntry())
        index += 2
        return buffer[index - 2] as K
    }

    fun nextValue(): V {
        assert(hashNextEntry())
        index += 2
        return buffer[index - 1] as V
    }

    fun nextEntry(): Map.Entry<K, V> {
        assert(hashNextEntry())
        index += 2
        return MapEntry(buffer[index - 2] as K, buffer[index - 1] as V)
    }

    fun currentNode(): TrieNode<out K, out V> {
        assert(hashNextNode())
        return buffer[index] as TrieNode<K, V>
    }

    fun hashNextNode(): Boolean {
        return index < buffer.size
    }

    fun moveToNextNode() {
        assert(hashNextNode())
        index++
    }
}

private class MapEntry<out K, out V>(override val key: K, override val value: V) : Map.Entry<K, V>

