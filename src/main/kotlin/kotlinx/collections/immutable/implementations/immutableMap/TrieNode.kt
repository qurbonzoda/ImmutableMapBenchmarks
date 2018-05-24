package kotlinx.collections.immutable.implementations.immutableMap


internal const val MAX_BRANCHING_FACTOR = 32
internal const val LOG_MAX_BRANCHING_FACTOR = 5
internal const val MAX_BRANCHING_FACTOR_MINUS_ONE = MAX_BRANCHING_FACTOR - 1
internal const val ENTRY_SIZE = 2
internal const val MAX_SHIFT = 30
internal const val NULL_HASH_CODE = 0


internal class TrieNode<K, V>(var dataMap: Int,
                              var nodeMap: Int,
                              var buffer: Array<Any?>) {
    fun makeMutableFor(mutator: PersistentHashMapBuilder<*, *>): TrieNode<K, V> {
        if (buffer[buffer.size - 1] === mutator.marker) { return this }

        val newBuffer = buffer.copyOf()
        newBuffer[newBuffer.size - 1] = mutator.marker
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    private fun ensureMutableBy(mutator: PersistentHashMapBuilder<*, *>) {
        if (buffer[buffer.size - 1] !== mutator.marker) {
            throw IllegalStateException("Marker expected")
        }
    }

    private fun hasDataAt(position: Int): Boolean {
        return dataMap and position != 0
    }

    private fun hasNodeAt(position: Int): Boolean {
        return nodeMap and position != 0
    }

    private fun keyDataIndex(position: Int): Int {
        return ENTRY_SIZE * Integer.bitCount(dataMap and (position - 1))
    }

    private fun keyNodeIndex(position: Int): Int {
        return buffer.size - 2 - Integer.bitCount(nodeMap and (position - 1))   // last element is marker
    }

    private fun <K> keyAt(position: Int): K {
        val keyIndex = keyDataIndex(position)
        return buffer[keyIndex] as K
    }

    private fun <V> valueAt(position: Int): V {
        val valueIndex = keyDataIndex(position) + 1
        return buffer[valueIndex] as V
    }

    private fun nodeAt(position: Int): TrieNode<K, V> {
        val nodeIndex = keyNodeIndex(position)
        return buffer[nodeIndex] as TrieNode<K, V>
    }

    private fun putDataAt(position: Int, key: K, value: V): TrieNode<K, V> {
//        assert(!hasDataAt(position))

        val keyIndex = keyDataIndex(position)
        val newBuffer = arrayOfNulls<Any?>(buffer.size + 2)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex, newBuffer, keyIndex + 2, buffer.size - 1 - keyIndex) // marker
        newBuffer[keyIndex] = key
        newBuffer[keyIndex + 1] = value
        return TrieNode(dataMap or position, nodeMap, newBuffer)
    }

    private fun mutablePutDataAt(position: Int, key: K, value: V) {
//        assert(!hasDataAt(position))

        val keyIndex = keyDataIndex(position)
        val newBuffer = arrayOfNulls<Any?>(buffer.size + 2)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex, newBuffer, keyIndex + 2, buffer.size - keyIndex)
        newBuffer[keyIndex] = key
        newBuffer[keyIndex + 1] = value
        buffer = newBuffer
        dataMap = dataMap or position
    }

    private fun updateValueAt(position: Int, value: V): TrieNode<K, V> {
//        assert(hasDataAt(position))

        val keyIndex = keyDataIndex(position)
//        assert(buffer[keyIndex + 1] !== value)
        val newBuffer = buffer.copyOf()
        newBuffer[keyIndex + 1] = value
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    private fun mutableUpdateValueAt(position: Int, value: V): V {
//        assert(hasDataAt(position))

        val keyIndex = keyDataIndex(position)
        val previousValue = buffer[keyIndex + 1]
        buffer[keyIndex + 1] = value
        return previousValue as V
    }

    private fun updateNodeAt(position: Int, newNode: TrieNode<K, V>): TrieNode<K, V> {
//        assert(hasNodeAt(position))

        val nodeIndex = keyNodeIndex(position)
//        assert(buffer[nodeIndex] !== newNode)
        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    private fun mutableUpdateNodeAt(position: Int, newNode: TrieNode<K, V>) {
//        assert(hasNodeAt(position))

        val nodeIndex = keyNodeIndex(position)
        buffer[nodeIndex] = newNode
    }

    private fun moveDataToNode(position: Int, storedKeyHash: Int, newKeyHash: Int,
                               newKey: K, newValue: V, shift: Int): TrieNode<K, V> {
//        assert(hasDataAt(position))
//        assert(!hasNodeAt(position))

        val keyIndex = keyDataIndex(position)
        val nodeIndex = keyNodeIndex(position) - 1
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, nodeIndex - keyIndex)
        System.arraycopy(buffer, nodeIndex + 2, newBuffer, nodeIndex + 1, buffer.size - nodeIndex - 3) // marker

        newBuffer[nodeIndex] = makeNode(storedKeyHash, buffer[keyIndex] as K, buffer[keyIndex + 1] as V,
                newKeyHash, newKey, newValue, shift + LOG_MAX_BRANCHING_FACTOR, null)
        return TrieNode(dataMap xor position, nodeMap or position, newBuffer)
    }

    private fun mutableMoveDataToNode(position: Int, storedKeyHash: Int, newKeyHash: Int, newKey: K, newValue: V,
                                      shift: Int, mutator: PersistentHashMapBuilder<*, *>) {
//        assert(hasDataAt(position))
//        assert(!hasNodeAt(position))

        val keyIndex = keyDataIndex(position)
        val nodeIndex = keyNodeIndex(position) - 1
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, nodeIndex - keyIndex)
        System.arraycopy(buffer, nodeIndex + 2, newBuffer, nodeIndex + 1, buffer.size - nodeIndex - 2)

        newBuffer[nodeIndex] = makeNode(storedKeyHash, buffer[keyIndex] as K, buffer[keyIndex + 1] as V,
                newKeyHash, newKey, newValue, shift + LOG_MAX_BRANCHING_FACTOR, mutator.marker)

        buffer = newBuffer
        dataMap = dataMap xor position
        nodeMap = nodeMap or position
    }

    private fun makeNode(keyHash1: Int, key1: K, value1: V, keyHash2: Int, key2: K, value2: V, shift: Int, mutatorMarker: Marker?): TrieNode<K, V> {
        if (shift > MAX_SHIFT) {
//            assert(key1 != key2)
            return TrieNode(0, 0, arrayOf(key1, value1, key2, value2, mutatorMarker))
        }

        val setBit1 = (keyHash1 shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE
        val setBit2 = (keyHash2 shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE

        if (setBit1 != setBit2) {
            val newDataMap = (1 shl setBit1) or (1 shl setBit2)
            if (setBit1 < setBit2) {
                return TrieNode(newDataMap, 0, arrayOf(key1, value1, key2, value2, mutatorMarker))
            }
            return TrieNode(newDataMap, 0, arrayOf(key2, value2, key1, value1, mutatorMarker))
        }
        val node = makeNode(keyHash1, key1, value1, keyHash2, key2, value2, shift + LOG_MAX_BRANCHING_FACTOR, mutatorMarker)
        return TrieNode(0, 1 shl setBit1, arrayOf(node, mutatorMarker))
    }

    private fun bufferRemoveDataAtIndex(keyIndex: Int): Array<Any?> {
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 2)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, buffer.size - keyIndex - 3) // marker
        return newBuffer
    }

    private fun mutableBufferRemoveDataAtIndex(keyIndex: Int): Array<Any?> {
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 2)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, buffer.size - keyIndex - 2)
        return newBuffer
    }

    private fun removeDataAt(position: Int): TrieNode<K, V>? {
//        assert(hasDataAt(position))
        if (buffer.size == 3 && nodeMap == 0) { return null }

        val keyIndex = keyDataIndex(position)
        val newBuffer = bufferRemoveDataAtIndex(keyIndex)
        return TrieNode(dataMap xor position, nodeMap, newBuffer)
    }

    private fun mutableRemoveDataAt(position: Int): V {
//        assert(hasDataAt(position))
        val keyIndex = keyDataIndex(position)
        val previousValue = buffer[keyIndex + 1]
        buffer = mutableBufferRemoveDataAtIndex(keyIndex)
        dataMap = dataMap xor position
        return previousValue as V
    }

    private fun collisionRemoveDataAt(i: Int): TrieNode<K, V>? {
        if (buffer.size == 3 && nodeMap == 0) { return null }

        val newBuffer = bufferRemoveDataAtIndex(i)
        return TrieNode(0, 0, newBuffer)
    }

    private fun mutableCollisionRemoveDataAt(i: Int): V? {
        val previousValue = buffer[i + 1]
        buffer = mutableBufferRemoveDataAtIndex(i)
        return previousValue as V
    }

    private fun removeNodeAt(position: Int): TrieNode<K, V>? {
//        assert(hasNodeAt(position))
        if (buffer.size == 2 && dataMap == 0) { return null } // marker

        val keyIndex = keyNodeIndex(position)
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex + 1, newBuffer, keyIndex, buffer.size - keyIndex - 2) // marker
        return TrieNode(dataMap, nodeMap xor position, newBuffer)
    }

    private fun mutableRemoveNodeAt(position: Int) {
//        assert(hasNodeAt(position))
        val keyIndex = keyNodeIndex(position)
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex + 1, newBuffer, keyIndex, buffer.size - keyIndex - 1)
        buffer = newBuffer
        nodeMap = nodeMap xor position
    }

    private fun collisionContainsKey(key: K): Boolean {
        for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
            if (key == buffer[i]) { return true }
        }
        return false
    }

    private fun collisionGet(key: K): V? {
        for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
            if (key == buffer[i]) { return buffer[i + 1] as V }
        }
        return null
    }

    private fun collisionPut(key: K, value: V, modification: ModificationWrapper): TrieNode<K, V> {
        for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
            if (key == buffer[i]) {
                if (value === buffer[i + 1]) {
                    return this
                }
                modification.value = UPDATE_VALUE
                val newBuffer = buffer.copyOf()
                newBuffer[i + 1] = value
                newBuffer[newBuffer.size - 1] = null // marker
                return TrieNode(0, 0, newBuffer)
            }
        }
        modification.value = PUT_KEY_VALUE
        val newBuffer = arrayOfNulls<Any?>(buffer.size + 2)
        System.arraycopy(buffer, 0, newBuffer, 2, buffer.size - 1) // marker
        newBuffer[0] = key
        newBuffer[1] = value
        return TrieNode(0, 0, newBuffer)
    }

    private fun mutableCollisionPut(key: K, value: V, mutator: PersistentHashMapBuilder<*, *>): V? {
        for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
            if (key == buffer[i]) {
                val previousValue = buffer[i + 1]
                buffer[i + 1] = value
                return previousValue as V
            }
        }
        mutator.size++
        val newBuffer = arrayOfNulls<Any?>(buffer.size + 2)
        System.arraycopy(buffer, 0, newBuffer, 2, buffer.size)
        newBuffer[0] = key
        newBuffer[1] = value
        buffer = newBuffer
        return null
    }

    private fun collisionRemove(key: K): TrieNode<K, V>? {
        for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
            if (key == buffer[i]) {
                return collisionRemoveDataAt(i)
            }
        }
        return this
    }

    private fun mutableCollisionRemove(key: K, mutator: PersistentHashMapBuilder<*, *>): V? {
        for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
            if (key == buffer[i]) {
                mutator.size--
                return mutableCollisionRemoveDataAt(i)
            }
        }
        return null
    }

    private fun collisionRemove(key: K, value: V): TrieNode<K, V>? {
        for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
            if (key == buffer[i] && value == buffer[i + 1]) {
                return collisionRemoveDataAt(i)
            }
        }
        return this
    }

    fun containsKey(keyHash: Int, key: K, shift: Int): Boolean {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            return key == keyAt<K>(keyPosition)
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAt(keyPosition)
            if (shift == MAX_SHIFT) {
                return targetNode.collisionContainsKey(key)
            }
            return targetNode.containsKey(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
        }

        // key is absent
        return false
    }

    fun get(keyHash: Int, key: K, shift: Int): V? {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val storedKey = keyAt<K>(keyPosition)
            if (key == storedKey) {
                return valueAt(keyPosition)
            }
            return null
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAt(keyPosition)
            if (shift == MAX_SHIFT) {
                return targetNode.collisionGet(key)
            }
            return targetNode.get(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
        }

        // key is absent
        return null
    }

    fun put(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int, modification: ModificationWrapper): TrieNode<K, V> {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val storedKey = keyAt<K>(keyPosition)
            if (key == storedKey) {
                if (valueAt<V>(keyPosition) === value) { return this }
                modification.value = UPDATE_VALUE
                return updateValueAt(keyPosition, value)
            }
            modification.value = PUT_KEY_VALUE
            val storedKeyHash = storedKey?.hashCode() ?: NULL_HASH_CODE
            return moveDataToNode(keyPosition, storedKeyHash, keyHash, key, value, shift)
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAt(keyPosition)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionPut(key, value, modification)
            } else {
                targetNode.put(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, modification)
            }
            if (targetNode === newNode) { return this }
            return updateNodeAt(keyPosition, newNode)
        }

        // key is absent
        modification.value = PUT_KEY_VALUE
        return putDataAt(keyPosition, key, value)
    }

    fun mutablePut(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int, mutator: PersistentHashMapBuilder<*, *>): V? {
        ensureMutableBy(mutator)
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val storedKey = keyAt<K>(keyPosition)
            if (key == storedKey) {
                return mutableUpdateValueAt(keyPosition, value)
            }
            mutator.size++
            val storedKeyHash = storedKey?.hashCode() ?: NULL_HASH_CODE
            mutableMoveDataToNode(keyPosition, storedKeyHash, keyHash, key, value, shift, mutator)
            return null
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAt(keyPosition).makeMutableFor(mutator)
            mutableUpdateNodeAt(keyPosition, targetNode)
            return if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionPut(key, value, mutator)
            } else {
                targetNode.mutablePut(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
        }

        // key is absent
        mutator.size++
        mutablePutDataAt(keyPosition, key, value)
        return null
    }

    fun remove(keyHash: Int, key: K, shift: Int): TrieNode<K, V>? {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val storedKey = keyAt<K>(keyPosition)
            if (key == storedKey) {
                return removeDataAt(keyPosition)
            }
            return this
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAt(keyPosition)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(key)
            } else {
                targetNode.remove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            if (targetNode === newNode) { return this }
            if (newNode == null) { return removeNodeAt(keyPosition) }
            return updateNodeAt(keyPosition, newNode)
        }

        // key is absent
        return this
    }

    fun mutableRemove(keyHash: Int, key: K, shift: Int, mutator: PersistentHashMapBuilder<*, *>): V? {
        ensureMutableBy(mutator)
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val storedKey = keyAt<K>(keyPosition)
            if (key == storedKey) {
                mutator.size--
                return mutableRemoveDataAt(keyPosition)
            }
            return null
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAt(keyPosition).makeMutableFor(mutator)
            mutableUpdateNodeAt(keyPosition, targetNode)
            val previousValue = if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionRemove(key, mutator)
            } else {
                targetNode.mutableRemove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
            if (targetNode.buffer.size == 1) { mutableRemoveNodeAt(keyPosition) }
            return previousValue
        }

        // key is absent
        return null
    }

    fun remove(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int): TrieNode<K, V>? {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val storedKey = keyAt<K>(keyPosition)
            val oldValue = valueAt<V>(keyPosition)
            if (key == storedKey && value == oldValue) {
                return removeDataAt(keyPosition)
            }
            return this
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAt(keyPosition)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(key, value)
            } else {
                targetNode.remove(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            if (targetNode === newNode) { return this }
            if (newNode == null) { return removeNodeAt(keyPosition) }
            return updateNodeAt(keyPosition, newNode)
        }

        // key is absent
        return this
    }

    internal companion object {
        internal val EMPTY = TrieNode<Nothing, Nothing>(0, 0, arrayOf(null))
    }
}