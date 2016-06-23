package com.sequenceiq.periscope.rest.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.sequenceiq.periscope.api.endpoint.ConfigurationEndpoint
import com.sequenceiq.periscope.api.model.ScalingConfigurationJson
import com.sequenceiq.periscope.rest.converter.ClusterConverter
import com.sequenceiq.periscope.service.ClusterService

@Component
class ConfigurationController : ConfigurationEndpoint {

    @Autowired
    private val clusterService: ClusterService? = null
    @Autowired
    private val clusterConverter: ClusterConverter? = null

    override fun setScalingConfiguration(clusterId: Long?, json: ScalingConfigurationJson): ScalingConfigurationJson {
        clusterService!!.updateScalingConfiguration(clusterId!!, json)
        return json
    }

    override fun getScalingConfiguration(clusterId: Long?): ScalingConfigurationJson {
        return clusterService!!.getScalingConfiguration(clusterId!!)
    }

}
