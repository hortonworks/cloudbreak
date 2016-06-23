package com.sequenceiq.cloudbreak.cloud.openstack.nativ

import java.util.ArrayList

import javax.inject.Inject

import org.openstack4j.api.OSClient
import org.openstack4j.api.exceptions.OS4JException
import org.openstack4j.api.exceptions.StatusCode
import org.openstack4j.model.compute.ActionResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Variant
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext
import com.sequenceiq.cloudbreak.common.type.CommonStatus
import com.sequenceiq.cloudbreak.common.type.ResourceType

abstract class AbstractOpenStackResourceBuilder : CloudPlatformAware {

    @Inject
    private val openStackClient: OpenStackClient? = null

    protected fun createOSClient(auth: AuthenticatedContext): OSClient {
        return openStackClient!!.createOSClient(auth)
    }

    protected fun createNamedResource(resourceType: ResourceType, name: String): CloudResource {
        return CloudResource.Builder().name(name).type(resourceType).status(CommonStatus.REQUESTED).build()
    }

    @JvmOverloads protected fun createPersistedResource(namedResource: CloudResource, reference: String, params: MutableMap<String, Any> = Maps.newHashMap<String, Any>()): CloudResource {
        return CloudResource.Builder().cloudResource(namedResource).reference(reference).status(CommonStatus.CREATED).params(params).build()
    }

    protected fun checkResources(type: ResourceType, context: OpenStackContext, auth: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus> {
        val result = ArrayList<CloudResourceStatus>()
        for (resource in resources) {
            LOGGER.info("Check {} resource: {}", type, resource)
            try {
                val finished = checkStatus(context, auth, resource)
                val successStatus = if (context.isBuild) ResourceStatus.CREATED else ResourceStatus.DELETED
                result.add(CloudResourceStatus(resource, if (finished) successStatus else ResourceStatus.IN_PROGRESS))
                if (finished) {
                    if (successStatus === ResourceStatus.CREATED) {
                        LOGGER.info("Creation of {} was successful", resource)
                    } else {
                        LOGGER.info("Deletion of {} was successful", resource)
                    }
                }
            } catch (ex: OS4JException) {
                throw OpenStackResourceException("Error during status check", type, resource.name, ex)
            }

        }
        return result
    }

    protected fun checkDeleteResponse(response: ActionResponse, resourceType: ResourceType, auth: AuthenticatedContext, resource: CloudResource,
                                      faultMsg: String): CloudResource? {
        if (!response.isSuccess) {
            if (response.code != StatusCode.NOT_FOUND.code) {
                throw OpenStackResourceException(faultMsg, resourceType, resource.name, auth.cloudContext.id,
                        response.fault)
            } else {
                return null
            }
        }
        return resource
    }

    override fun platform(): Platform {
        return OpenStackConstants.OPENSTACK_PLATFORM
    }

    override fun variant(): Variant {
        return OpenStackConstants.OpenStackVariant.NATIVE.variant()
    }


    protected abstract fun checkStatus(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): Boolean

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AbstractOpenStackResourceBuilder::class.java)
    }
}
