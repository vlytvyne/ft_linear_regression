import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYSeries
import org.knowm.xchart.style.Styler
import org.knowm.xchart.style.markers.SeriesMarkers
import java.io.FileInputStream
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.system.exitProcess

const val LEARNING_RATE = 0.1
const val ACCEPTABLE_LEARNING_STEP_DIFF = 0.01

data class Car(val mileage: Double, val price: Int)

val inputCars = ArrayList<Car>()
val reducedMileageCars = ArrayList<Car>()
var averageMileage: Double = 0.0

var bias: Double = 0.0
var slope: Double = 0.0

fun main() {
	readInput(FileInputStream("src/data.csv"))
	averageMileage = inputCars.sumByDouble { it.mileage } / inputCars.size
	reduceMileage()
	calculateWeights()
	drawChart()
}

fun readInput(inputStream: InputStream) {
	val scanner = Scanner(inputStream)
	val csvTitle = scanner.nextLine()
	val values = csvTitle.split(',')
	if (values[0] != "km" || values[1] != "price") {
		invalidExit("Wrong csv format")
	}
	try {
		while (scanner.hasNextLine()) {
			val values = scanner.nextLine().split(',')
			inputCars.add(Car(values[0].toDouble(), values[1].toInt()))
		}
	} catch (e: Exception) {
		invalidExit("Wrong csv format")
	}
}

fun reduceMileage() {
	inputCars.forEach {
		val reducedMileage = it.mileage / averageMileage
		reducedMileageCars.add(Car(reducedMileage, it.price))
	}
}

fun calculateWeights() {
	while(true) {
		val newSlope = getSlopeAfterStep()
		val newBias = getBiasAfterStep()
		if (isConvergenceAchieved(newSlope, newBias)) {
			break
		}
		bias = newBias
		slope = newSlope
	}
	slope /= averageMileage
	println("estimated price")
	println(estimatePrice(74000.0))
	println("slope: $slope")
	println("bias: $bias")
}

private fun isConvergenceAchieved(newSlope: Double, newBias: Double) =
	abs(newSlope - slope) <= ACCEPTABLE_LEARNING_STEP_DIFF && abs(newBias - bias) <= ACCEPTABLE_LEARNING_STEP_DIFF

fun getSlopeAfterStep(): Double =
	slope - (reducedMileageCars.sumByDouble { (estimatePrice(it.mileage) - it.price) * it.mileage } / reducedMileageCars.size) * LEARNING_RATE

fun getBiasAfterStep(): Double =
	bias - (reducedMileageCars.sumByDouble { estimatePrice(it.mileage) - it.price } / reducedMileageCars.size) * LEARNING_RATE

fun estimatePrice(mileage: Double) =
	mileage * slope + bias

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