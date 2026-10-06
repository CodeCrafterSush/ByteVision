package com.example.bytevision

import android.graphics.Bitmap
import kotlin.random.Random

private fun generateSeedFromPassword(password: String): Long {
    var hash = 0L
    for (char in password) {
        hash = 31 * hash + char.code
    }
    return hash
}


fun shuffleBitmap(srcBitmap: Bitmap, password: String): Bitmap {
    val width = srcBitmap.width
    val height = srcBitmap.height
    val totalPixels = width * height

    val pixels = IntArray(totalPixels)
    srcBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val seed = generateSeedFromPassword(password)
    val random = Random(seed)

    for (i in totalPixels - 1 downTo 1) {
        val j = random.nextInt(i + 1)
        val temp = pixels[i]
        pixels[i] = pixels[j]
        pixels[j] = temp
    }

    val shuffledBitmap = Bitmap.createBitmap(width, height, srcBitmap.config ?: Bitmap.Config.ARGB_8888)
    shuffledBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

    return shuffledBitmap
}


fun unshuffleBitmap(shuffledBitmap: Bitmap, password: String): Bitmap {
    val width = shuffledBitmap.width
    val height = shuffledBitmap.height
    val totalPixels = width * height

    val pixels = IntArray(totalPixels)
    shuffledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val seed = generateSeedFromPassword(password)
    val random = Random(seed)

    val swapIndices = IntArray(totalPixels)
    for (i in totalPixels - 1 downTo 1) {
        swapIndices[i] = random.nextInt(i + 1)
    }

    for (i in 1 until totalPixels) {
        val j = swapIndices[i]
        val temp = pixels[i]
        pixels[i] = pixels[j]
        pixels[j] = temp
    }

    val restoredBitmap = Bitmap.createBitmap(width, height, shuffledBitmap.config ?: Bitmap.Config.ARGB_8888)
    restoredBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

    return restoredBitmap
}