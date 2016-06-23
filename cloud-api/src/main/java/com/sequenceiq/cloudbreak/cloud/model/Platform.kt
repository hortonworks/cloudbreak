package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType

class Platform private constructor(platform: String) : StringType(platform) {
    companion object {

        fun platform(platform: String): Platform {
            return Platform(platform)
        }
    }
}
