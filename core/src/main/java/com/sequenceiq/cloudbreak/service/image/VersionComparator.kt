package com.sequenceiq.cloudbreak.service.image

import java.util.Comparator

import com.sequenceiq.cloudbreak.cloud.model.Versioned

class VersionComparator : Comparator<Versioned> {

    override fun compare(o1: Versioned, o2: Versioned): Int {
        val vals1 = o1.version.split("[\\.\\-]".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        val vals2 = o2.version.split("[\\.\\-]".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

        var i = 0
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.size && i < vals2.size && vals1[i] == vals2[i]) {
            i++
        }
        // compare first non-equal ordinal number
        if (i < vals1.size && i < vals2.size) {
            val diff = Integer.valueOf(vals1[i])!!.compareTo(Integer.valueOf(vals2[i]))
            return Integer.signum(diff)
        }
        // the strings are equal or one string is a substring of the other, then shorter wins
        // e.g. "2.4.2.0" is newer than "2.4.2.0-9999"
        return Integer.signum(vals2.size - vals1.size)
    }
}
