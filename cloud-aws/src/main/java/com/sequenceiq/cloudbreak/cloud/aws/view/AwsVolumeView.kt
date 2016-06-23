package com.sequenceiq.cloudbreak.cloud.aws.view

import com.sequenceiq.cloudbreak.cloud.model.Volume

class AwsVolumeView(private val volume: Volume, index: Int) {

    val device: String

    val index: Int = 0

    init {
        this.device = "" + DEVICE_CHAR[index]
    }

    val mount: String
        get() = volume.mount

    val type: String
        get() = volume.type

    val size: Int
        get() = volume.size

    companion object {

        private val DEVICE_CHAR = charArrayOf('b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k')
    }
}