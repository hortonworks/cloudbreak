package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType

class Region private constructor(value: String) : StringType(value) {
    companion object {

        fun region(value: String): Region {
            return Region(value)
        }
    }

}
