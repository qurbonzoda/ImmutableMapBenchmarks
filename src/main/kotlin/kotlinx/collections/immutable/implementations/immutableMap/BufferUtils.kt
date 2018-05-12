package kotlinx.collections.immutable.implementations.immutableMap


//internal fun node_makeMutableFor(buffer: Array<Any?>, mutator: PersistentHashMapBuilder<*, *>): Array<Any?> {
//    if (buffer[buffer.size - 1] === mutator.marker) { return this }
//
//    val newBuffer = buffer.copyOf()
//    newBuffer[newBuffer.size - 1] = mutator.marker
//    return newBuffer
//}
//
//private fun ensureMutableBy(buffer: Array<Any?>, mutator: PersistentHashMapBuilder<*, *>) {
//    if (buffer[buffer.size - 1] !== mutator.marker) {
//        throw IllegalStateException("Marker expected")
//    }
//}

private fun hasDataAt(buffer: Array<Any?>, position: Int): Boolean {
    return (buffer[0] as Int) and position != 0
}

private fun hasNodeAt(buffer: Array<Any?>, position: Int): Boolean {
    return (buffer[1] as Int) and position != 0
}

private fun keyDataIndex(buffer: Array<Any?>, position: Int): Int {
    return ENTRY_SIZE * Integer.bitCount((buffer[0] as Int) and (position - 1)) + 2 // dataMap and nodeMap go first
}

private fun keyNodeIndex(buffer: Array<Any?>, position: Int): Int {
    return buffer.size - 2 - Integer.bitCount((buffer[1] as Int) and (position - 1))   // last element is marker
}

private fun <K> keyAt(buffer: Array<Any?>, position: Int): K {
    val keyIndex = keyDataIndex(buffer, position)
    return buffer[keyIndex] as K
}

private fun <V> valueAt(buffer: Array<Any?>, position: Int): V {
    val valueIndex = keyDataIndex(buffer, position) + 1
    return buffer[valueIndex] as V
}

private fun nodeAt(buffer: Array<Any?>, position: Int): Array<Any?> {
    val nodeIndex = keyNodeIndex(buffer, position)
    return buffer[nodeIndex] as Array<Any?>
}

private fun <K, V> putDataAt(buffer: Array<Any?>, position: Int, key: K, value: V): Array<Any?> {
//        assert(!hasDataAt(position))

    val keyIndex = keyDataIndex(buffer, position)
    val newBuffer = arrayOfNulls<Any?>(buffer.size + 2)
    System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
    System.arraycopy(buffer, keyIndex, newBuffer, keyIndex + 2, buffer.size - 1 - keyIndex) // marker
    newBuffer[keyIndex] = key
    newBuffer[keyIndex + 1] = value
    newBuffer[0] = (newBuffer[0] as Int) or position
    return newBuffer
}

//private fun <K, V> mutablePutDataAt(buffer: Array<Any?>, position: Int, key: K, value: V): Array<Any?> {
////        assert(!hasDataAt(position))
//
//    val keyIndex = keyDataIndex(buffer, position)
//    val newBuffer = arrayOfNulls<Any?>(buffer.size + 2)
//    System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
//    System.arraycopy(buffer, keyIndex, newBuffer, keyIndex + 2, buffer.size - keyIndex)
//    newBuffer[keyIndex] = key
//    newBuffer[keyIndex + 1] = value
//    newBuffer[0] = (newBuffer[0] as Int) or position
//    return newBuffer
//}

private fun <V> updateValueAt(buffer: Array<Any?>, position: Int, value: V): Array<Any?> {
//        assert(hasDataAt(position))

    val keyIndex = keyDataIndex(buffer, position)
//        assert(buffer[keyIndex + 1] !== value)
    val newBuffer = buffer.copyOf()
    newBuffer[keyIndex + 1] = value
    return newBuffer
}

//private fun <V> mutableUpdateValueAt(buffer: Array<Any?>, position: Int, value: V): V {
////        assert(hasDataAt(position))
//
//    val keyIndex = keyDataIndex(buffer, position)
//    val previousValue = buffer[keyIndex + 1]
//    buffer[keyIndex + 1] = value
//    return previousValue as V
//}

private fun updateNodeAt(buffer: Array<Any?>, position: Int, newNode: Array<Any?>): Array<Any?> {
//        assert(hasNodeAt(position))

    val nodeIndex = keyNodeIndex(buffer, position)
//        assert(buffer[nodeIndex] !== newNode)
    val newBuffer = buffer.copyOf()
    newBuffer[nodeIndex] = newNode
    return newBuffer
}

