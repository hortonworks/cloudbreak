package com.sequenceiq.it.spark.ambari.model

import com.google.gson.annotations.SerializedName

class Hosts(

        @SerializedName("host_name")
        var hostName: List<String>?,

        @SerializedName("host_status")
        var hostStatus: String?)
