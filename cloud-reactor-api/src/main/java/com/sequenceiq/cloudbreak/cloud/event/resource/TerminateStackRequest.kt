package com.sequenceiq.cloudbreak.cloud.event.resource

import java.util.ArrayList

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack

class TerminateStackRequest<T>(cloudContext: CloudContext, cloudStack: CloudStack, cloudCredential: CloudCredential, resources: List<CloudResource>) : CloudStackRequest<T>(cloudContext, cloudCredential, cloudStack) {

    val cloudResources: List<CloudResource>

    init {
        this.cloudResources = ArrayList(resources)
    }

    //BEGIN GENERATED CODE
    override fun toString(): String {
        return "TerminateStackRequest{, cloudResources=$cloudResources}"
    }
    //END GENERATED CODE

}
