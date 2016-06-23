package com.sequenceiq.cloudbreak.cloud.gcp

import java.io.IOException
import java.util.ArrayList

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.Disk
import com.google.api.services.compute.model.DiskList
import com.sequenceiq.cloudbreak.cloud.CredentialConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus

@Service
class GcpCredentialConnector : CredentialConnector {

    @Inject
    private val gcpContextBuilder: GcpContextBuilder? = null

    @Inject
    private val gcpPlatformParameters: GcpPlatformParameters? = null

    override fun verify(authenticatedContext: AuthenticatedContext): CloudCredentialStatus {
        LOGGER.info("Verify credential: {}", authenticatedContext.cloudCredential)
        val gcpContext = gcpContextBuilder!!.contextInit(authenticatedContext.cloudContext, authenticatedContext, null, null, false)
        try {
            val compute = gcpContext.compute ?: throw CloudConnectorException("Problem with your credential key please use the correct format.")
            listDisks(gcpContext, compute)
        } catch (e: GoogleJsonResponseException) {
            val errorMessage = String.format(e.details.message)
            LOGGER.error(errorMessage, e)
            return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.FAILED, e, errorMessage)
        } catch (e: Exception) {
            val errorMessage = String.format("Could not verify credential [credential: '%s'], detailed message: %s", gcpContext.name, e.message)
            LOGGER.error(errorMessage, e)
            return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.FAILED, e, errorMessage)
        }

        return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.VERIFIED)
    }

    override fun create(authenticatedContext: AuthenticatedContext): CloudCredentialStatus {
        return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.CREATED)
    }

    @Throws(IOException::class)
    private fun listDisks(gcpContext: GcpContext, compute: Compute) {
        val disks = ArrayList<Disk>()
        for (gcpZone in gcpPlatformParameters!!.availabilityZones().allAvailabilityZone) {
            try {
                val list = compute.disks().list(gcpContext.projectId, gcpZone.value())
                val execute = list.execute()
                disks.addAll(execute.items)
            } catch (ex: NullPointerException) {
                disks.addAll(ArrayList<Disk>())
            }

        }
    }

    override fun delete(authenticatedContext: AuthenticatedContext): CloudCredentialStatus {
        return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.DELETED)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(GcpCredentialConnector::class.java)
    }
}
