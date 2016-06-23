package com.sequenceiq.cloudbreak.common.type

enum class CbUserRole private constructor(private val value: String) {
    DEPLOYER("deployer"),
    ADMIN("admin"),
    USER("user");


    companion object {

        fun fromString(text: String?): CbUserRole? {
            if (text != null) {
                for (cbUserRole in CbUserRole.values()) {
                    if (text.equals(cbUserRole.value, ignoreCase = true)) {
                        return cbUserRole
                    }
                }
            }
            return null
        }
    }

}