//private fun mutableUpdateNodeAt(buffer: Array<Any?>, position: Int, newNode: Array<Any?>) {
////        assert(hasNodeAt(position))
//
//    val nodeIndex = keyNodeIndex(buffer, position)
//    buffer[nodeIndex] = newNode
//}

private fun <K, V> moveDataToNode(buffer: Array<Any?>, position: Int, oldKeyHash: Int, newKeyHash: Int,
                                  newKey: K, newValue: V, shift: Int): Array<Any?> {
//        assert(hasDataAt(position))
//        assert(!hasNodeAt(position))

    val keyIndex = keyDataIndex(buffer, position)
    val nodeIndex = keyNodeIndex(buffer, position) - 1
    val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
    System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
    System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, nodeIndex - keyIndex)
    System.arraycopy(buffer, nodeIndex + 2, newBuffer, nodeIndex + 1, buffer.size - nodeIndex - 3) // marker

    newBuffer[nodeIndex] = makeNode(oldKeyHash, buffer[keyIndex] as K, buffer[keyIndex + 1] as V,
            newKeyHash, newKey, newValue, shift + LOG_MAX_BRANCHING_FACTOR, null)
    newBuffer[0] = (newBuffer[0] as Int) xor position
    newBuffer[1] = (newBuffer[1] as Int) or position
    return newBuffer
}

//private fun <K, V> mutableMoveDataToNode(buffer: Array<Any?>, position: Int, oldKeyHash: Int, newKeyHash: Int, newKey: K, newValue: V,
//                                         shift: Int, mutator: PersistentHashMapBuilder<*, *>): Array<Any?> {
////        assert(hasDataAt(position))
////        assert(!hasNodeAt(position))
//
//    val keyIndex = keyDataIndex(buffer, position)
//    val nodeIndex = keyNodeIndex(buffer, position) - 1
//    val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
//    System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
//    System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, nodeIndex - keyIndex)
//    System.arraycopy(buffer, nodeIndex + 2, newBuffer, nodeIndex + 1, buffer.size - nodeIndex - 2)
//
//    newBuffer[nodeIndex] = makeNode(oldKeyHash, buffer[keyIndex] as K, buffer[keyIndex + 1] as V,
//            newKeyHash, newKey, newValue, shift + LOG_MAX_BRANCHING_FACTOR, mutator.marker)
//
//    newBuffer[0] = (newBuffer[0] as Int) xor position
//    newBuffer[1] = (newBuffer[1] as Int) or position
//    return newBuffer
//}

private fun <K, V> makeNode(keyHash1: Int, key1: K, value1: V, keyHash2: Int, key2: K, value2: V, shift: Int, mutatorMarker: Marker?): Array<Any?> {
    if (shift > MAX_SHIFT) {
//            assert(key1 != key2)
        return arrayOf(key1, value1, key2, value2, mutatorMarker)
    }

    val setBit1 = (keyHash1 shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE
    val setBit2 = (keyHash2 shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE

    if (setBit1 != setBit2) {
        val newDataMap = (1 shl setBit1) or (1 shl setBit2)
        if (setBit1 < setBit2) {
            return arrayOf(newDataMap, 0, key1, value1, key2, value2, mutatorMarker)
        }
        return arrayOf(newDataMap, 0, key2, value2, key1, value1, mutatorMarker)
    }
    val node = makeNode(keyHash1, key1, value1, keyHash2, key2, value2, shift + LOG_MAX_BRANCHING_FACTOR, mutatorMarker)
    return arrayOf(0, 1 shl setBit1, node, mutatorMarker)
}

private fun bufferRemoveDataAtIndex(buffer: Array<Any?>, keyIndex: Int): Array<Any?> {
    val newBuffer = arrayOfNulls<Any?>(buffer.size - 2)
    System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
    System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, buffer.size - keyIndex - 3) // marker
    return newBuffer
}

//private fun mutableBufferRemoveDataAtIndex(buffer: Array<Any?>, keyIndex: Int): Array<Any?> {
//    val newBuffer = arrayOfNulls<Any?>(buffer.size - 2)
//    System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
//    System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, buffer.size - keyIndex - 2)
//    return newBuffer
//}

private fun removeDataAt(buffer: Array<Any?>, position: Int): Array<Any?>? {
//        assert(hasDataAt(position))
    if (buffer.size == 5) { return null }

    val keyIndex = keyDataIndex(buffer, position)
    val newBuffer = bufferRemoveDataAtIndex(buffer, keyIndex)
    newBuffer[0] = (newBuffer[0] as Int) xor position
    return newBuffer
}

//private fun mutableRemoveDataAt(buffer: Array<Any?>, position: Int): Array<Any?> {
////        assert(hasDataAt(position))
//    val keyIndex = keyDataIndex(buffer, position)
//    val previousValue = buffer[keyIndex + 1]
//    buffer = mutableBufferRemoveDataAtIndex(buffer, keyIndex)
//    dataMap = dataMap xor position
//    return previousValue as V
//}

private fun collisionRemoveDataAt(buffer: Array<Any?>, i: Int): Array<Any?>? {
    if (buffer.size == 3) { return null }
    return bufferRemoveDataAtIndex(buffer, i)
}

//private fun mutableCollisionRemoveDataAt(i: Int): V? {
//    val previousValue = buffer[i + 1]
//    buffer = mutableBufferRemoveDataAtIndex(i)
//    return previousValue as V
//}

private fun removeNodeAt(buffer: Array<Any?>, position: Int): Array<Any?>? {
//        assert(hasNodeAt(position))
    if (buffer.size == 4) { return null } // marker

    val keyIndex = keyNodeIndex(buffer, position)
    val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
    System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
    System.arraycopy(buffer, keyIndex + 1, newBuffer, keyIndex, buffer.size - keyIndex - 2) // marker
    newBuffer[1] = (newBuffer[1] as Int) xor position
    return newBuffer
}

//private fun mutableRemoveNodeAt(buffer: Array<Any?>, position: Int) {
////        assert(hasNodeAt(position))
//    val keyIndex = keyNodeIndex(position)
//    val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
//    System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
//    System.arraycopy(buffer, keyIndex + 1, newBuffer, keyIndex, buffer.size - keyIndex - 1)
//    buffer = newBuffer
//    nodeMap = nodeMap xor position
//}

private fun <K, V> collisionGet(buffer: Array<Any?>, key: K): V? {
    for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
        if (key == buffer[i]) { return buffer[i + 1] as V }
    }
    return null
}

