package benchmarks.paguro

import benchmarks.*
import kotlinx.collections.immutable.implementations.immutableMap.KeyWrapper
import org.openjdk.jmh.annotations.*
import org.organicdesign.fp.collections.BaseMap
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

    @Param(PAGURO_HASH_MAP, PAGURO_TREE_MAP)
    var implementation = ""

    private val random = Random(40)

    private val distinctKeys = mutableListOf<KeyWrapper<Int>>()
    private val randomKeys = mutableListOf<KeyWrapper<Int>>()
    private val collisionKeys = mutableListOf<KeyWrapper<Int>>()

    private val anotherRandomKeys = mutableListOf<KeyWrapper<Int>>()

    private var distinctKeysMap: BaseMap<KeyWrapper<Int>, String> = emptyPMap(PAGURO_HASH_MAP)
    private var randomKeysMap: BaseMap<KeyWrapper<Int>, String> = emptyPMap(PAGURO_HASH_MAP)
    private var collisionKeysMap: BaseMap<KeyWrapper<Int>, String> = emptyPMap(PAGURO_HASH_MAP)

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
            distinctKeysMap = distinctKeysMap.assoc(distinctKeys[index], "some element")
            randomKeysMap = randomKeysMap.assoc(randomKeys[index], "some element")
            collisionKeysMap = collisionKeysMap.assoc(collisionKeys[index], "some element")
        }
    }

    @Benchmark
    fun removeDistinct(): BaseMap<KeyWrapper<Int>, String> {
        var map = distinctKeysMap
        repeat(times = this.listSize) { index ->
            map = map.without(distinctKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeRandom(): BaseMap<KeyWrapper<Int>, String> {
        var map = randomKeysMap
        repeat(times = this.listSize) { index ->
            map = map.without(randomKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeCollision(): BaseMap<KeyWrapper<Int>, String> {
        var map = collisionKeysMap
        repeat(times = this.listSize) { index ->
            map = map.without(collisionKeys[index])
        }
        return map
    }

    @Benchmark
    fun removeNonExisting(): BaseMap<KeyWrapper<Int>, String> {
        var map = randomKeysMap
        repeat(times = this.listSize) { index ->
            map = map.without(anotherRandomKeys[index])
        }
        return map
    }
}