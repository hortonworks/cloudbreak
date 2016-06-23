package com.sequenceiq.it.spark.spi

import java.util.ArrayList

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.it.spark.ITResponse
import com.sequenceiq.it.util.ServerAddressGenerator

class CloudMetaDataStatuses(private val mockServerAddress: String, private val sshPort: Int) : ITResponse() {

    private fun createCloudVmMetaDataStatuses(cloudInstances: List<CloudInstance>): List<CloudVmMetaDataStatus> {
        val cloudVmMetaDataStatuses = ArrayList<CloudVmMetaDataStatus>()
        val numberOfServers = cloudInstances.size
        ServerAddressGenerator(numberOfServers).iterateOver { address, number ->
            val cloudInstance = cloudInstances[number]
            val cloudInstanceWithId = CloudInstance("instance-" + address, cloudInstance.template)
            val cloudVmInstanceStatus = CloudVmInstanceStatus(cloudInstanceWithId, InstanceStatus.STARTED)
            val cloudInstanceMetaData = CloudInstanceMetaData(address, mockServerAddress, sshPort, "MOCK")
            val cloudVmMetaDataStatus = CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData)
            cloudVmMetaDataStatuses.add(cloudVmMetaDataStatus)
        }
        return cloudVmMetaDataStatuses
    }

    @Throws(Exception::class)
    override fun handle(request: spark.Request, response: spark.Response): Any {
        val cloudInstances = Gson().fromJson<List<CloudInstance>>(request.body(), object : TypeToken<List<CloudInstance>>() {

        }.type)
        return createCloudVmMetaDataStatuses(cloudInstances)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(CloudMetaDataStatuses::class.java)
    }
}