private fun <K, V> collisionPut(buffer: Array<Any?>, key: K, value: V, modification: ModificationWrapper): Array<Any?> {
    for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
        if (key == buffer[i]) {
            if (value === buffer[i + 1]) {
                return buffer
            }
            modification.value = UPDATE_VALUE
            val newBuffer = buffer.copyOf()
            newBuffer[i + 1] = value
            newBuffer[newBuffer.size - 1] = null // marker
            return newBuffer
        }
    }
    modification.value = PUT_KEY_VALUE
    val newBuffer = arrayOfNulls<Any?>(buffer.size + 2)
    System.arraycopy(buffer, 0, newBuffer, 2, buffer.size - 1) // marker
    newBuffer[0] = key
    newBuffer[1] = value
    return newBuffer
}

//private fun mutableCollisionPut(buffer: Array<Any?>, key: K, value: V, mutator: PersistentHashMapBuilder<*, *>): V? {
//    for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
//        if (key == buffer[i]) {
//            val previousValue = buffer[i + 1]
//            buffer[i + 1] = value
//            return previousValue as V
//        }
//    }
//    mutator.size++
//    val newBuffer = arrayOfNulls<Any?>(buffer.size + 2)
//    System.arraycopy(buffer, 0, newBuffer, 2, buffer.size)
//    newBuffer[0] = key
//    newBuffer[1] = value
//    buffer = newBuffer
//    return null
//}

private fun <K> collisionRemove(buffer: Array<Any?>, key: K): Array<Any?>? {
    for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
        if (key == buffer[i]) {
            return collisionRemoveDataAt(buffer, i)
        }
    }
    return buffer
}

//private fun mutableCollisionRemove(buffer: Array<Any?>, key: K, mutator: PersistentHashMapBuilder<*, *>): V? {
//    for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
//        if (key == buffer[i]) {
//            mutator.size--
//            return mutableCollisionRemoveDataAt(i)
//        }
//    }
//    return null
//}

private fun <K, V> collisionRemove(buffer: Array<Any?>, key: K, value: V): Array<Any?>? {
    for (i in 0 until buffer.size - 1 step ENTRY_SIZE) {
        if (key == buffer[i] && value == buffer[i + 1]) {
            return collisionRemoveDataAt(buffer, i)
        }
    }
    return buffer
}

