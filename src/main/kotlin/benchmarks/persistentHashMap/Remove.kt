package benchmarks.persistentHashMap

import benchmarks.*
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.implementations.immutableMap.KeyWrapper
import kotlinx.collections.immutable.implementations.immutableMap.persistentHashMapOf
import org.openjdk.jmh.annotations.*
import java.util.*
import java.util.concurrent.TimeUnit

@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
open class Remove {
    @Param(BM_1, BM_4, BM_10, BM_15, BM_20, BM_25, BM_50,
            BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var listSize: Int = 0

    @Param("persistentHashMap")
    var implementation = ""

    private val emptyMap = persistentHashMapOf<KeyWrapper<Int>, String>()
    private val random = Random()

    private val distinctKeys = mutableListOf<KeyWrapper<Int>>()
    private val randomKeys = mutableListOf<KeyWrapper<Int>>()
    private val collisionKeys = mutableListOf<KeyWrapper<Int>>()

    private val anotherRandomKeys = mutableListOf<KeyWrapper<Int>>()

    private var distinctKeysMap = persistentHashMapOf<KeyWrapper<Int>, String>()
    private var randomKeysMap = persistentHashMapOf<KeyWrapper<Int>, String>()
    private var collisionKeysMap = persistentHashMapOf<KeyWrapper<Int>, String>()

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

        distinctKeysMap = this.emptyMap
        randomKeysMap = this.emptyMap
        collisionKeysMap = this.emptyMap
        repeat(times = this.listSize) { index ->
            distinctKeysMap = distinctKeysMap.put(distinctKeys[index], "some element")
            randomKeysMap = randomKeysMap.put(randomKeys[index], "some element")
            collisionKeysMap = collisionKeysMap.put(collisionKeys[index], "some element")
        }
    }

    @Benchmark
    fun removeDistinct(): ImmutableMap<KeyWrapper<Int>, String> {
        var map = distinctKeysMap
        repeat(times = this.listSize) { index ->
            map = map.remove(distinctKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeRandom(): ImmutableMap<KeyWrapper<Int>, String> {
        var map = randomKeysMap
        repeat(times = this.listSize) { index ->
            map = map.remove(randomKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeCollision(): ImmutableMap<KeyWrapper<Int>, String> {
        var map = collisionKeysMap
        repeat(times = this.listSize) { index ->
            map = map.remove(collisionKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeNonExisting(): ImmutableMap<KeyWrapper<Int>, String> {
        var map = randomKeysMap
        repeat(times = this.listSize) { index ->
            map = map.remove(anotherRandomKeys[index])
        }
        return map
    }
}