import com.xenomachina.argparser.*
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYSeries
import org.knowm.xchart.style.Styler
import org.knowm.xchart.style.markers.SeriesMarkers
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.system.exitProcess

const val DEFAULT_LEARNING_RATE = 0.1
const val DEFAULT_ACCEPTABLE_LEARNING_STEP_DIFF = 0.01

data class Car(val mileage: Double, val price: Int)

val inputCars = ArrayList<Car>()
val reducedMileageCars = ArrayList<Car>()
var averageMileage: Double = 0.0

var learningRate = DEFAULT_LEARNING_RATE
var acceptableLearningStepDiff = DEFAULT_ACCEPTABLE_LEARNING_STEP_DIFF

var bias: Double = 0.0
var slope: Double = 0.0

//https://medium.com/@lachlanmiller_52885/machine-learning-week-1-cost-function-gradient-descent-and-univariate-linear-regression-8f5fe69815fd
fun main(args: Array<String>) {
	val clArgs = parseArgs(args)
	learningRate = clArgs.learningRage
	acceptableLearningStepDiff = clArgs.acceptableLearningStepDiff
	processInput(clArgs.inputFileName)
	averageMileage = inputCars.sumByDouble { it.mileage } / inputCars.size
	reduceMileage()
	calculateWeights(clArgs.verbose)
	printResult()
	if (clArgs.showChart) { drawChart() }
}

fun parseArgs(args: Array<String>): CommandLineArguments {
	return try {
		ArgParser(args).parseInto(::CommandLineArguments)
	} catch (e: SystemExitException) {
		e.printAndExit()
	}
}

fun processInput(inputFileName: String?) {
	if (inputFileName == null) {
		readInput(System.`in`)
	} else {
		try {
			FileInputStream(inputFileName).use(::readInput)
		} catch (e: IOException) {
			invalidExit("Can't open file or file doesn't exist")
		}
	}
}

fun readInput(inputStream: InputStream) {
	val scanner = Scanner(inputStream)
	processCSVHeader(scanner)
	try {
		while (scanner.hasNextLine()) {
			val values = scanner.nextLine().split(',')
			inputCars.add(Car(values[0].toDouble(), values[1].toInt()))
		}
	} catch (e: Exception) {
		invalidExit("Wrong csv format")
	}
}

private fun processCSVHeader(scanner: Scanner) {
	if (scanner.hasNextLine()) {
		val csvHeader = scanner.nextLine()
		val csvTitles = csvHeader.split(',')
		if (csvTitles[0] != "km" || csvTitles[1] != "price") {
			invalidExit("Wrong csv format")
		}
	} else {
		invalidExit("Input is empty")
	}
}

fun reduceMileage() {
	inputCars.forEach {
		val reducedMileage = it.mileage / averageMileage
		reducedMileageCars.add(Car(reducedMileage, it.price))
	}
}

fun calculateWeights(printProgress: Boolean) {
	var iteration = 0
	while(true) {
		if (printProgress) {
			printIterationResult(iteration)
		}
		val newSlope = getSlopeAfterStep()
		val newBias = getBiasAfterStep()
		if (isConvergenceAchieved(newSlope, newBias)) {
			break
		}
		bias = newBias
		slope = newSlope
		iteration++
	}
	slope /= averageMileage
}

private fun printIterationResult(iteration: Int) {
	println("ITERATION: $iteration")
	println("Theta0 (bias): " + String.format("%.6f", bias))
	println("Theta1 (slope): " + String.format("%.6f", slope / averageMileage))
	println()
}

private fun isConvergenceAchieved(newSlope: Double, newBias: Double) =
	abs(newSlope - slope) <= acceptableLearningStepDiff && abs(newBias - bias) <= acceptableLearningStepDiff

fun getSlopeAfterStep(): Double =
	slope - (reducedMileageCars.sumByDouble { (estimatePrice(it.mileage) - it.price) * it.mileage } / reducedMileageCars.size) * learningRate

fun getBiasAfterStep(): Double =
	bias - (reducedMileageCars.sumByDouble { estimatePrice(it.mileage) - it.price } / reducedMileageCars.size) * learningRate

fun estimatePrice(mileage: Double) =
	mileage * slope + bias

fun printResult() {
	println("Theta0=$bias")
	println("Theta1=$slope")
}

fun drawChart() {
	val chart = createChart()
	addCarMarkersOnChart(chart)
	addLinearRegLineOnChart(chart)
	SwingWrapper(chart).displayChart()
}

fun createChart(): XYChart {
	val chart = XYChartBuilder()
		.width(600)
		.height(500)
		.title("Training Result")
		.xAxisTitle("mileage")
		.yAxisTitle("price")
		.build()

	with(chart.styler) {
		defaultSeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Scatter
		legendPosition = Styler.LegendPosition.InsideSW
		markerSize = 8
		xAxisDecimalPattern = ""
		yAxisDecimalPattern = ""
	}

	return chart
}

fun addCarMarkersOnChart(chart: XYChart) {
	val carsX = inputCars.map { it.mileage }
	val carsY = inputCars.map { it.price }

	chart.addSeries("Car", carsX, carsY)
}

fun addLinearRegLineOnChart(chart: XYChart) {
	val xLinearRegStart = inputCars.minBy { it.mileage }!!.mileage
	val xLinearRegEnd = inputCars.maxBy { it.mileage }!!.mileage
	val yLinearRegStart = estimatePrice(xLinearRegStart)
	val yLinearRegEnd = estimatePrice(xLinearRegEnd)
	val xLinearRegData = doubleArrayOf(xLinearRegStart, xLinearRegEnd)
	val yLinearRegData = doubleArrayOf(yLinearRegStart, yLinearRegEnd)

	val series = chart.addSeries("Linear regression", xLinearRegData, yLinearRegData)
	series.marker = SeriesMarkers.NONE
	series.xySeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Line
}

fun invalidExit(errorMsg: String) {
	println(errorMsg)
	exitProcess(1)
}

class CommandLineArguments(parser: ArgParser) {

	val verbose by parser.flagging(
		"-v", "--verbose",
		help = "show verbose output of training progress"
	)

	val showChart by parser.flagging(
		"-c", "--chart",
		help = "show chart of linear regression built"
	)

	val inputFileName by parser.storing(
		"-f", "--file",
		help = "set file as input stream (stdin by default)"
	).default<String?>(null)

	val learningRage by parser.storing(
		"-l", "--learning-rate",
		help = "set learning rate (0.1 by default)"
	) { toDouble() }
		.default(DEFAULT_LEARNING_RATE)
		.addValidator {
			when {
				value < 0 -> throw InvalidArgumentException("Learning rate can't be negative")
				value < 0.0001 -> throw InvalidArgumentException("Learning rate is too small (min is 0.0001)")
				value > 0.5 -> throw InvalidArgumentException("Learning rate is too large (max is 0.5)")
			}
		}

	val acceptableLearningStepDiff by parser.storing(
		"-d", "--step-diff",
		help = "set acceptable learning step diff (0.01 by default)"
	) { toDouble() }
		.default(DEFAULT_ACCEPTABLE_LEARNING_STEP_DIFF)
		.addValidator {
			when {
				value < 0 -> throw InvalidArgumentException("Step diff can't be negative")
				value < 0.0001 -> throw InvalidArgumentException("Step diff is too small (min is 0.0001)")
				value > 1.0 -> throw InvalidArgumentException("Step diff is too large (max is 1)")
			}
		}
}