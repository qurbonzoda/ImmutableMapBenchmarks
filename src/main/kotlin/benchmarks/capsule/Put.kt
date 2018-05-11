package benchmarks.capsule

import benchmarks.*
import kotlinx.collections.immutable.implementations.immutableMap.KeyWrapper
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import io.usethesource.capsule.Map
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

    @Param(CAPSULE_TRIE_MAP)
    var implementation = ""

    private var emptyMap: Map.Immutable<KeyWrapper<Int>, String> = emptyPMap(CAPSULE_TRIE_MAP)
    private val random = Random()

    private val distinctKeys = mutableListOf<KeyWrapper<Int>>()
    private var randomKeys = mutableListOf<KeyWrapper<Int>>()
    private var collisionKeys = mutableListOf<KeyWrapper<Int>>()

    @Setup(Level.Trial)
    fun prepare() {
        emptyMap = emptyPMap(implementation)

        distinctKeys.clear()
        randomKeys.clear()
        collisionKeys.clear()
        repeat(times = listSize) { index ->
            distinctKeys.add(KeyWrapper(index, index))
            randomKeys.add(KeyWrapper(index, random.nextInt()))
            collisionKeys.add(KeyWrapper(index, random.nextInt((listSize + 1) / 2)))
        }
    }

    @Benchmark
    fun putDistinct(): Map.Immutable<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.__put(distinctKeys[index], "some element")
        }
        return map
    }

    @Benchmark
    fun putRandom(): Map.Immutable<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.__put(randomKeys[index], "some element")
        }
        return map
    }

    @Benchmark
    fun putCollision(): Map.Immutable<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.__put(collisionKeys[index], "some element")
        }
        return map
    }

    @Benchmark
    fun putAndGetDistinct(bh: Blackhole): Map.Immutable<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.__put(distinctKeys[index], "some element")
        }
        repeat(times = this.listSize) { index ->
            bh.consume(map[distinctKeys[index]])
        }
        return map
    }

    @Benchmark
    fun putAndGetRandom(bh: Blackhole): Map.Immutable<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.__put(randomKeys[index], "some element")
        }
        repeat(times = this.listSize) { index ->
            bh.consume(map[randomKeys[index]])
        }
        return map
    }

    @Benchmark
    fun putAndGetCollision(bh: Blackhole): Map.Immutable<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.__put(collisionKeys[index], "some element")
        }
        repeat(times = this.listSize) { index ->
            bh.consume(map[collisionKeys[index]])
        }
        return map
    }
}