internal fun <K, V> node_get(buffer: Array<Any?>, keyHash: Int, key: K, shift: Int): V? {
    val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

    if (hasDataAt(buffer, keyPosition)) { // key is directly in buffer
        val storedKey = keyAt<K>(buffer, keyPosition)!!
        if (key == storedKey) {
            return valueAt(buffer, keyPosition)
        }
        return null
    }
    if (hasNodeAt(buffer, keyPosition)) { // key is in node
        val targetNode = nodeAt(buffer, keyPosition)
        if (shift == MAX_SHIFT) {
            return collisionGet(targetNode, key)
        }
        return node_get(targetNode, keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
    }

    // key is absent
    return null
}

internal fun <K, V> node_put(buffer: Array<Any?>, keyHash: Int, key: K, value: @UnsafeVariance V,
                             shift: Int, modification: ModificationWrapper): Array<Any?> {
    val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

    if (hasDataAt(buffer, keyPosition)) { // key is directly in buffer
        val oldKey = keyAt<K>(buffer, keyPosition)!!
        if (key == oldKey) {
            if (valueAt<V>(buffer, keyPosition) === value) { return buffer }
            modification.value = UPDATE_VALUE
            return updateValueAt(buffer, keyPosition, value)
        }
        modification.value = PUT_KEY_VALUE
        val oldKeyHash = oldKey.hashCode()
        return moveDataToNode(buffer, keyPosition, oldKeyHash, keyHash, key, value, shift)
    }
    if (hasNodeAt(buffer, keyPosition)) { // key is in node
        val targetNode = nodeAt(buffer, keyPosition)
        val newNode = if (shift == MAX_SHIFT) {
            collisionPut(targetNode, key, value, modification)
        } else {
            node_put(targetNode, keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, modification)
        }
        if (targetNode === newNode) { return buffer }
        return updateNodeAt(buffer, keyPosition, newNode)
    }

    // key is absent
    modification.value = PUT_KEY_VALUE
    return putDataAt(buffer, keyPosition, key, value)
}

//internal fun <K, V> node_mutablePut(buffer: Array<Any?>, keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int, mutator: PersistentHashMapBuilder<*, *>): V? {
//    ensureMutableBy(buffer, mutator)
//    val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)
//
//    if (hasDataAt(buffer, keyPosition)) { // key is directly in buffer
//        val oldKey = keyAt<K>(buffer, keyPosition)!!
//        if (key == oldKey) {
//            return mutableUpdateValueAt(buffer, keyPosition, value)
//        }
//        mutator.size++
//        val oldKeyHash = oldKey.hashCode()
//        mutableMoveDataToNode(buffer, keyPosition, oldKeyHash, keyHash, key, value, shift, mutator)
//        return null
//    }
//    if (hasNodeAt(buffer, keyPosition)) { // key is in node
//        val targetNode =  node_makeMutableFor(nodeAt(buffer, keyPosition), mutator)
//        mutableUpdateNodeAt(buffer, keyPosition, targetNode)
//        return if (shift == MAX_SHIFT) {
//            mutableCollisionPut(targetNode, key, value, mutator)
//        } else {
//            node_mutablePut(targetNode, keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
//        }
//    }
//
//    // key is absent
//    mutator.size++
//    mutablePutDataAt(buffer, keyPosition, key, value)
//    return null
//}

internal fun <K> node_remove(buffer: Array<Any?>, keyHash: Int, key: K, shift: Int): Array<Any?>? {
    val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

    if (hasDataAt(buffer, keyPosition)) { // key is directly in buffer
        val oldKey = keyAt<K>(buffer, keyPosition)!!
        if (key == oldKey) {
            return removeDataAt(buffer, keyPosition)
        }
        return buffer
    }
    if (hasNodeAt(buffer, keyPosition)) { // key is in node
        val targetNode = nodeAt(buffer, keyPosition)
        val newNode: Array<Any?>? = if (shift == MAX_SHIFT) {
            collisionRemove(targetNode, key)
        } else {
            node_remove(targetNode, keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
        }
        if (targetNode === newNode) { return buffer}
        if (newNode == null) { return removeNodeAt(buffer, keyPosition) }
        return updateNodeAt(buffer, keyPosition, newNode)
    }

    // key is absent
    return buffer
}

internal fun <K, V> node_remove(buffer: Array<Any?>, keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int): Array<Any?>? {
    val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

    if (hasDataAt(buffer, keyPosition)) { // key is directly in buffer
        val oldKey = keyAt<K>(buffer, keyPosition)
        val oldValue = valueAt<V>(buffer, keyPosition)
        if (key == oldKey && value == oldValue) {
            return removeDataAt(buffer, keyPosition)
        }
        return buffer
    }
    if (hasNodeAt(buffer, keyPosition)) { // key is in node
        val targetNode = nodeAt(buffer, keyPosition)
        val newNode: Array<Any?>? = if (shift == MAX_SHIFT) {
            collisionRemove(targetNode, key, value)
        } else {
            node_remove(targetNode, keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR)
        }
        if (targetNode === newNode) { return buffer }
        if (newNode == null) { return removeNodeAt(buffer, keyPosition) }
        return updateNodeAt(buffer, keyPosition, newNode)
    }

    // key is absent
    return buffer
}

internal val NODE_EMPTY = arrayOf<Any?>(0, 0, null)