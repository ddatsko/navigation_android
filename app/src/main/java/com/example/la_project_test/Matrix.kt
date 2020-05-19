package com.example.la_project_test

class Matrix() {
    companion object {
        val minors = arrayListOf(
            intArrayOf(4, 8, 5, 7),
            intArrayOf(3, 8, 5, 6),
            intArrayOf(3, 7, 4, 6),
            intArrayOf(1, 8, 2, 7),
            intArrayOf(0, 8, 2, 6),
            intArrayOf(0, 7, 1, 6),
            intArrayOf(1, 5, 2, 4),
            intArrayOf(0, 5, 2, 3),
            intArrayOf(0, 4, 1, 3)
        )
        val swaps = arrayListOf(
            Pair(1, 3),
            Pair(2, 6),
            Pair(5, 7)
        )

        fun identity(): Matrix {
            return Matrix(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0))
        }
    }

    var values = DoubleArray(9)

    init {
        for (i in 0..8)
            values[i] = 0.0
    }

    constructor(initial: DoubleArray) : this() {
        values = initial.copyOf()
    }

    constructor(col1: Vector, col2: Vector, col3: Vector): this() {
        for (i in 0..2) {
            values[i * 3] = col1[i]
            values[i * 3 + 1] = col2[i]
            values[i * 3 + 2] = col3[i]
        }
    }

    private fun columns(): ArrayList<Vector> {
        val res = ArrayList<Vector>(3)
        for (i in 0..2) {
            res.add(Vector(values[i], values[i + 3], values[i + 6]))
        }
        return res
    }

    private fun rows(): ArrayList<Vector> {
        val res = ArrayList<Vector>(3)
        for (i in 0..2) {
            res.add(Vector(values[i * 3], values[i * 3 + 1], values[i * 3 + 2]))
        }
        return res
    }

    operator fun times(v: Vector): Vector {
        var res = Vector()
        val cols = columns()
        for (i in 0..2) {
            res += cols[i] * v[i]
        }
        return res
    }

    operator fun times(d: Double): Matrix {
        var newValues = DoubleArray(9)
        for (i in 0..8) {
            newValues[i] = values[i] * d
        }
        return Matrix(newValues)
    }

    operator fun set(x: Int, y: Int, value: Double) {
        values[x * 3 + y] = value
    }

    operator fun set(index: Int, value: Double) {
        values[index] = value
    }

    operator fun get(index: Int): Double {
        return values[index]
    }

    private fun mul(vararg indexes: Int): Double {
        var res = 1.0
        for (i in indexes) {
            res *= values[i]
        }
        return res
    }

    operator fun times(other: Matrix): Matrix {
        val res = Matrix()
        val rows = rows()
        val cols = other.columns()
        for (i in 0..2) {
            for (j in 0..2) {
                res[i, j] = rows[i].innerProduct(cols[j])
            }
        }
        return res
    }


    fun transpose(): Matrix {
        var newM = Matrix(values)
        var tmp: Double
        for (swapPair in swaps) {
            tmp = newM[swapPair.first]
            newM[swapPair.first] = newM[swapPair.second]
            newM[swapPair.second] = tmp
        }
        return newM
    }

    fun det(): Double {
        val plus = mul(0, 4, 8) + mul(1, 5, 6) + mul(3, 7, 2)
        val minus = mul(2, 4, 6) + mul(1, 3, 8) + mul(0, 5, 7)
        return plus - minus
    }


    fun inverse(): Matrix {
        var minorsMatrix = Matrix()
        for (i in 0..8) {
            minorsMatrix[i] =
                values[minors[i][0]] * values[minors[i][1]] - values[minors[i][2]] * values[minors[i][3]]
            if (i % 2 == 1)
                minorsMatrix[i] *= -1.0
        }
        return minorsMatrix.transpose() * (1 / det())
    }

    override fun toString(): String {
        var builder = StringBuilder()
        for (i in 0..2) {
            for (j in 0..2) {
                builder.append(String.format("%.3f", values[i * 3 + j]))
                if (j < 2)
                    builder.append(", ")
            }
            builder.append("\n")
        }
        return builder.toString()
    }

}

