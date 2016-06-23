package com.sequenceiq.cloudbreak.cloud.gcp

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.Instance
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.template.AbstractInstanceConnector
import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import java.io.IOException
import java.util.ArrayList

@Service
class GcpInstanceConnector : AbstractInstanceConnector() {

    @Value("${cb.gcp.hostkey.verify:}")
    private val verifyHostKey: Boolean = false

    override fun check(ac: AuthenticatedContext, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val statuses = ArrayList<CloudVmInstanceStatus>()
        val credential = ac.cloudCredential
        val cloudContext = ac.cloudContext
        val compute = GcpStackUtil.buildCompute(credential)
        for (instance in vms) {
            var status = InstanceStatus.UNKNOWN
            try {
                val executeInstance = getInstance(cloudContext, credential, compute, instance.instanceId)
                if ("RUNNING" == executeInstance.status) {
                    status = InstanceStatus.STARTED
                } else if ("TERMINATED" == executeInstance.status) {
                    status = InstanceStatus.STOPPED
                }
            } catch (e: GoogleJsonResponseException) {
                if (e.statusCode == HttpStatus.SC_NOT_FOUND) {
                    status = InstanceStatus.TERMINATED
                } else {
                    LOGGER.warn(String.format("Instance %s is not reachable", instance), e)
                }
            } catch (e: IOException) {
                LOGGER.warn(String.format("Instance %s is not reachable", instance), e)
            }

            statuses.add(CloudVmInstanceStatus(instance, status))
        }
        return statuses
    }

    override fun getConsoleOutput(authenticatedContext: AuthenticatedContext, vm: CloudInstance): String {
        if (!verifyHostKey) {
            throw CloudOperationNotSupportedException("Host key verification is disabled on GCP")
        }
        val credential = authenticatedContext.cloudCredential
        try {
            val instanceGet = GcpStackUtil.buildCompute(credential)!!.instances().getSerialPortOutput(GcpStackUtil.getProjectId(credential),
                    authenticatedContext.cloudContext.location!!.availabilityZone.value(), vm.instanceId)
            return instanceGet.execute().contents
        } catch (e: Exception) {
            throw GcpResourceException("Couldn't parse SSH fingerprint from console output.", e)
        }

    }

    @Throws(IOException::class)
    private fun getInstance(context: CloudContext, credential: CloudCredential, compute: Compute, instanceName: String): Instance {
        return compute.instances().get(GcpStackUtil.getProjectId(credential),
                context.location!!.availabilityZone.value(), instanceName).execute()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(GcpInstanceConnector::class.java)
    }
}
