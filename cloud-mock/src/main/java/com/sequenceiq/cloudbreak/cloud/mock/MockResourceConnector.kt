package com.sequenceiq.cloudbreak.cloud.mock

import java.util.ArrayList

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.cloud.ResourceConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier

@Service
class MockResourceConnector : ResourceConnector {
    @Throws(Exception::class)
    override fun launch(authenticatedContext: AuthenticatedContext, stack: CloudStack, persistenceNotifier: PersistenceNotifier,
                        adjustmentType: AdjustmentType, threshold: Long?): List<CloudResourceStatus> {
        return ArrayList()
    }

    override fun check(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus> {
        return ArrayList()
    }

    @Throws(Exception::class)
    override fun terminate(authenticatedContext: AuthenticatedContext, stack: CloudStack, cloudResources: List<CloudResource>): List<CloudResourceStatus> {
        return ArrayList()
    }

    @Throws(Exception::class)
    override fun update(authenticatedContext: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        return ArrayList()
    }

    @Throws(Exception::class)
    override fun upscale(authenticatedContext: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        return ArrayList()
    }

    @Throws(Exception::class)
    override fun downscale(authenticatedContext: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>,
                           vms: List<CloudInstance>): List<CloudResourceStatus> {
        return ArrayList()
    }
}
