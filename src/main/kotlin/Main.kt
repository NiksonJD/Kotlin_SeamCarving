package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.sqrt

fun removeSeam(image: BufferedImage): BufferedImage {
    val (w, h) = listOf(image.width, image.height)
    val energy = Array(h) { DoubleArray(w) }
    val cost = Array(h) { DoubleArray(w) { Double.POSITIVE_INFINITY } }
    val prev = Array(h) { IntArray(w) }

    for (x in 0 until w) {
        for (y in 0 until h) {
            energy[y][x] = sqrt(
                (gradient(Color(image.getRGB(mx(x, w) - 1, y)), Color(image.getRGB(mx(x, w) + 1, y))) +
                        gradient(Color(image.getRGB(x, mx(y, h) - 1)), Color(image.getRGB(x, mx(y, h) + 1)))).toDouble()
            )
        }
    }

    energy[0].forEachIndexed { x, e -> cost[0][x] = e }

    for (y in 1 until h) {
        for (x in 0 until w) {
            val left = if (x > 0) cost[y - 1][x - 1] else Double.POSITIVE_INFINITY
            val middle = cost[y - 1][x]
            val right = if (x < w - 1) cost[y - 1][x + 1] else Double.POSITIVE_INFINITY
            val mapCost = mapOf(x - 1 to left, x to middle, x + 1 to right)
            prev[y][x] = mapCost.minByOrNull { it.value }?.key!!
            cost[y][x] = energy[y][x] + mapCost.entries.minOf { it.value }
        }
    }

    var currentX = cost.last().withIndex().minByOrNull { it.value }?.index!!
    val seam = MutableList(h) { currentX }

    ((h - 1) downTo 0).forEach { y -> seam[y] = currentX; currentX = prev[y][currentX] }

    return BufferedImage(w - 1, h, image.type).apply {
        (0 until h).forEach { y ->
            (0 until seam[y]).forEach { x -> setRGB(x, y, image.getRGB(x, y)) }
            (seam[y] + 1 until w).forEach { x -> setRGB(x - 1, y, image.getRGB(x, y)) }
        }
    }
}

fun mx(s: Int, t: Int) = when (s) {
    0 -> 1
    t - 1 -> t - 2
    else -> s
}

fun gradient(c1: Color, c2: Color): Int {
    val r = c1.red.minus(c2.red)
    val g = c1.green.minus(c2.green)
    val b = c1.blue.minus(c2.blue)
    return r * r + g * g + b * b
}

fun rotateImage(image: BufferedImage) = BufferedImage(image.height, image.width, image.type).apply {
    (0 until image.width).forEach { x -> (0 until image.height).forEach { y -> setRGB(y, x, image.getRGB(x, y)) } }
}

fun main(args: Array<String>) {
    var image = ImageIO.read(File(args[1]))
    repeat(args[5].toInt()) { image = removeSeam(image) }
    image = rotateImage(image)
    repeat(args[7].toInt()) { image = removeSeam(image) }
    image = rotateImage(image)
    ImageIO.write(image, "png", File(args[3]))
}