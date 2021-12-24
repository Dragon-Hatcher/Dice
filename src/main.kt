import com.github.sarxos.webcam.Webcam
import java.awt.Color
import java.awt.Dimension
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min
import javax.imageio.ImageIO
import javax.swing.JFrame
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


data class Point(val x: Int, val y: Int) {
    fun distTo(other: Point) = sqrt(((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y)).toDouble())
}

fun timeIt(it: () -> Unit) {
    val startTime = System.currentTimeMillis()
    it()
    println(System.currentTimeMillis() - startTime)
}

fun resize(img: BufferedImage, newW: Int, newH: Int): BufferedImage {
    val tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH)
    val dimg = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
    val g2d = dimg.createGraphics()
    g2d.drawImage(tmp, 0, 0, null)
    g2d.dispose()
    return dimg
}

fun main() {
    val webcam = Webcam.getDefault()
//    webcam.customViewSizes = arrayOf(Dimension(1920, 1080))
//    webcam.viewSize = webcam.viewSizes.last()
    while (true) {
        webcam.open()
//        val image = ImageIO.read(File("C:\\Users\\danie\\Pictures\\Camera Roll\\Dice3.jpg"))
        val image = resize(webcam.image, 1920, 1080)

        val dice = getDice(image, 5) ?: return
        println(dice)
        val solution = solve(1045.38, dice)
        println(solution.first)
        Thread.sleep(3000)
    }
}

enum class Operator {
    PLUS, MINUS, MULTIPLY, DIVIDE, POWER;

    override fun toString() = when (this) {
        PLUS -> "+"
        MINUS -> "-"
        MULTIPLY -> "*"
        DIVIDE -> "/"
        POWER -> "^"
    }
}

data class Op(val num1: Double, val num2: Double, val operator: Operator) {
    override fun toString() = "$num1 $operator $num2 = $value"

    val value: Double
        get() = when (operator) {
            Operator.PLUS -> num1 + num2
            Operator.MINUS -> num1 - num2
            Operator.MULTIPLY -> num1 * num2
            Operator.DIVIDE -> num1 / num2
            Operator.POWER -> num1.pow(num2)
        }
}

data class Method(val ops: List<Op>) {
    fun add(op: Op) = Method(ops.plus(op))

    override fun toString() = ops.joinToString(separator = "\n")
}

fun solve(goal: Double, allNumbers: List<Int>): Pair<Method, Double> {
    val answers = mutableMapOf<Method, Double>()
    var found = false

    fun subSolve(method: Method, numbers: List<Double>) {
        if (numbers.size == 1) {
            answers[method] = numbers.first()
            if (numbers.first() == goal) found = true
            return
        }

        for (num1Index in numbers.indices) {
            for (num2Index in (num1Index + 1) until numbers.size) {
                val newList = numbers.toMutableList()
                val num2 = newList.removeAt(num2Index)
                val num1 = newList.removeAt(num1Index)

                newList.add(num1 + num2)
                subSolve(method.add(Op(num1, num2, Operator.PLUS)), newList)
                newList.removeLast()
                if (found) return

                newList.add(num1 - num2)
                subSolve(method.add(Op(num1, num2, Operator.MINUS)), newList)
                newList.removeLast()
                if (found) return

                newList.add(num2 - num1)
                subSolve(method.add(Op(num2, num1, Operator.MINUS)), newList)
                newList.removeLast()
                if (found) return

                newList.add(num1 * num2)
                subSolve(method.add(Op(num1, num2, Operator.MULTIPLY)), newList)
                newList.removeLast()
                if (found) return

                newList.add(num1 / num2)
                subSolve(method.add(Op(num1, num2, Operator.DIVIDE)), newList)
                newList.removeLast()
                if (found) return

                newList.add(num2 / num1)
                subSolve(method.add(Op(num2, num1, Operator.DIVIDE)), newList)
                newList.removeLast()
                if (found) return

                newList.add(num1.pow(num2))
                subSolve(method.add(Op(num1, num2, Operator.POWER)), newList)
                newList.removeLast()
                if (found) return

                newList.add(num2.pow(num1))
                subSolve(method.add(Op(num2, num1, Operator.POWER)), newList)
                newList.removeLast()
                if (found) return
            }
        }
    }

    subSolve(Method(listOf()), allNumbers.map { it.toDouble() })

    var closest = answers.entries.first()
    answers.entries.forEach {
        if (abs(goal - it.value) < abs(goal - closest.value)) closest = it
    }
    return Pair(closest.key, closest.value)
}

val frame = JFrame().also {
    it.isVisible = true
}

fun getDice(image: BufferedImage, diceCount: Int): List<Int>? {
    val blackAndWhite = changeToOneBitColor(image)

    val jPicture = JPicture(blackAndWhite)
    frame.size = Dimension(image.width, image.height)
    frame.contentPane = jPicture
    frame.revalidate()

    val centers = mutableListOf<Point>()
    for (y: Int in 0 until image.height) {
        for (x: Int in 0 until image.width) {
            val r = circleHere(blackAndWhite, x, y)
            if (r != null) {
                centers.add(Point(x, y))
                jPicture.drawCircle(x, y, r, Color.RED)
            }
        }
    }

    if (centers.size !in diceCount..(diceCount * 6)) {
        return null
    }
    return diceNums(jPicture, centers, diceCount)
}

