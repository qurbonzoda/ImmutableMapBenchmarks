package benchmarks.persistentHashMap

import benchmarks.*
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.implementations.immutableMap.KeyWrapper
import kotlinx.collections.immutable.implementations.immutableMap.persistentHashMapOf
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.*
import java.util.concurrent.TimeUnit

@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
open class Get {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
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
        if (implementation != "persistentHashMap") {
            throw AssertionError("Unknown implementation: $implementation")
        }

        distinctKeys.clear()
        randomKeys.clear()
        collisionKeys.clear()
        anotherRandomKeys.clear()
        repeat(times = this.listSize) { index ->
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
    fun getDistinct(bh: Blackhole): ImmutableMap<KeyWrapper<Int>, String> {
        val map = distinctKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[distinctKeys[index]])
        }
        return map
    }

    @Benchmark
    fun getRandom(bh: Blackhole): ImmutableMap<KeyWrapper<Int>, String> {
        val map = randomKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[randomKeys[index]])
        }
        return map
    }

    @Benchmark
    fun getCollision(bh: Blackhole): ImmutableMap<KeyWrapper<Int>, String> {
        val map = collisionKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[collisionKeys[index]])
        }
        return map
    }

    @Benchmark
    fun getNonExisting(bh: Blackhole): ImmutableMap<KeyWrapper<Int>, String> {
        val map = randomKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[anotherRandomKeys[index]])
        }
        return map
    }
}