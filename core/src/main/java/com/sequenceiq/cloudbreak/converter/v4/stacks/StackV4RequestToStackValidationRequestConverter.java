package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashSet;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackValidationV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class StackV4RequestToStackValidationRequestConverter extends AbstractConversionServiceAwareConverter<StackV4Request, StackValidationV4Request> {

    @Override
    public StackValidationV4Request convert(StackV4Request source) {
        StackValidationV4Request stackValidationRequest = new StackValidationV4Request();
        stackValidationRequest.setClusterDefinitionName(source.getCluster().getAmbari().getClusterDefinitionName());
        stackValidationRequest.setEnvironmentName(source.getEnvironment().getName());
        stackValidationRequest.setCredentialName(source.getEnvironment().getCredentialName());
        stackValidationRequest.setNetwork(source.getNetwork());
        stackValidationRequest.setInstanceGroups(new HashSet<>(source.getInstanceGroups()));
        return stackValidationRequest;
    }
}
