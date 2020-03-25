import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYSeries
import org.knowm.xchart.style.Styler
import org.knowm.xchart.style.markers.SeriesMarkers
import java.io.FileInputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.math.abs
import kotlin.math.min

val cars = ArrayList<Car>()
val normalizedCars = ArrayList<NormalizedCar>()

var bias: Double = 800.0
var slope: Double = -900.0

const val LEARNING_RATE = 0.1

const val ACCEPTED_STEP_DIFF = 0.01

fun main() {
	readInput()

	normalize()

	findWeights()

//	drawChart()
}

var maxMileage: Int = 0
var minMileage: Int = 0
var maxPrice: Int = 0
var minPrice: Int = 0
var averageMileage: Int = 0

fun normalize() {
	maxMileage = cars.maxBy { it.mileage }!!.mileage
	minMileage = cars.minBy { it.mileage }!!.mileage
	maxPrice = cars.maxBy { it.price }!!.price
	minPrice = cars.minBy { it.price }!!.price

	averageMileage = cars.sumBy { it.mileage } / cars.size

	cars.forEach {
		val normalizedMileage = it.mileage / 300_000.0
//		val normalizedPrice = (it.price - minPrice).toDouble() / (maxPrice - minPrice).toDouble()
		normalizedCars.add(NormalizedCar(normalizedMileage, it.price.toDouble()))
	}
}

fun estimatePrice(mileage: Double) =
	mileage * slope + bias

fun findWeights() {
	while(true) {
		println(String.format("tuned slope: %2.2f", getTunedSlope()))
		println(String.format("tuned bias: %2.2f", getTunedBias()))
		val newSlope = getTunedSlope()
		val newBias = getTunedBias()
		if (abs(newSlope - slope) <= ACCEPTED_STEP_DIFF && abs(newBias - bias) <= ACCEPTED_STEP_DIFF) {
			bias = newBias
			slope = newSlope
			break
		}
		bias = newBias
		slope = newSlope
	}
	println("average: ${averageMileage}")
	println("estimated price")
	slope /= 300_000.0
	println(estimatePrice(240000.0))
	println("slope: $slope")
	println("bias: $bias")
}

fun getTunedSlope(): Double =
	slope - (normalizedCars.sumByDouble { (estimatePrice(it.mileage) - it.price) * it.mileage } / normalizedCars.size) * LEARNING_RATE

fun getTunedBias(): Double =
	bias - (normalizedCars.sumByDouble { estimatePrice(it.mileage) - it.price } / normalizedCars.size) * LEARNING_RATE

private fun drawChart() {
	val startX = cars.minBy { it.mileage }!!.mileage
	val endX = cars.maxBy { it.mileage }!!.mileage
	val startY = estimatePrice((startX - minMileage).toDouble() / (maxMileage - minMileage).toDouble())
	val endY = estimatePrice((endX - minMileage).toDouble() / (maxMileage - minMileage).toDouble())
	val xLineData = doubleArrayOf(startX.toDouble(), endX.toDouble())
	val yLineData = doubleArrayOf(startY, endY)

	val chart = XYChartBuilder()
			.width(600)
			.height(500)
			.title("Training Result")
			.xAxisTitle("mileage")
			.yAxisTitle("price")
			.build()

	with(chart.styler) {
		defaultSeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Scatter
//		isChartTitleVisible = false
		legendPosition = Styler.LegendPosition.InsideSW
		markerSize = 8
		xAxisDecimalPattern = ""
	}

	val carsX = cars.map { it.mileage }
	val carsY = cars.map { it.price }

	chart.addSeries("Car", carsX, carsY)

	val series = chart.addSeries("Linear regression", xLineData, yLineData)
	series.marker = SeriesMarkers.NONE
	series.xySeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Line


	SwingWrapper(chart).displayChart()
}

fun readInput() {
	val scanner = Scanner(FileInputStream("src/data.csv"))
	scanner.nextLine()
	while (scanner.hasNextLine()) {
		val values = scanner.nextLine().split(',')
		cars.add(Car(values[0].toInt(), values[1].toInt()))
	}
}

data class Car(val mileage: Int, val price: Int)
data class NormalizedCar(val mileage: Double, val price: Double)
