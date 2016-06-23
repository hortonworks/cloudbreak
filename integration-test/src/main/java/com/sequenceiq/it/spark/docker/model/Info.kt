package com.sequenceiq.it.spark.docker.model

import java.util.ArrayList

import com.google.gson.annotations.SerializedName

class Info {

    @SerializedName("DriverStatus")
    var driverStatuses: List<Any>? = null

    constructor() {
    }

    constructor(serverNumber: Int) {
        driverStatuses = createStatusList(serverNumber)
    }

    private fun createStatusList(serverNumber: Int): List<Any> {
        val statusList = ArrayList<Any>()
        for (i in 0..serverNumber / 254) {
            val subAddress = Integer.min(254, serverNumber - i * 254)
            for (j in 1..subAddress) {
                val ipList = ArrayList<String>()
                ipList.add("server")
                ipList.add("192.168.$i.$j")
                statusList.add(ipList)
            }
        }
        return statusList
    }
}
