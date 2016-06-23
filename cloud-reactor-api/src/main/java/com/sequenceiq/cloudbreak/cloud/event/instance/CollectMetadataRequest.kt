package com.sequenceiq.cloudbreak.cloud.event.instance

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource

class CollectMetadataRequest(cloudContext: CloudContext, cloudCredential: CloudCredential, val cloudResource: List<CloudResource>, val vms: List<CloudInstance>) : CloudPlatformRequest<CollectMetadataResult>(cloudContext, cloudCredential) {

    //BEGIN GENERATED CODE
    override fun toString(): String {
        return "CollectMetadataRequest{, cloudResource=$cloudResource, vms=$vms}"
    }
    //END GENERATED CODE

}
