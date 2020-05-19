package com.example.la_project_test

import java.lang.StringBuilder


class Vector {
    private var values = DoubleArray(3)

    constructor() {
        for (i in 0..2)
            values[i] = 0.0
    }

    constructor(initial: FloatArray) {
        for (i in 0..2) {
            values[i] = initial[i].toDouble()
        }
    }

    constructor(x1: Double, x2: Double, x3: Double) {
        values[0] = x1
        values[1] = x2
        values[2] = x3
    }

    constructor(other: Vector) {
        values = other.values.copyOf()
    }

    operator fun get(i: Int): Double {
        return values[i]
    }

    operator fun set(i: Int, value: Double) {
        values[i] = value;
    }

    fun innerProduct(other: Vector): Double {
        var res = 0.0
        for (i in 0..2) {
            res += values[i] * other[i]
        }
        return res
    }

    fun norm(): Double {
        var res = 0.0
        for (v in values) {
            res += v * v
        }
        return kotlin.math.sqrt(res)
    }

    fun normalize() {
        val length = norm()
        for (i in 0..2) {
            values[i] = values[i] / length
        }
    }

    operator fun times(d: Double): Vector {
        val res = Vector()
        for (i in 0..2) {
            res[i] = values[i] * d
        }
        return res
    }

    operator fun plus(other: Vector): Vector {
        val v = Vector()
        for (i in 0..2) {
            v[i] = values[i] + other[i]
        }
        return v
    }

    operator fun minus(other: Vector): Vector {
        val v = Vector()
        for (i in 0..2) {
            v[i] = values[i] - other[i]
        }
        return v
    }

    fun crossProduct(other: Vector): Vector {
        val res = Vector()
        res[0] = values[1] * other[2] - values[2] * other[1]
        res[1] = values[2] * other[0] - values[0] * other[2]
        res[2] = values[0] * other[1] - values[1] * other[0]
        return res
    }

    override fun toString(): String {
        var rounded = values.map { String.format("%.3f", it) }
        return "(${rounded[0]}, ${rounded[1]}, ${rounded[2]})"
    }

}