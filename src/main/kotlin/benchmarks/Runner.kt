package benchmarks

import org.openjdk.jmh.results.RunResult
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.openjdk.jmh.runner.options.TimeValue
import java.io.FileWriter

fun main(args: Array<String>) {
    val millisInSecond = 1000L
    val secondsInMinute = 60L
    val tenMinutes = 5 * secondsInMinute * millisInSecond
    Thread.sleep(tenMinutes)

    val outputFile = "teamcityArtifacts/persistentHashMap.csv"
    val options = OptionsBuilder()
            .jvmArgs("-Xms3072m", "-Xmx3072m")
//            .include(".putDistinct$")
            .warmupIterations(10)
            .measurementIterations(10)
            .warmupTime(TimeValue.milliseconds(2000))
            .measurementTime(TimeValue.milliseconds(2000))
            .param("implementation", *args)
            .param("listSize", BM_100000, BM_1000000)
            .addProfiler("gc")

    val runResults = Runner(options.build()).run()
    printResults(runResults, outputFile)
}

fun printResults(runResults: Collection<RunResult>, outputFile: String) {
    val csvHeader = "Implementation,Method,listSize,Score,Score Error,Allocation Rate"

    val fileWriter = FileWriter(outputFile)

    fileWriter.appendln(csvHeader)

    runResults.forEach {
        fileWriter.appendln(csvRowFrom(it))
    }

    fileWriter.flush()
    fileWriter.close()
}

fun csvRowFrom(result: RunResult): String {
    val nanosInMicros = 1000
    val method = result.primaryResult.getLabel()
    val listSize = result.params.getParam("listSize").toInt()
    val score = result.primaryResult.getScore() * nanosInMicros / listSize
    val scoreError = result.primaryResult.getScoreError() * nanosInMicros / listSize
    val allocationRate = result.secondaryResults["·gc.alloc.rate.norm"]!!.getScore() / listSize

    val implementation = result.params.getParam("implementation")
    return "$implementation,$method,$listSize,%.3f,%.3f,%.3f".format(score, scoreError, allocationRate)
}