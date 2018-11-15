package com.sequenceiq.cloudbreak.converter.stack;

import java.util.HashSet;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialSourceRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackValidationRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class StackRequestToStackValidationRequestConverter extends AbstractConversionServiceAwareConverter<StackRequest, StackValidationRequest> {

    @Override
    public StackValidationRequest convert(StackRequest source) {
        ClusterRequest clusterRequest = source.getClusterRequest();
        StackValidationRequest stackValidationRequest = new StackValidationRequest();
        stackValidationRequest.setBlueprint(clusterRequest.getBlueprint());
        stackValidationRequest.setBlueprintId(clusterRequest.getBlueprintId());
        stackValidationRequest.setEnvironment(source.getEnvironment());
        stackValidationRequest.setCredential(source.getCredential());
        stackValidationRequest.setCredentialId(source.getCredentialId());
        stackValidationRequest.setNetwork(source.getNetwork());
        stackValidationRequest.setNetworkId(source.getNetworkId());
        stackValidationRequest.setPlatform(source.getCloudPlatform());
        CredentialSourceRequest credentialSource = source.getCredentialSource();
        if (credentialSource != null) {
            if (!Strings.isNullOrEmpty(credentialSource.getSourceName())) {
                stackValidationRequest.setCredentialName(credentialSource.getSourceName());
            } else if (credentialSource.getSourceId() != null) {
                stackValidationRequest.setCredentialId(credentialSource.getSourceId());
            }
        }
        if (!Strings.isNullOrEmpty(source.getCredentialName())) {
            stackValidationRequest.setCredentialName(source.getCredentialName());
        }
        stackValidationRequest.setBlueprintName(clusterRequest.getBlueprintName());
        stackValidationRequest.setFileSystem(source.getClusterRequest().getFileSystem());
        stackValidationRequest.setHostGroups(source.getClusterRequest().getHostGroups());
        stackValidationRequest.setInstanceGroups(new HashSet<>(source.getInstanceGroups()));
        return stackValidationRequest;
    }
}
