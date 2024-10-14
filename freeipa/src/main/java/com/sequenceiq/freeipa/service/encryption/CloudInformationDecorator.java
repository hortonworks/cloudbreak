package com.sequenceiq.freeipa.service.encryption;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

public interface CloudInformationDecorator {

    default List<String> getLuksEncryptionKeyCryptographicPrincipals(DetailedEnvironmentResponse environment) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default List<String> getCloudSecretManagerEncryptionKeyCryptographicPrincipals(DetailedEnvironmentResponse environment) {
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

    default EncryptionKeyType getUserdataSecretEncryptionKeyType() {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default List<String> getUserdataSecretCryptographicPrincipals(Stack stack, CredentialResponse credentialResponse) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default List<String> getUserdataSecretCryptographicAuthorizedClients(Stack stack, String instanceId) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default ResourceType getUserdataSecretResourceType() {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    Platform platform();

    Variant variant();

}
