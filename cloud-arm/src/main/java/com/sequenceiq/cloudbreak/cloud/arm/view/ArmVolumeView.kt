package com.sequenceiq.cloudbreak.cloud.arm.view

import com.sequenceiq.cloudbreak.cloud.model.Volume

class ArmVolumeView(private val volume: Volume, index: Int) {

    val device: String

    val index: Int = 0

    init {
        this.device = KVM_DEVICE_PREFIX + DEVICE_CHAR[index]
    }

    val mount: String
        get() = volume.mount

    val type: String
        get() = volume.type

    val size: Int
        get() = volume.size

    companion object {

        private val KVM_DEVICE_PREFIX = "/dev/vd"

        private val DEVICE_CHAR = charArrayOf('b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z')
    }
}