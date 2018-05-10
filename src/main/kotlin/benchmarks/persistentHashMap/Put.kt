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
open class Put {
    @Param(BM_1, BM_4, BM_10, BM_15, BM_20, BM_25, BM_50,
            BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var listSize: Int = 0

    private val emptyMap: ImmutableMap<Int, String> = persistentHashMapOf()
    private val random = Random()
    private val distinctKeys = mutableListOf<Int>()
    private var randomKeys = mutableListOf<Int>()
    private var collisionKeys = mutableListOf<Int>()
    private var values = mutableListOf<String>()

    @Setup(Level.Trial)
    fun prepare() {
        distinctKeys.clear()
        randomKeys.clear()
        collisionKeys.clear()
        values.clear()
        repeat(times = this.listSize) { index ->
            distinctKeys.add(index)
            randomKeys.add(random.nextInt())
            collisionKeys.add(random.nextInt((this.listSize + 1) / 2))
            values.add("some element" + random.nextInt(3))
        }
    }

    @Benchmark
    fun putDistinct(): ImmutableMap<Int, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.put(distinctKeys[index], values[index])
        }
        return map
    }

    @Benchmark
    fun putRandom(): ImmutableMap<Int, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.put(randomKeys[index], values[index])
        }
        return map
    }

    @Benchmark
    fun putCollision(): ImmutableMap<Int, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.put(collisionKeys[index], values[index])
        }
        return map
    }

    @Benchmark
    fun putAndGetDistinct(bh: Blackhole): ImmutableMap<Int, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.put(distinctKeys[index], values[index])
        }
        repeat(times = this.listSize) { index ->
            bh.consume(map[distinctKeys[index]])
        }
        return map
    }

    @Benchmark
    fun putAndGetRandom(bh: Blackhole): ImmutableMap<Int, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.put(randomKeys[index], values[index])
        }
        repeat(times = this.listSize) { index ->
            bh.consume(map[randomKeys[index]])
        }
        return map
    }

    @Benchmark
    fun putAndGetCollision(bh: Blackhole): ImmutableMap<Int, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.put(collisionKeys[index], values[index])
        }
        repeat(times = this.listSize) { index ->
            bh.consume(map[collisionKeys[index]])
        }
        return map
    }
}