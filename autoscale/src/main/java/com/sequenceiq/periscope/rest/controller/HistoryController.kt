package com.sequenceiq.periscope.rest.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.sequenceiq.periscope.api.endpoint.HistoryEndpoint
import com.sequenceiq.periscope.api.model.HistoryJson
import com.sequenceiq.periscope.domain.History
import com.sequenceiq.periscope.rest.converter.HistoryConverter
import com.sequenceiq.periscope.service.HistoryService

@Component
class HistoryController : HistoryEndpoint {

    @Autowired
    private val historyService: HistoryService? = null
    @Autowired
    private val historyConverter: HistoryConverter? = null

    override fun getHistory(clusterId: Long?): List<HistoryJson> {
        val history = historyService!!.getHistory(clusterId!!)
        return historyConverter!!.convertAllToJson(history)
    }

    override fun getHistory(clusterId: Long?, historyId: Long?): HistoryJson {
        val history = historyService!!.getHistory(clusterId!!, historyId!!)
        return historyConverter!!.convert(history)
    }
}
