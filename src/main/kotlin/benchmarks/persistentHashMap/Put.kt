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
open class Put {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var listSize: Int = 0

    @Param("persistentHashMap")
    var implementation = ""

    private val emptyMap: ImmutableMap<KeyWrapper<Int>, String> = persistentHashMapOf()
    private val random = Random()

    private val distinctKeys = mutableListOf<KeyWrapper<Int>>()
    private var randomKeys = mutableListOf<KeyWrapper<Int>>()
    private var collisionKeys = mutableListOf<KeyWrapper<Int>>()

    @Setup(Level.Trial)
    fun prepare() {
        if (implementation != "persistentHashMap") {
            throw AssertionError("Unknown implementation: $implementation")
        }

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
    fun putDistinct(): ImmutableMap<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.put(distinctKeys[index], "some element")
        }
        return map
    }

    @Benchmark
    fun putRandom(): ImmutableMap<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.put(randomKeys[index], "some element")
        }
        return map
    }

    @Benchmark
    fun putCollision(): ImmutableMap<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.put(collisionKeys[index], "some element")
        }
        return map
    }

//    @Benchmark
//    fun putAndGetDistinct(bh: Blackhole): ImmutableMap<KeyWrapper<Int>, String> {
//        var map = this.emptyMap
//        repeat(times = this.listSize) { index ->
//            map = map.put(distinctKeys[index], "some element")
//        }
//        repeat(times = this.listSize) { index ->
//            bh.consume(map[distinctKeys[index]])
//        }
//        return map
//    }
//
//    @Benchmark
//    fun putAndGetRandom(bh: Blackhole): ImmutableMap<KeyWrapper<Int>, String> {
//        var map = this.emptyMap
//        repeat(times = this.listSize) { index ->
//            map = map.put(randomKeys[index], "some element")
//        }
//        repeat(times = this.listSize) { index ->
//            bh.consume(map[randomKeys[index]])
//        }
//        return map
//    }
//
//    @Benchmark
//    fun putAndGetCollision(bh: Blackhole): ImmutableMap<KeyWrapper<Int>, String> {
//        var map = this.emptyMap
//        repeat(times = this.listSize) { index ->
//            map = map.put(collisionKeys[index], "some element")
//        }
//        repeat(times = this.listSize) { index ->
//            bh.consume(map[collisionKeys[index]])
//        }
//        return map
//    }
}