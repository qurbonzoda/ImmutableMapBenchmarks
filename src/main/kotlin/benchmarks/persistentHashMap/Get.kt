package benchmarks.persistentHashMap

import benchmarks.*
import kotlinx.collections.immutable.ImmutableMap
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
    @Param(BM_1, BM_4, BM_10, BM_15, BM_20, BM_25, BM_50,
            BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var listSize: Int = 0

    private val emptyMap = persistentHashMapOf<Int, String>()
    private val random = Random()
    private val distinctKeys = mutableListOf<Int>()
    private val randomKeys = mutableListOf<Int>()
    private val collisionKeys = mutableListOf<Int>()
    private val values = mutableListOf<String>()

    private val anotherRandomKeys = mutableListOf<Int>()

    private var distinctKeysMap = persistentHashMapOf<Int, String>()
    private var randomKeysMap = persistentHashMapOf<Int, String>()
    private var collisionKeysMap = persistentHashMapOf<Int, String>()

    @Setup(Level.Trial)
    fun prepare() {
        distinctKeys.clear()
        randomKeys.clear()
        collisionKeys.clear()
        values.clear()
        anotherRandomKeys.clear()
        repeat(times = this.listSize) { index ->
            distinctKeys.add(index)
            randomKeys.add(random.nextInt())
            anotherRandomKeys.add(random.nextInt())
            collisionKeys.add(random.nextInt((this.listSize + 1) / 2))
            values.add("some element" + random.nextInt(3))
        }

        distinctKeysMap = this.emptyMap
        randomKeysMap = this.emptyMap
        collisionKeysMap = this.emptyMap
        repeat(times = this.listSize) { index ->
            distinctKeysMap = distinctKeysMap.put(distinctKeys[index], values[index])
            randomKeysMap = randomKeysMap.put(randomKeys[index], values[index])
            collisionKeysMap = collisionKeysMap.put(collisionKeys[index], values[index])
        }
    }

    @Benchmark
    fun getDistinct(bh: Blackhole): ImmutableMap<Int, String> {
        val map = distinctKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[distinctKeys[index]])
        }
        return map
    }

    @Benchmark
    fun getRandom(bh: Blackhole): ImmutableMap<Int, String> {
        val map = randomKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[randomKeys[index]])
        }
        return map
    }

    @Benchmark
    fun getCollision(bh: Blackhole): ImmutableMap<Int, String> {
        val map = collisionKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[collisionKeys[index]])
        }
        return map
    }

    @Benchmark
    fun getNonExisting(bh: Blackhole): ImmutableMap<Int, String> {
        val map = randomKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[anotherRandomKeys[index]])
        }
        return map
    }
}