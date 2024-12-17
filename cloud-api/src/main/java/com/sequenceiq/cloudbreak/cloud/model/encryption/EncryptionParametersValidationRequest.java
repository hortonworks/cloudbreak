package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

public record EncryptionParametersValidationRequest(
        CloudContext cloudContext,
        CloudCredential cloudCredential,
        Map<ResourceType, CloudResource> cloudResources) {
}
