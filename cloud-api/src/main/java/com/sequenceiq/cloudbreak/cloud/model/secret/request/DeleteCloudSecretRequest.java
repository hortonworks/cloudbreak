package com.sequenceiq.cloudbreak.cloud.model.secret.request;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public record DeleteCloudSecretRequest(CloudContext cloudContext, CloudCredential cloudCredential, List<CloudResource> cloudResources, String secretName) {
}
