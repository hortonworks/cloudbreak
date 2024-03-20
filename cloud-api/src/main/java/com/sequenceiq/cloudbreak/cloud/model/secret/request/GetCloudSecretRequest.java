package com.sequenceiq.cloudbreak.cloud.model.secret.request;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public record GetCloudSecretRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudResource cloudResource) {
}
