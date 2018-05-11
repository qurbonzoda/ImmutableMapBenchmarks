package benchmarks.paguro

import benchmarks.*
import kotlinx.collections.immutable.implementations.immutableMap.KeyWrapper
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.organicdesign.fp.collections.BaseMap
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

    @Param(PAGURO_HASH_MAP, PAGURO_TREE_MAP)
    var implementation = ""

    private val random = Random()

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
    fun getDistinct(bh: Blackhole): BaseMap<KeyWrapper<Int>, String> {
        val map = distinctKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[distinctKeys[index]])
        }
        return map
    }

    @Benchmark
    fun getRandom(bh: Blackhole): BaseMap<KeyWrapper<Int>, String> {
        val map = randomKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[randomKeys[index]])
        }
        return map
    }

    @Benchmark
    fun getCollision(bh: Blackhole): BaseMap<KeyWrapper<Int>, String> {
        val map = collisionKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[collisionKeys[index]])
        }
        return map
    }

    @Benchmark
    fun getNonExisting(bh: Blackhole): BaseMap<KeyWrapper<Int>, String> {
        val map = randomKeysMap
        repeat(times = this.listSize) { index ->
            bh.consume(map[anotherRandomKeys[index]])
        }
        return map
    }
}