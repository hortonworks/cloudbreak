package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashSet;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackValidationV4Request;

@Component
public class StackV4RequestToStackValidationRequestConverter {

    public StackValidationV4Request convert(StackV4Request source) {
        StackValidationV4Request stackValidationRequest = new StackValidationV4Request();
        stackValidationRequest.setBlueprintName(source.getCluster().getBlueprintName());
        stackValidationRequest.setEnvironmentCrn(source.getEnvironmentCrn());
        stackValidationRequest.setNetwork(source.getNetwork());
        stackValidationRequest.setInstanceGroups(new HashSet<>(source.getInstanceGroups()));
        return stackValidationRequest;
    }
}
