package benchmarks.capsule

import benchmarks.*
import kotlinx.collections.immutable.implementations.immutableMap.KeyWrapper
import org.openjdk.jmh.annotations.*
import io.usethesource.capsule.Map
import java.util.*
import java.util.concurrent.TimeUnit

@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
open class Remove {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var listSize: Int = 0

    @Param(CAPSULE_TRIE_MAP)
    var implementation = ""

    private val random = Random(40)

    private val distinctKeys = mutableListOf<KeyWrapper<Int>>()
    private val randomKeys = mutableListOf<KeyWrapper<Int>>()
    private val collisionKeys = mutableListOf<KeyWrapper<Int>>()

    private val anotherRandomKeys = mutableListOf<KeyWrapper<Int>>()

    private var distinctKeysMap: Map.Immutable<KeyWrapper<Int>, String> = emptyPMap(CAPSULE_TRIE_MAP)
    private var randomKeysMap: Map.Immutable<KeyWrapper<Int>, String> = emptyPMap(CAPSULE_TRIE_MAP)
    private var collisionKeysMap: Map.Immutable<KeyWrapper<Int>, String> = emptyPMap(CAPSULE_TRIE_MAP)

    @Setup(Level.Trial)
    fun prepare() {
        distinctKeys.clear()
        randomKeys.clear()
        collisionKeys.clear()
        anotherRandomKeys.clear()
        repeat(times = listSize) { index ->
            distinctKeys.add(KeyWrapper(index, index))
            randomKeys.add(KeyWrapper(index, random.nextInt()))
            collisionKeys.add(KeyWrapper(index, random.nextInt((listSize + 1) / 2)))
            anotherRandomKeys.add(KeyWrapper(random.nextInt(), random.nextInt()))
        }

        val emptyMap = emptyPMap<KeyWrapper<Int>, String>(implementation)
        distinctKeysMap = emptyMap
        randomKeysMap = emptyMap
        collisionKeysMap = emptyMap
        repeat(times = this.listSize) { index ->
            distinctKeysMap = distinctKeysMap.__put(distinctKeys[index], "some element")
            randomKeysMap = randomKeysMap.__put(randomKeys[index], "some element")
            collisionKeysMap = collisionKeysMap.__put(collisionKeys[index], "some element")
        }
    }

    @Benchmark
    fun removeDistinct(): Map.Immutable<KeyWrapper<Int>, String> {
        var map = distinctKeysMap
        repeat(times = this.listSize) { index ->
            map = map.__remove(distinctKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeRandom(): Map.Immutable<KeyWrapper<Int>, String> {
        var map = randomKeysMap
        repeat(times = this.listSize) { index ->
            map = map.__remove(randomKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeCollision(): Map.Immutable<KeyWrapper<Int>, String> {
        var map = collisionKeysMap
        repeat(times = this.listSize) { index ->
            map = map.__remove(collisionKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeNonExisting(): Map.Immutable<KeyWrapper<Int>, String> {
        var map = randomKeysMap
        repeat(times = this.listSize) { index ->
            map = map.__remove(anotherRandomKeys[index])
        }
        return map
    }
}