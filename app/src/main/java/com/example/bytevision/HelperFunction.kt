package com.example.bytevision

import android.graphics.Bitmap
import kotlin.random.Random





import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// ==========================================
// AES-256 ENCRYPTION / DECRYPTION HELPERS
// ==========================================

private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
    val tmp = factory.generateSecret(spec)
    return SecretKeySpec(tmp.encoded, "AES")
}

private fun encryptText(text: String, password: String): ByteArray {
    val salt = "ByteVisionSalt123".toByteArray()
    val key = deriveKey(password, salt)
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, key)
    return cipher.doFinal(text.toByteArray(Charsets.UTF_8))
}

private fun decryptText(encryptedBytes: ByteArray, password: String): String {
    val salt = "ByteVisionSalt123".toByteArray()
    val key = deriveKey(password, salt)
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, key)
    return String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
}

// ==========================================
// 1. ENCODE (HIDE MESSAGE) FUNCTION
// ==========================================

fun encodeMessageToImage(
    context: Context,
    imageUri: Uri,
    message: String,
    password: String
): Bitmap? {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        val bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        inputStream?.close()

        val encryptedData = encryptText(message, password)

        val payload = ByteBuffer.allocate(4 + encryptedData.size)
            .putInt(encryptedData.size)
            .put(encryptedData)
            .array()

        val totalBits = payload.size * 8
        val width = bitmap.width
        val height = bitmap.height

        if (totalBits > width * height * 3) {
            return null // Image message hold karne ke liye choti hai
        }

        var bitIndex = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (bitIndex >= totalBits) break

                val pixel = bitmap.getPixel(x, y)
                var red = (pixel shr 16) and 0xFF
                var green = (pixel shr 8) and 0xFF
                var blue = pixel and 0xFF

                if (bitIndex < totalBits) {
                    val bit = (payload[bitIndex / 8].toInt() shr (7 - (bitIndex % 8))) and 1
                    red = (red and 0xFE) or bit
                    bitIndex++
                }

                if (bitIndex < totalBits) {
                    val bit = (payload[bitIndex / 8].toInt() shr (7 - (bitIndex % 8))) and 1
                    green = (green and 0xFE) or bit
                    bitIndex++
                }

                if (bitIndex < totalBits) {
                    val bit = (payload[bitIndex / 8].toInt() shr (7 - (bitIndex % 8))) and 1
                    blue = (blue and 0xFE) or bit
                    bitIndex++
                }

                val updatedPixel = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
                bitmap.setPixel(x, y, updatedPixel)
            }
            if (bitIndex >= totalBits) break
        }

        return bitmap

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

// ==========================================
// 2. DECODE (EXTRACT MESSAGE) FUNCTION
// ==========================================

fun decodeMessageFromImage(
    context: Context,
    imageUri: Uri,
    password: String
): String? {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream?.close()

        val width = bitmap.width
        val height = bitmap.height

        var bitIndex = 0
        var lengthBuffer = 0
        val lengthBitsToRead = 32

        // Step A: Header se encrypted data ki length read karo
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (bitIndex >= lengthBitsToRead) break

                val pixel = bitmap.getPixel(x, y)
                val channels = intArrayOf(
                    (pixel shr 16) and 0xFF,
                    (pixel shr 8) and 0xFF,
                    pixel and 0xFF
                )

                for (channel in channels) {
                    if (bitIndex < lengthBitsToRead) {
                        val bit = channel and 1
                        lengthBuffer = (lengthBuffer shl 1) or bit
                        bitIndex++
                    }
                }
            }
            if (bitIndex >= lengthBitsToRead) break
        }

        val dataSize = lengthBuffer
        if (dataSize <= 0 || dataSize > (width * height * 3) / 8) {
            return null
        }

        val totalDataBits = dataSize * 8
        val encryptedBytes = ByteArray(dataSize)
        var dataBitIndex = 0

        val totalBitsToRead = 32 + totalDataBits
        var currentBitCounter = 0

        // Step B: Encrypted payload bytes extract karo
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (currentBitCounter >= totalBitsToRead) break

                val pixel = bitmap.getPixel(x, y)
                val channels = intArrayOf(
                    (pixel shr 16) and 0xFF,
                    (pixel shr 8) and 0xFF,
                    pixel and 0xFF
                )

                for (channel in channels) {
                    if (currentBitCounter >= 32 && currentBitCounter < totalBitsToRead) {
                        val bit = channel and 1
                        val byteIdx = dataBitIndex / 8
                        val bitInByteShift = 7 - (dataBitIndex % 8)
                        encryptedBytes[byteIdx] = (encryptedBytes[byteIdx].toInt() or (bit shl bitInByteShift)).toByte()
                        dataBitIndex++
                    }
                    currentBitCounter++
                }
            }
            if (currentBitCounter >= totalBitsToRead) break
        }

        return decryptText(encryptedBytes, password)

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}














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