package com.sequenceiq.periscope.utils

import java.util.HashMap

class CloneUtils private constructor() {

    init {
        throw IllegalStateException()
    }

    companion object {

        fun copy(original: Map<String, Map<String, String>>?): Map<String, Map<String, String>> {
            val copy = HashMap<String, Map<String, String>>()
            if (original != null) {
                for (key in original.keys) {
                    copy.put(key, HashMap(original[key]))
                }
            }
            return copy
        }
    }
}
