package com.sequenceiq.cloudbreak.converter;

import java.util.HashSet;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest;

@Component
public class StackRequestToStackValidationRequestConverter extends AbstractConversionServiceAwareConverter<StackRequest, StackValidationRequest> {
    @Override
    public StackValidationRequest convert(StackRequest source) {
        ClusterRequest clusterRequest = source.getClusterRequest();
        StackValidationRequest stackValidationRequest = new StackValidationRequest();
        stackValidationRequest.setBlueprint(clusterRequest.getBlueprint());
        stackValidationRequest.setBlueprintId(clusterRequest.getBlueprintId());
        stackValidationRequest.setCredential(source.getCredential());
        stackValidationRequest.setCredentialId(source.getCredentialId());
        stackValidationRequest.setNetwork(source.getNetwork());
        stackValidationRequest.setNetworkId(source.getNetworkId());
        stackValidationRequest.setPlatform(source.getCloudPlatform());
        if (source.getCredentialSource() != null) {
            stackValidationRequest.setCredentialName(source.getCredentialSource().getSourceName());
            stackValidationRequest.setCredentialId(source.getCredentialSource().getSourceId());
        }
        stackValidationRequest.setFileSystem(source.getClusterRequest().getFileSystem());
        stackValidationRequest.setHostGroups(source.getClusterRequest().getHostGroups());
        stackValidationRequest.setInstanceGroups(new HashSet<>(source.getInstanceGroups()));
        return stackValidationRequest;
    }
}
