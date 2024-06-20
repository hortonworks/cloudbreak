package com.sequenceiq.cloudbreak.service.encryption;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public interface CloudInformationDecorator {

    default List<String> getLuksEncryptionKeyCryptographicPrincipals(DetailedEnvironmentResponse environment, Stack stack) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default List<String> getCloudSecretManagerEncryptionKeyCryptographicPrincipals(DetailedEnvironmentResponse environment, Stack stack) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default EncryptionKeyType getUserdataSecretEncryptionKeyType() {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default ResourceType getLuksEncryptionKeyResourceType() {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default ResourceType getCloudSecretManagerEncryptionKeyResourceType() {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default String getAuthorizedClientForLuksEncryptionKey(Stack stack, InstanceMetaData instanceMetaData) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default Map<String, List<String>> getUserdataSecretCryptographicPrincipalsForInstanceGroups(DetailedEnvironmentResponse environment, Stack stack) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default List<String> getUserdataSecretCryptographicAuthorizedClients(Stack stack, String instanceId) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    Platform platform();

    Variant variant();

}
