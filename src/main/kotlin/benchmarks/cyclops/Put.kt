package benchmarks.cyclops

import benchmarks.*
import com.aol.cyclops.clojure.collections.ClojureHashPMap
import kotlinx.collections.immutable.implementations.immutableMap.KeyWrapper
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.pcollections.PMap
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

    @Param(CLOJURE_HASH_MAP, SCALA_HASH_MAP, SCALA_TREE_MAP,
            JAVASLANG_HASH_MAP, JAVASLANG_TREE_MAP, PCOLLECTIONS)
    var implementation = ""

    private var emptyMap: PMap<KeyWrapper<Int>, String> = ClojureHashPMap.empty()
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
    fun putDistinct(): PMap<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.plus(distinctKeys[index], "some element")
        }
        return map
    }

    @Benchmark
    fun putRandom(): PMap<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.plus(randomKeys[index], "some element")
        }
        return map
    }

    @Benchmark
    fun putCollision(): PMap<KeyWrapper<Int>, String> {
        var map = this.emptyMap
        repeat(times = this.listSize) { index ->
            map = map.plus(collisionKeys[index], "some element")
        }
        return map
    }

//    @Benchmark
//    fun putAndGetDistinct(bh: Blackhole): PMap<KeyWrapper<Int>, String> {
//        var map = this.emptyMap
//        repeat(times = this.listSize) { index ->
//            map = map.plus(distinctKeys[index], "some element")
//        }
//        repeat(times = this.listSize) { index ->
//            bh.consume(map[distinctKeys[index]])
//        }
//        return map
//    }
//
//    @Benchmark
//    fun putAndGetRandom(bh: Blackhole): PMap<KeyWrapper<Int>, String> {
//        var map = this.emptyMap
//        repeat(times = this.listSize) { index ->
//            map = map.plus(randomKeys[index], "some element")
//        }
//        repeat(times = this.listSize) { index ->
//            bh.consume(map[randomKeys[index]])
//        }
//        return map
//    }
//
//    @Benchmark
//    fun putAndGetCollision(bh: Blackhole): PMap<KeyWrapper<Int>, String> {
//        var map = this.emptyMap
//        repeat(times = this.listSize) { index ->
//            map = map.plus(collisionKeys[index], "some element")
//        }
//        repeat(times = this.listSize) { index ->
//            bh.consume(map[collisionKeys[index]])
//        }
//        return map
//    }
}

