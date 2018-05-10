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

internal class PersistentHashMapBuilder<K, V>(private val node: TrieNode<K, V>,
                                              override var size: Int) : ImmutableMap.Builder<K, V>, AbstractMutableMap<K, V>() {
    override fun build(): ImmutableMap<K, V> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun put(key: K, value: V): V? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}