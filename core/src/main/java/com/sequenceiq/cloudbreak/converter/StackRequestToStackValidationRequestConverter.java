package com.sequenceiq.cloudbreak.converter;

import java.util.HashSet;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

@Component
public class StackRequestToStackValidationRequestConverter extends AbstractConversionServiceAwareConverter<StackRequest, StackValidationRequest> {

    @Inject
    private CredentialService credentialService;

    @Inject
    private BlueprintService blueprintService;

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
            if (!Strings.isNullOrEmpty(source.getCredentialSource().getSourceName())) {
                Credential credential = credentialService.get(source.getCredentialSource().getSourceName(), source.getAccount());
                stackValidationRequest.setCredentialName(source.getCredentialSource().getSourceName());
                stackValidationRequest.setCredentialId(credential.getId());
            } else if (source.getCredentialSource().getSourceId() != null) {
                stackValidationRequest.setCredentialId(source.getCredentialSource().getSourceId());
            }
        }
        if (!Strings.isNullOrEmpty(source.getCredentialName())) {
            Credential credential = credentialService.get(source.getCredentialName(), source.getAccount());
            stackValidationRequest.setCredentialName(source.getCredentialName());
            stackValidationRequest.setCredentialId(credential.getId());
        }
        if (!Strings.isNullOrEmpty(clusterRequest.getBlueprintName())) {
            Blueprint blueprint = blueprintService.get(source.getCredentialName(), source.getAccount());
            stackValidationRequest.setBlueprintId(blueprint.getId());
        }
        stackValidationRequest.setFileSystem(source.getClusterRequest().getFileSystem());
        stackValidationRequest.setHostGroups(source.getClusterRequest().getHostGroups());
        stackValidationRequest.setInstanceGroups(new HashSet<>(source.getInstanceGroups()));
        return stackValidationRequest;
    }
}
