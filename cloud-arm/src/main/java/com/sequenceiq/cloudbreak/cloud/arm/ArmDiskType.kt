package com.sequenceiq.cloudbreak.cloud.arm

enum class ArmDiskType private constructor(private val value: String, val abbreviation: String) {
    LOCALLY_REDUNDANT("Standard_LRS", "l"),
    GEO_REDUNDANT("Standard_GRS", "g"),
    PREMIUM_LOCALLY_REDUNDANT("Premium_LRS", "p");

    fun value(): String {
        return value
    }

    companion object {

        fun getByValue(value: String): ArmDiskType {
            when (value) {
                "Standard_LRS" -> return LOCALLY_REDUNDANT
                "Standard_GRS" -> return GEO_REDUNDANT
                "Premium_LRS" -> return PREMIUM_LOCALLY_REDUNDANT
                else -> return LOCALLY_REDUNDANT
            }
        }
    }
}
