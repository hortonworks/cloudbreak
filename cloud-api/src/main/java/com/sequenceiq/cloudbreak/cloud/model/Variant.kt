package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType

class Variant private constructor(variant: String) : StringType(variant) {
    companion object {

        val EMPTY = Variant("")

        fun variant(variant: String): Variant {
            return Variant(variant)
        }
    }
}
