package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static org.apache.commons.lang3.ObjectUtils.anyNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.SecurityRuleUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.util.CidrUtil;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;

@Component
public class InstanceGroupV1ToInstanceGroupV4Converter {

    @Inject
    private InstanceTemplateV1ToInstanceTemplateV4Converter instanceTemplateConverter;

    @Inject
    private InstanceGroupParameterConverter instanceGroupParameterConverter;

    public List<InstanceGroupV4Request> convertTo(Set<InstanceGroupV1Request> instanceGroups, DetailedEnvironmentResponse environment) {
        return instanceGroups.stream().map(ig -> convert(ig, environment)).collect(Collectors.toList());
    }

    public Set<InstanceGroupV1Request> convertFrom(List<InstanceGroupV4Request> instanceGroups) {
        return instanceGroups.stream().map(this::convert).collect(Collectors.toSet());
    }

    private InstanceGroupV4Request convert(InstanceGroupV1Request source, DetailedEnvironmentResponse environment) {
        InstanceGroupV4Request response = new InstanceGroupV4Request();
        response.setNodeCount(source.getNodeCount());
        response.setType(source.getType());
        response.setCloudPlatform(source.getCloudPlatform());
        response.setName(source.getName());
        response.setTemplate(getIfNotNull(source.getTemplate(), instanceTemplateConverter::convert));
        response.setRecoveryMode(source.getRecoveryMode());
        response.setSecurityGroup(createSecurityGroupFromEnvironment(source.getType(), environment));
        response.setRecipeNames(source.getRecipeNames());
        response.setAws(getIfNotNull(source.getAws(), instanceGroupParameterConverter::convert));
        response.setAzure(getIfNotNull(source.getAzure(), instanceGroupParameterConverter::convert));
        response.setGcp(getIfNotNull(source.getGcp(), instanceGroupParameterConverter::convert));
        response.setOpenstack(getIfNotNull(source.getOpenstack(), instanceGroupParameterConverter::convert));
        return response;
    }

    private InstanceGroupV1Request convert(InstanceGroupV4Request source) {
        InstanceGroupV1Request response = new InstanceGroupV1Request();
        response.setNodeCount(source.getNodeCount());
        response.setType(source.getType());
        response.setName(source.getName());
        response.setTemplate(getIfNotNull(source.getTemplate(), instanceTemplateConverter::convert));
        response.setRecoveryMode(source.getRecoveryMode());
        response.setRecipeNames(source.getRecipeNames());
        response.setAws(getIfNotNull(source.getAws(), instanceGroupParameterConverter::convert));
        response.setAzure(getIfNotNull(source.getAzure(), instanceGroupParameterConverter::convert));
        response.setGcp(getIfNotNull(source.getGcp(), instanceGroupParameterConverter::convert));
        response.setOpenstack(getIfNotNull(source.getOpenstack(), instanceGroupParameterConverter::convert));
        return response;
    }

    private SecurityGroupV4Request createSecurityGroupFromEnvironment(InstanceGroupType type, DetailedEnvironmentResponse environment) {
        if (environment == null) {
            SecurityGroupV4Request response = new SecurityGroupV4Request();
            SecurityRuleV4Request securityRule = new SecurityRuleV4Request();
            securityRule.setProtocol("tcp");
            securityRule.setSubnet("0.0.0.0/0");
            securityRule.setPorts(getPorts(type));
            response.setSecurityRules(List.of(securityRule));
            return response;
        } else {
            Optional<SecurityAccessResponse> securityAccess = Optional.of(environment).map(DetailedEnvironmentResponse::getSecurityAccess);
            if (securityAccess.isPresent() && anyNotNull(securityAccess.get().getSecurityGroupIdForKnox(),
                    securityAccess.get().getDefaultSecurityGroupId(),
                    securityAccess.get().getCidr())) {
                SecurityGroupV4Request securityGroup = new SecurityGroupV4Request();
                SecurityRuleV4Request securityRule = new SecurityRuleV4Request();
                securityRule.setProtocol("tcp");
                securityRule.setPorts(getPorts(type));
                securityGroup.setSecurityRules(List.of(securityRule));
                setupSecurityAccess(type, securityAccess.get(), securityGroup);
                return securityGroup;
            }
        }
        return null;
    }

    private List<String> getPorts(InstanceGroupType type) {
        List<String> ret = new ArrayList<>();
        ret.add("22");
        if (type == InstanceGroupType.GATEWAY) {
            ret.add("443");
        }
        return ret;
    }

    private void setupSecurityAccess(InstanceGroupType type, SecurityAccessResponse securityAccess, SecurityGroupV4Request securityGroup) {
        String securityGroupIdForKnox = securityAccess.getSecurityGroupIdForKnox();
        String defaultSecurityGroupId = securityAccess.getDefaultSecurityGroupId();
        String cidrs = securityAccess.getCidr();
        if (type == InstanceGroupType.GATEWAY) {
            setSecurityAccess(securityGroup, securityGroupIdForKnox, cidrs);
        } else {
            setSecurityAccess(securityGroup, defaultSecurityGroupId, cidrs);
        }
    }

    private void setSecurityAccess(SecurityGroupV4Request securityGroup, String securityGroupId, String cidrs) {
        if (!Strings.isNullOrEmpty(securityGroupId)) {
            securityGroup.setSecurityGroupIds(Set.of(securityGroupId));
            securityGroup.setSecurityRules(new ArrayList<>());
        } else if (!Strings.isNullOrEmpty(cidrs)) {
            List<SecurityRuleV4Request> generatedSecurityRules = new ArrayList<>();
            List<SecurityRuleV4Request> originalSecurityRules = securityGroup.getSecurityRules();
            for (String cidr : CidrUtil.cidrs(cidrs)) {
                SecurityRuleUtil.propagateCidr(generatedSecurityRules, originalSecurityRules, cidr);
            }
            // Because of YCLOUD we should not set this if null
            if (originalSecurityRules != null) {
                securityGroup.setSecurityRules(generatedSecurityRules);
            }
            securityGroup.setSecurityGroupIds(new HashSet<>());
        } else {
            securityGroup.setSecurityRules(new ArrayList<>());
            securityGroup.setSecurityGroupIds(new HashSet<>());
        }
    }
}
