package com.sequenceiq.cloudbreak.cloud.gcp

import java.io.IOException
import java.util.ArrayList
import java.util.Collections

import javax.inject.Inject

import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.compute.model.Operation
import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Variant
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel
import com.sequenceiq.cloudbreak.common.type.ResourceType

abstract class AbstractGcpResourceBuilder : CloudPlatformAware {

    @Inject
    val resourceNameService: GcpResourceNameService? = null

    override fun platform(): Platform {
        return GcpConstants.GCP_PLATFORM
    }

    override fun variant(): Variant {
        return GcpConstants.GCP_VARIANT
    }

    protected fun checkResources(type: ResourceType, context: GcpContext, auth: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus> {
        val result = ArrayList<CloudResourceStatus>()
        for (resource in resources) {
            LOGGER.info("Check {} resource: {}", type, resource)
            try {
                val operation = check(context, resource)
                val finished = operation == null || GcpStackUtil.analyzeOperation(operation)
                val successStatus = if (context.isBuild) ResourceStatus.CREATED else ResourceStatus.DELETED
                result.add(CloudResourceStatus(resource, if (finished) successStatus else ResourceStatus.IN_PROGRESS))
                if (finished) {
                    if (successStatus === ResourceStatus.CREATED) {
                        LOGGER.info("Creation of {} was successful", resource)
                    } else {
                        LOGGER.info("Deletion of {} was successful", resource)
                    }
                }
            } catch (e: Exception) {
                val cloudContext = auth.cloudContext
                throw GcpResourceException("Error during status check", type,
                        cloudContext.name, cloudContext.id, resource.name, e)
            }

        }
        return result
    }

    @Throws(IOException::class)
    protected fun check(context: GcpContext, resource: DynamicModel): Operation? {
        val operation = resource.getStringParameter(OPERATION_ID) ?: return null
        try {
            val execute = GcpStackUtil.globalOperations(context.compute, context.projectId, operation).execute()
            checkError(execute)
            return execute
        } catch (e: GoogleJsonResponseException) {
            if (e.details["code"] == HttpStatus.SC_NOT_FOUND) {
                val location = context.location
                try {
                    val execute = GcpStackUtil.regionOperations(context.compute, context.projectId, operation, location.region).execute()
                    checkError(execute)
                    return execute
                } catch (e1: GoogleJsonResponseException) {
                    if (e1.details["code"] == HttpStatus.SC_NOT_FOUND) {
                        val execute = GcpStackUtil.zoneOperations(context.compute, context.projectId, operation,
                                location.availabilityZone).execute()
                        checkError(execute)
                        return execute
                    } else {
                        throw e1
                    }
                }

            } else {
                throw e
            }
        }

    }

    protected fun checkError(execute: Operation) {
        if (execute.error != null) {
            var msg: String? = null
            val error = StringBuilder()
            if (execute.error.errors != null) {
                for (errors in execute.error.errors) {
                    error.append(String.format("code: %s -> message: %s %s", errors.code, errors.message, System.lineSeparator()))
                }
                msg = error.toString()
            }
            throw CloudConnectorException(msg)
        }
    }

    protected fun checkException(execute: GoogleJsonResponseException): String {
        return execute.details.message
    }

    protected fun createNamedResource(type: ResourceType, name: String): CloudResource {
        return CloudResource.Builder().type(type).name(name).build()
    }

    protected fun createOperationAwareCloudResource(resource: CloudResource, operation: Operation): CloudResource {
        return CloudResource.Builder().cloudResource(resource).params(Collections.singletonMap<String, Any>(OPERATION_ID, operation.name)).persistent(false).build()
    }

    protected fun createOperationAwareCloudInstance(instance: CloudInstance, operation: Operation): CloudInstance {
        return CloudInstance(instance.instanceId, instance.template,
                Collections.singletonMap<String, Any>(OPERATION_ID, operation.name))
    }

    protected fun exceptionHandler(ex: GoogleJsonResponseException, name: String, resourceType: ResourceType) {
        if (ex.details["code"] == HttpStatus.SC_NOT_FOUND) {
            LOGGER.info("Resource {} not found: {}", resourceType, name)
        } else {
            throw GcpResourceException(ex.details.message, ex)
        }
    }

    companion object {

        protected val OPERATION_ID = "opid"
        private val LOGGER = LoggerFactory.getLogger(AbstractGcpResourceBuilder::class.java)
    }

}
