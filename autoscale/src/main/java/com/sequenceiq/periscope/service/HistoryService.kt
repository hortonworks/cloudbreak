package com.sequenceiq.periscope.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import com.sequenceiq.periscope.api.model.ScalingStatus
import com.sequenceiq.periscope.domain.History
import com.sequenceiq.periscope.domain.ScalingPolicy
import com.sequenceiq.periscope.repository.HistoryRepository

@Service
class HistoryService {

    @Autowired
    private val historyRepository: HistoryRepository? = null
    @Autowired
    private val clusterService: ClusterService? = null

    fun createEntry(scalingStatus: ScalingStatus, statusReason: String, originalNodeCount: Int, scalingPolicy: ScalingPolicy) {
        val history = History(scalingStatus, statusReason, originalNodeCount).withScalingPolicy(scalingPolicy).withAlert(scalingPolicy.alert).withCluster(scalingPolicy.alert.cluster)
        historyRepository!!.save(history)
    }

    fun getHistory(clusterId: Long): List<History> {
        clusterService!!.findOneByUser(clusterId)
        return historyRepository!!.findAllByCluster(clusterId)
    }

    fun getHistory(clusterId: Long, historyId: Long): History {
        return historyRepository!!.findByCluster(clusterId, historyId)
    }
}