val colors = listOf(
    Color.CYAN,
    Color.GREEN,
    Color.BLUE,
    Color.MAGENTA,
    Color.ORANGE
)

fun diceNums(image: JPicture, pipsIn: List<Point>, diceCountIn: Int): List<Int>? {
    println(pipsIn.size)

    fun iter(pips: List<Point>, diceCount: Int): Triple<Double, List<Int>, List<Int>> {
        val assignments = MutableList(pips.size) { (0 until diceCount).random() }
        val centers = MutableList(diceCount) { Point(0, 0) }
        var score = 0.0

        fun recalculateCenters(): Boolean {
            var change = false
            for (c in 0 until diceCount) {
                var xTot = 0
                var yTot = 0
                var pipCount = 0
                for ((i, group) in assignments.withIndex()) {
                    if (group == c) {
                        xTot += pips[i].x
                        yTot += pips[i].y
                        pipCount++
                    }
                }
                val newCenter = when (pipCount) {
                    0 -> Point(0, 0)
                    else -> Point(xTot / pipCount, yTot / pipCount)
                }
                change = change || centers[c] != newCenter
                centers[c] = newCenter
            }
            return change
        }

        fun reassignPoints() {
            score = 0.0
            for ((i, p) in pips.withIndex()) {
                val distances = centers.map { p.distTo(it) }
                val min = distances.minOf { it }
                val center = distances.indexOf(min)
                score += min * min
                assignments[i] = center
            }
        }

        while (recalculateCenters()) {
            reassignPoints()
        }

        val groupCounts = MutableList(diceCount) { 0 }
        assignments.forEach { groupCounts[it]++ }

        return Triple(score, groupCounts, assignments)
    }

    var result = iter(pipsIn, diceCountIn)
    repeat(10) {
        val newResult = iter(pipsIn, diceCountIn)
        if (newResult.first < result.first && newResult.second.all { it in 1..6 }) result = newResult
    }
    if (result.second.all { it in 1..6 }) {
        result.third.withIndex().forEach {
            image.drawCircle(pipsIn[it.index].x, pipsIn[it.index].y, 5, colors[it.value])
        }
        return result.second
    } else {
        return null
    }
}

val BLACK = Color.BLACK.rgb
val WHITE = Color.WHITE.rgb

fun circleHere(image: BufferedImage, x: Int, y: Int, debug: Boolean = false): Int? {
    if (image.getRGB(x, y) == WHITE) return null

    var xNeg = 0
    while (x - xNeg >= 0 && image.getRGB(x - xNeg, y) == BLACK) xNeg++
    var xPos = 0
    while (x + xPos < image.width && image.getRGB(x + xPos, y) == BLACK) xPos++
    var yNeg = 0
    while (y - yNeg >= 0 && image.getRGB(x, y - yNeg) == BLACK) yNeg++
    var yPos = 0
    while (y + yPos < image.height && image.getRGB(x, y + yPos) == BLACK) yPos++

    if (debug) println("$xNeg, $xPos, $yNeg, $yPos")
    val min = min(min(xNeg, xPos), min(yNeg, yPos))
    val radius = max(max(xNeg, xPos), max(yNeg, yPos))
    if (min <= 10 || radius >= 30) return null
    val searchR = (radius * 1.5).toInt()

    var matchCount = 0
    var matchTotal = 0
    for (sx: Int in (x - searchR)..(x + searchR)) {
        for (sy: Int in (y - searchR)..(y + searchR)) {
            if (sx >= 0 && sx < image.width && sy >= 0 && sy < image.height) {
                val distance = sqrt(((sx - x) * (sx - x) + (sy - y) * (sy - y)).toDouble())
                val correctColor = if (distance <= radius) BLACK else WHITE
                if (correctColor == WHITE) {
                    matchTotal += 8
                    if (image.getRGB(sx, sy) == correctColor) {
                        matchCount += 8
                    }
                } else {
                    matchTotal++
                    if (image.getRGB(sx, sy) == correctColor) {
                        matchCount++
                    }
                }
            }
        }
    }

    val threshold = 0.98
    val correctPercent = matchCount.toDouble() / matchTotal.toDouble()
    if (debug) println(correctPercent)

    return if (correctPercent > threshold) radius else null
}

fun changeToOneBitColor(image: BufferedImage): BufferedImage {
    val newImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
    for (x: Int in 0 until image.width) {
        for (y: Int in 0 until image.height) {
            val color = Color(image.getRGB(x, y))
//            val redGreaterThanGreen = (color.red - color.green) in -15..40
//            val greenGreaterThanBlue = (color.green - color.blue) in -15..30
//            val max = max(color.red, max(color.green, color.blue))
//            val isBlackEnough = redGreaterThanGreen && greenGreaterThanBlue && max < 80
            val isBlackEnough =
                (color.red < 80 && color.green < 80) || (color.red < 80 && color.blue < 80) || (color.green < 80 && color.green < 80)
            newImage.setRGB(x, y, if (isBlackEnough) BLACK else WHITE)
        }
    }
    return newImage
}