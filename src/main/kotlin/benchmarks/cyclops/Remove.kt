package benchmarks.cyclops

import benchmarks.*
import com.aol.cyclops.clojure.collections.ClojureHashPMap
import kotlinx.collections.immutable.implementations.immutableMap.KeyWrapper
import org.openjdk.jmh.annotations.*
import org.pcollections.PMap
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

    @Param(CLOJURE_HASH_MAP, SCALA_HASH_MAP, SCALA_TREE_MAP,
            JAVASLANG_HASH_MAP, JAVASLANG_TREE_MAP, PCOLLECTIONS)
    var implementation = ""
    private val random = Random()

    private val distinctKeys = mutableListOf<KeyWrapper<Int>>()
    private val randomKeys = mutableListOf<KeyWrapper<Int>>()
    private val collisionKeys = mutableListOf<KeyWrapper<Int>>()

    private val anotherRandomKeys = mutableListOf<KeyWrapper<Int>>()

    private var distinctKeysMap: PMap<KeyWrapper<Int>, String> = ClojureHashPMap.empty()
    private var randomKeysMap: PMap<KeyWrapper<Int>, String> = ClojureHashPMap.empty()
    private var collisionKeysMap: PMap<KeyWrapper<Int>, String> = ClojureHashPMap.empty()

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
            distinctKeysMap = distinctKeysMap.plus(distinctKeys[index], "some element")
            randomKeysMap = randomKeysMap.plus(randomKeys[index], "some element")
            collisionKeysMap = collisionKeysMap.plus(collisionKeys[index], "some element")
        }
    }

    @Benchmark
    fun removeDistinct(): PMap<KeyWrapper<Int>, String> {
        var map = distinctKeysMap
        repeat(times = this.listSize) { index ->
            map = map.minus(distinctKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeRandom(): PMap<KeyWrapper<Int>, String> {
        var map = randomKeysMap
        repeat(times = this.listSize) { index ->
            map = map.minus(randomKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeCollision(): PMap<KeyWrapper<Int>, String> {
        var map = collisionKeysMap
        repeat(times = this.listSize) { index ->
            map = map.minus(collisionKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeNonExisting(): PMap<KeyWrapper<Int>, String> {
        var map = randomKeysMap
        repeat(times = this.listSize) { index ->
            map = map.minus(anotherRandomKeys[index])
        }
        return map
    }
}