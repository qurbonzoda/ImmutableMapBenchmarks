package benchmarks

import org.openjdk.jmh.results.RunResult
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.openjdk.jmh.runner.options.TimeValue
import java.io.FileWriter

fun main(args: Array<String>) {
    val implementation = "persistentHashMap"
    val outputFile = "teamcityArtifacts/$implementation.csv"
    val options = OptionsBuilder()
            .jvmArgs("-Xms3072m", "-Xmx3072m")
//            .include("putDistinct$")
//            .include("getRandom$")
//            .include("removeCollision$")
            .warmupIterations(10)
            .measurementIterations(10)
            .warmupTime(TimeValue.milliseconds(1000))
            .measurementTime(TimeValue.milliseconds(1000))
//            .param("listSize", BM_1000000)
            .addProfiler("gc")

    val runResults = Runner(options.build()).run()
    printResults(runResults, implementation, outputFile)
}

fun printResults(runResults: Collection<RunResult>, implementation: String, outputFile: String) {
    val csvHeader = "Implementation,Method,listSize,Score,Score Error,Allocation Rate"

    val fileWriter = FileWriter(outputFile)

    fileWriter.appendln(csvHeader)

    runResults.forEach {
        fileWriter.appendln(csvRowFrom(it, implementation))
    }

    fileWriter.flush()
    fileWriter.close()
}

fun csvRowFrom(result: RunResult, implementation: String): String {
    val nanosInMicros = 1000
    val method = result.primaryResult.getLabel()
    val listSize = result.params.getParam("listSize").toInt()
    val score = result.primaryResult.getScore() * nanosInMicros / listSize
    val scoreError = result.primaryResult.getScoreError() * nanosInMicros / listSize
    val allocationRate = result.secondaryResults["·gc.alloc.rate.norm"]!!.getScore() / listSize

    return "$implementation,$method,$listSize,%.3f,%.3f,%.3f".format(score, scoreError, allocationRate)
}