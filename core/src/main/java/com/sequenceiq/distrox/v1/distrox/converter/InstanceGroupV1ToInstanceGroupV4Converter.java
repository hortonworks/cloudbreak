package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;

@Component
public class InstanceGroupV1ToInstanceGroupV4Converter {

    @Inject
    private InstanceTemplateV1ToInstanceTemplateV4Converter instanceTemplateConverter;

    @Inject
    private InstanceGroupParameterConverter instanceGroupParameterConverter;

    public List<InstanceGroupV4Request> convertTo(Set<InstanceGroupV1Request> instanceGroups) {
        return instanceGroups.stream().map(this::convert).collect(Collectors.toList());
    }

    public InstanceGroupV4Request convert(InstanceGroupV1Request source) {
        InstanceGroupV4Request response = new InstanceGroupV4Request();
        response.setNodeCount(source.getNodeCount());
        response.setType(source.getType());
        response.setCloudPlatform(source.getCloudPlatform());
        response.setName(source.getName());
        response.setTemplate(getIfNotNull(source.getTemplate(), instanceTemplateConverter::convert));
        response.setRecoveryMode(source.getRecoveryMode());
        response.setSecurityGroup(getSecurityGroup(source.getType()));
        response.setRecipeNames(source.getRecipeNames());
        response.setAws(getIfNotNull(source.getAws(), instanceGroupParameterConverter::convert));
        response.setAzure(getIfNotNull(source.getAzure(), instanceGroupParameterConverter::convert));
        return response;
    }

    public Set<InstanceGroupV1Request> convertFrom(List<InstanceGroupV4Request> instanceGroups) {
        return instanceGroups.stream().map(this::convert).collect(Collectors.toSet());
    }

    public InstanceGroupV1Request convert(InstanceGroupV4Request source) {
        InstanceGroupV1Request response = new InstanceGroupV1Request();
        response.setNodeCount(source.getNodeCount());
        response.setType(source.getType());
        response.setName(source.getName());
        response.setTemplate(getIfNotNull(source.getTemplate(), instanceTemplateConverter::convert));
        response.setRecoveryMode(source.getRecoveryMode());
        response.setRecipeNames(source.getRecipeNames());
        response.setAws(getIfNotNull(source.getAws(), instanceGroupParameterConverter::convert));
        response.setAzure(getIfNotNull(source.getAzure(), instanceGroupParameterConverter::convert));
        return response;
    }

    private SecurityGroupV4Request getSecurityGroup(InstanceGroupType type) {
        SecurityGroupV4Request response = new SecurityGroupV4Request();
        SecurityRuleV4Request securityRule = new SecurityRuleV4Request();
        securityRule.setProtocol("tcp");
        securityRule.setSubnet("0.0.0.0/0");
        securityRule.setPorts(getPorts(type));
        response.setSecurityRules(List.of(securityRule));
        return response;
    }

    private List<String> getPorts(InstanceGroupType type) {
        List<String> ret = new ArrayList<>();
        ret.add("22");
        if (type == InstanceGroupType.GATEWAY) {
            ret.addAll(List.of("9443", "8443", "443"));
        }
        return ret;
    }
}
