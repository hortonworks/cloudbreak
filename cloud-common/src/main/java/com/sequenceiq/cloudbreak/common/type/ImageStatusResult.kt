package com.sequenceiq.cloudbreak.common.type

class ImageStatusResult(val imageStatus: ImageStatus, val statusProgressValue: Int?) {
    companion object {

        val COMPLETED = 100
        val HALF = 100
        val INIT = 0
    }
}
