package com.sequenceiq.cloudbreak.cloud.context

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel

/**
 * Context object to store the credentials and the cached Cloud Platfrom client object. The Cloud provider client objects are
 * stored in the [DynamicModel] and must be thread-safe.
 */
class AuthenticatedContext(val cloudContext: CloudContext, val cloudCredential: CloudCredential) : DynamicModel()
