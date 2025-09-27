package com.kiran.telemetrylab.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * A class responsible for performing a CPU-intensive 2D convolution.
 * This simulates a heavy computation task that would run on an edge device.
 */
@Singleton
class ConvolutionProcessor @Inject constructor() {

    private val matrixSize = 256
    private val kernelSize = 3
    private val matrix = Array(matrixSize) { FloatArray(matrixSize) { Math.random().toFloat() } }
    private val outputMatrix = Array(matrixSize) { FloatArray(matrixSize) }

    private val kernel = arrayOf(
        floatArrayOf(0f, -1f, 0f),
        floatArrayOf(-1f, 5f, -1f),
        floatArrayOf(0f, -1f, 0f)
    )

    fun performConvolution(passes: Int) {
        repeat(passes) {
            for (i in 1 until matrixSize - 1) {
                for (j in 1 until matrixSize - 1) {
                    var sum = 0f
                    for (k in 0 until kernelSize) {
                        for (l in 0 until kernelSize) {
                            sum += matrix[i - 1 + k][j - 1 + l] * kernel[k][l]
                        }
                    }
                    outputMatrix[i][j] = sum
                }
            }
        }
    }
}