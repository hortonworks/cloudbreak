package com.sequenceiq.cloudbreak.cloud.model

import com.google.common.base.Optional

class StackParamValidation(val name: String, private val required: Boolean?, val clazz: Class<Any>, val regex: Optional<String>) {

    fun getRequired(): Boolean? {
        return required
    }

}
