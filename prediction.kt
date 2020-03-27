import com.xenomachina.argparser.*
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess

var theta0 = 0.0
var theta1 = 0.0

fun main(args: Array<String>) {
	val clArgs = parseArgs(args)!!
	theta0 = clArgs.theta0
	theta1 = clArgs.theta1
	val predictedPrice = estimatePrice(clArgs.mileage)
	printResult(predictedPrice.toInt(), clArgs.expectedPrice)
}

private fun parseArgs(args: Array<String>): PredictionCommandLineArguments? {
	try {
		return ArgParser(args, helpFormatter = DefaultHelpFormatter(HELP_PROGRAM_DESC)).parseInto(::PredictionCommandLineArguments)
	} catch (e: SystemExitException) {
		e.printAndExit()
	} catch (e: Exception) {
		invalidExit("Wrong command line arguments")
	}
	return null
}

private fun estimatePrice(mileage: Double) =
	mileage * theta1 + theta0

private fun printResult(predictedPrice: Int, expectedPrice: Int?) {
	println("Predicted price: $predictedPrice")
	if (expectedPrice != null) {
		println("Expected price: $expectedPrice")
		val precision = (min(predictedPrice, expectedPrice).toDouble() / max(predictedPrice, expectedPrice).toDouble() * 100).toInt()
		println("Precision: $precision%")
	}
}

private fun invalidExit(errorMsg: String) {
	println(errorMsg)
	exitProcess(1)
}

private const val HELP_PROGRAM_DESC = "This program is used to make prediction for car price with regard to car mileage"

private class PredictionCommandLineArguments(parser: ArgParser)  {

	val mileage by parser.storing(
		"-m", "--mileage",
		help = "mileage you want to predict price for"
	) { toDouble() }
	.addValidator {
		when {
			value < 0 -> throw InvalidArgumentException("mileage can't be less than 0")
		}
	}

	val theta0 by parser.storing(
		"--theta-0",
		help = "theta0 value"
	) { toDouble() }

	val theta1 by parser.storing(
		"--theta-1",
		help = "theta1 value"
	) { toDouble() }

	val expectedPrice by parser.storing(
		"-e", "--expected",
		help = "expected price to compare and calc prediction precision"
	) { toInt() }
	.default<Int?>(null)
	.addValidator {
		when {
			value != null && value!! < 0 -> throw InvalidArgumentException("expected price can't be less than 0")
		}
	}


}