/*
 * Copyright 2016-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.collections.immutable.implementations.immutableMap

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.mutate

const val MAX_BRANCHING_FACTOR = 32
const val LOG_MAX_BRANCHING_FACTOR = 5
const val MAX_BRANCHING_FACTOR_MINUS_ONE = MAX_BRANCHING_FACTOR - 1
const val ENTRY_SIZE = 2
const val MAX_SHIFT = 30


const val NO_MODIFICATION = 0
const val UPDATE_VALUE = 1
const val PUT_KEY_VALUE = 2
const val REMOVE_KEY_VALUE = 4
internal class ModificationWrapper(var value: Int = NO_MODIFICATION)


internal class TrieNode<K, V>(val dataMap: Int,
                              val nodeMap: Int,
                              val buffer: Array<Any?>) {
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
        return buffer.size - 1 - Integer.bitCount(nodeMap and (position - 1))
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
        assert(!hasDataAt(position))

        val keyIndex = keyDataIndex(position)
        val newBuffer = arrayOfNulls<Any?>(buffer.size + 2)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex, newBuffer, keyIndex + 2, buffer.size - keyIndex)
        newBuffer[keyIndex] = key
        newBuffer[keyIndex + 1] = value
        return TrieNode(dataMap or position, nodeMap, newBuffer)
    }

    private fun updateDataAt(position: Int, value: V): TrieNode<K, V> {
        assert(hasDataAt(position))

        val keyIndex = keyDataIndex(position)
        val newBuffer = buffer.copyOf()
        newBuffer[keyIndex + 1] = value
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    private fun updateNodeAt(position: Int, newNode: TrieNode<K, V>): TrieNode<K, V> {
        assert(hasNodeAt(position))

        val nodeIndex = keyNodeIndex(position)
        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    private fun moveDataToNode(position: Int, oldKeyHash: Int, newKeyHash: Int,
                               newKey: K, newValue: V, shift: Int): TrieNode<K, V> {
        assert(hasDataAt(position))
        assert(!hasNodeAt(position))

        val keyIndex = keyDataIndex(position)
        val nodeIndex = keyNodeIndex(position) - 1
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, nodeIndex - keyIndex)
        System.arraycopy(buffer, nodeIndex + 2, newBuffer, nodeIndex + 1, buffer.size - nodeIndex - 2)

        newBuffer[nodeIndex] = makeNode(oldKeyHash, buffer[keyIndex] as K, buffer[keyIndex + 1] as V,
                newKeyHash, newKey, newValue, shift + LOG_MAX_BRANCHING_FACTOR)
        return TrieNode(dataMap xor position, nodeMap or position, newBuffer)
    }

    private fun makeNode(keyHash1: Int, key1: K, value1: V, keyHash2: Int, key2: K, value2: V, shift: Int): TrieNode<K, V> {
        if (shift > MAX_SHIFT) {
            assert(key1 != key2)
            return TrieNode(0, 0, arrayOf(key1, value1, key2, value2))
        }

        val setBit1 = (keyHash1 shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE
        val setBit2 = (keyHash2 shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE

        if (setBit1 != setBit2) {
            val newDataMap = (1 shl setBit1) or (1 shl setBit2)
            if (setBit1 < setBit2) {
                return TrieNode(newDataMap, 0, arrayOf(key1, value1, key2, value2))
            }
            return TrieNode(newDataMap, 0, arrayOf(key2, value2, key1, value1))
        }
        val node = makeNode(keyHash1, key1, value1, keyHash2, key2, value2, shift + LOG_MAX_BRANCHING_FACTOR)
        return TrieNode<K, V>(0, 1 shl setBit1, arrayOf(node))
    }

    private fun removeDataAt(position: Int): TrieNode<K, V> {
        assert(hasDataAt(position))

        val keyIndex = keyDataIndex(position)
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 2)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, buffer.size - keyIndex - 2)
        return TrieNode(dataMap xor position, nodeMap, newBuffer)
    }

    private fun collisionGet(key: K): V? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) { return buffer[i + 1] as V }
        }
        return null
    }

    private fun collisionPut(key: K, value: V, modification: ModificationWrapper): TrieNode<K, V> {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) {
                if (value === buffer[i + 1]) {
                    return this
                }
                modification.value = UPDATE_VALUE
                val newBuffer = buffer.copyOf()
                newBuffer[i + 1] = value
                return TrieNode(0, 0, newBuffer)
            }
        }
        modification.value = PUT_KEY_VALUE
        val newBuffer = arrayOfNulls<Any?>(buffer.size + 2)
        System.arraycopy(buffer, 0, newBuffer, 2, buffer.size)
        newBuffer[0] = key
        newBuffer[1] = value
        return TrieNode(0, 0, newBuffer)
    }

    private fun collisionRemove(key: K, modification: ModificationWrapper): TrieNode<K, V> {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) {
                modification.value = REMOVE_KEY_VALUE
                val newBuffer = arrayOfNulls<Any?>(buffer.size - 2)
                System.arraycopy(buffer, 0, newBuffer, 0, i)
                System.arraycopy(buffer, i + 2, newBuffer, i, buffer.size - i - 2)
                return TrieNode(0, 0, newBuffer)
            }
        }
        return this
    }

    private fun collisionRemove(key: K, value: V, modification: ModificationWrapper): TrieNode<K, V> {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i] && value == buffer[i + 1]) {
                modification.value = REMOVE_KEY_VALUE
                val newBuffer = arrayOfNulls<Any?>(buffer.size - 2)
                System.arraycopy(buffer, 0, newBuffer, 0, i)
                System.arraycopy(buffer, i + 2, newBuffer, i, buffer.size - i - 2)
                return TrieNode(0, 0, newBuffer)
            }
        }
        return this
    }

    fun get(keyHash: Int, key: K, shift: Int): V? {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val storedKey = keyAt<K>(keyPosition)!!
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
            val oldKey = keyAt<K>(keyPosition)!!
            if (key == oldKey) {
                modification.value = UPDATE_VALUE
                return updateDataAt(keyPosition, value)
            }
            modification.value = PUT_KEY_VALUE
            val oldKeyHash = oldKey.hashCode()
            return moveDataToNode(keyPosition, oldKeyHash, keyHash, key, value, shift)
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAt(keyPosition)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionPut(key, value, modification)
            } else {
                targetNode.put(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, modification)
            }
            return updateNodeAt(keyPosition, newNode)
        }

        // key is absent
        modification.value = PUT_KEY_VALUE
        return putDataAt(keyPosition, key, value)
    }

    fun remove(keyHash: Int, key: K, shift: Int, modification: ModificationWrapper): TrieNode<K, V> {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val oldKey = keyAt<K>(keyPosition)!!
            if (key == oldKey) {
                modification.value = REMOVE_KEY_VALUE
                return removeDataAt(keyPosition)
            }
            return this
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAt(keyPosition)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(key, modification)
            } else {
                targetNode.remove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR, modification)
            }
            return updateNodeAt(keyPosition, newNode)
        }

        // key is absent
        return this
    }

    fun remove(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int, modification: ModificationWrapper): TrieNode<K, V> {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val oldKey = keyAt<K>(keyPosition)
            val oldValue = valueAt<V>(keyPosition)
            if (key == oldKey && value == oldValue) {
                modification.value = REMOVE_KEY_VALUE
                return removeDataAt(keyPosition)
            }
            return this
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAt(keyPosition)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(key, value, modification)
            } else {
                targetNode.remove(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, modification)
            }
            return updateNodeAt(keyPosition, newNode)
        }

        // key is absent
        return this
    }
}

internal class PersistentHashMap<K, out V>(private val node: TrieNode<K, V>,
                                           override val size: Int): ImmutableMap<K, V> {

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override val keys: Set<K>
        get() {
            val iterator = PersistentHashMapIterator(node)
            val keys = mutableSetOf<K>()
            while (iterator.hasNext()) {
                keys.add(iterator.nextKey())
            }
            return keys
        }
    override val values: Collection<V>
        get() {
            val iterator = PersistentHashMapIterator(node)
            val values = mutableListOf<V>()
            while (iterator.hasNext()) {
                values.add(iterator.nextValue())
            }
            return values
        }
    override val entries: Set<Map.Entry<K, V>>
        get() {
            val iterator = PersistentHashMapIterator(node)
            val entries = mutableSetOf<Map.Entry<K, V>>()
            while (iterator.hasNext()) {
                entries.add(iterator.nextEntry())
            }
            return entries
        }

    override fun containsKey(key: K): Boolean {
        return get(key) != null     // what if value is optional?
    }

    override fun containsValue(value: @UnsafeVariance V): Boolean {
        return values.contains(value)   // TODO: optimize
    }

    override fun get(key: K): V? {
        if (key == null) {
            throw IllegalArgumentException()
        }

        val keyHash = key.hashCode()
        return node.get(keyHash, key, 0)
    }

    override fun put(key: K, value: @UnsafeVariance V): ImmutableMap<K, V> {
        if (key == null) {
            throw IllegalArgumentException()
        }

        val keyHash = key.hashCode()
        val modification = ModificationWrapper()
        val newNode = node.put(keyHash, key, value, 0, modification)
        val sizeDelta = if (modification.value == PUT_KEY_VALUE) 1 else 0
        return PersistentHashMap(newNode, size + sizeDelta)
    }

    override fun remove(key: K): ImmutableMap<K, V> {
        if (key == null) {
            throw IllegalArgumentException()
        }

        val keyHash = key.hashCode()
        val modification = ModificationWrapper()
        val newNode = node.remove(keyHash, key, 0, modification)
        val sizeDelta = if (modification.value == REMOVE_KEY_VALUE) -1 else 0
        return PersistentHashMap(newNode, size + sizeDelta)
    }

    override fun remove(key: K, value: @UnsafeVariance V): ImmutableMap<K, V> {
        if (key == null) {
            throw IllegalArgumentException()
        }

        val keyHash = key.hashCode()
        val modification = ModificationWrapper()
        val newNode = node.remove(keyHash, key, value, 0, modification)
        val sizeDelta = if (modification.value == REMOVE_KEY_VALUE) -1 else 0
        return PersistentHashMap(newNode, size + sizeDelta)
    }

    override fun putAll(m: Map<out K, @UnsafeVariance V>): ImmutableMap<K, V> {
        return this.mutate { it.putAll(m) }
    }

    override fun clear(): ImmutableMap<K, V> {
        return persistentHashMapOf()
    }

    override fun builder(): ImmutableMap.Builder<K, @UnsafeVariance V> {
        return PersistentHashMapBuilder(node, size)
    }

    internal companion object {
        internal val EMPTY = PersistentHashMap<Any?, Nothing>(TrieNode(0, 0, emptyArray()), 0)
    }
}

fun <K, V> persistentHashMapOf(): ImmutableMap<K, V> {
    return PersistentHashMap.EMPTY as PersistentHashMap<K, V>
}