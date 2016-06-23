package com.sequenceiq.periscope.utils

import java.text.DecimalFormat

import com.sequenceiq.ambari.client.AmbariClient

object ClusterUtils {

    val TIME_FORMAT = DecimalFormat("##.##")
    val MAX_CAPACITY = 100
    val MIN_IN_MS = 1000 * 60


    fun getTotalNodes(ambariClient: AmbariClient): Int {
        return ambariClient.clusterHosts.size
    }
}
