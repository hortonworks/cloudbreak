package com.sequenceiq.cloudbreak.cloud.model

class VolumeParameterConfig(private val volumeParameterType: VolumeParameterType,
                            private val minimumSize: Int?,
                            private val maximumSize: Int?,
                            private val minimumNumber: Int?,
                            private val maximumNumber: Int?) {

    fun volumeParameterType(): VolumeParameterType {
        return volumeParameterType
    }

    fun minimumSize(): Int? {
        return minimumSize
    }

    fun minimumNumber(): Int? {
        return minimumNumber
    }

    fun maximumSize(): Int? {
        return maximumSize
    }

    fun maximumNumber(): Int? {
        return maximumNumber
    }
}
