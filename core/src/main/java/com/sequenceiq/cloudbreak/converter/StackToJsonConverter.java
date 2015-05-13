package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.FailurePolicyJson;
import com.sequenceiq.cloudbreak.controller.json.InstanceGroupJson;
import com.sequenceiq.cloudbreak.controller.json.StackResponse;
import com.sequenceiq.cloudbreak.controller.json.SubnetJson;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Subnet;

@Component
public class StackToJsonConverter extends AbstractConversionServiceAwareConverter<Stack, StackResponse> {

    @Override
    public StackResponse convert(Stack source) {
        StackResponse stackJson = new StackResponse();
        stackJson.setName(source.getName());
        stackJson.setOwner(source.getOwner());
        stackJson.setAccount(source.getAccount());
        stackJson.setPublicInAccount(source.isPublicInAccount());
        stackJson.setId(source.getId());
        if (source.getCredential() == null) {
            stackJson.setCloudPlatform(null);
            stackJson.setCredentialId(null);
        } else {
            stackJson.setCloudPlatform(source.cloudPlatform());
            stackJson.setCredentialId(source.getCredential().getId());
        }
        stackJson.setStatus(source.getStatus());
        stackJson.setStatusReason(source.getStatusReason());
        stackJson.setAmbariServerIp(source.getAmbariIp());
        stackJson.setUserName(source.getUserName());
        stackJson.setPassword(source.getPassword());
        stackJson.setConsulServerCount(source.getConsulServers());
        stackJson.setRegion(source.getRegion());
        stackJson.setOnFailureAction(source.getOnFailureActionAction());
        stackJson.setAllowedSubnets(convertSubnets(source.getAllowedSubnets()));
        List<InstanceGroupJson> templateGroups = new ArrayList<>();
        templateGroups.addAll(convertInstanceGroups(source.getInstanceGroups()));
        stackJson.setInstanceGroups(templateGroups);
        if (source.getCluster() != null) {
            stackJson.setCluster(getConversionService().convert(source.getCluster(), ClusterResponse.class));
        } else {
            stackJson.setCluster(new ClusterResponse());
        }
        if (source.getFailurePolicy() != null) {
            stackJson.setFailurePolicy(getConversionService().convert(source.getFailurePolicy(), FailurePolicyJson.class));
        }
        stackJson.setImage(source.getImage());
        if (source.getNetwork() == null) {
            stackJson.setNetworkId(null);
        } else {
            stackJson.setNetworkId(source.getNetwork().getId());
        }
        return stackJson;
    }

    private List<SubnetJson> convertSubnets(Set<Subnet> source) {
        return (List<SubnetJson>) getConversionService().convert(source,
                TypeDescriptor.forObject(source),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(SubnetJson.class)));
    }

    private Set<InstanceGroupJson> convertInstanceGroups(Set<InstanceGroup> instanceGroups) {
        return (Set<InstanceGroupJson>) getConversionService().convert(instanceGroups, TypeDescriptor.forObject(instanceGroups),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceGroupJson.class)));
    }

}
