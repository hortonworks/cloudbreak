package com.sequenceiq.cloudbreak.cloud.model

import java.lang.Character.isDigit
import java.lang.Character.isSpaceChar

import java.util.Comparator

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType

class StringTypesCompare : Comparator<StringType> {

    internal fun compareRight(a: String, b: String): Int {
        var bias = 0
        var ia = 0
        var ib = 0
        while (true) {
            val ca = charAt(a, ia)
            val cb = charAt(b, ib)
            if (!isDigit(ca) && !isDigit(cb)) {
                return bias
            } else if (!isDigit(ca)) {
                return -1
            } else if (!isDigit(cb)) {
                return +1
            } else if (ca < cb) {
                if (bias == 0) {
                    bias = -1
                }
            } else if (ca > cb) {
                if (bias == 0) {
                    bias = +1
                }
            } else if (ca.toInt() == 0 && cb.toInt() == 0) {
                return bias
            }
            ia++
            ib++
        }
    }

    override fun compare(o1: StringType, o2: StringType): Int {
        val a = o1.value().toString()
        val b = o2.value().toString()

        var ia = 0
        var ib = 0
        var nza = 0
        var nzb = 0
        var ca: Char
        var cb: Char
        var result: Int

        while (true) {
            nza = 0
            nzb = 0
            ca = charAt(a, ia)
            cb = charAt(b, ib)
            while (isSpaceChar(ca) || ca == '0') {
                if (ca == '0') {
                    nza++
                } else {
                    nza = 0
                }
                ca = charAt(a, ++ia)
            }

            while (isSpaceChar(cb) || cb == '0') {
                if (cb == '0') {
                    nzb++
                } else {
                    nzb = 0
                }
                cb = charAt(b, ++ib)
            }

            if (isDigit(ca) && isDigit(cb)) {
                result = compareRight(a.substring(ia), b.substring(ib))
                if (result != 0) {
                    return result
                }
            }
            if (ca.toInt() == 0 && cb.toInt() == 0) {
                return nza - nzb
            }
            if (ca < cb) {
                return -1
            } else if (ca > cb) {
                return +1
            }

            ++ia
            ++ib
        }
    }

    internal fun charAt(s: String, i: Int): Char {
        if (i >= s.length) {
            return 0.toChar()
        } else {
            return s[i]
        }
    }
}