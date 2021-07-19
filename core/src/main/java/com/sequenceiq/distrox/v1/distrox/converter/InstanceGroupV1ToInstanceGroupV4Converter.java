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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.SecurityRuleUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.CidrUtil;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.azure.InstanceGroupAzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.gcp.InstanceGroupGcpNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.mock.InstanceGroupMockNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;

@Component
public class InstanceGroupV1ToInstanceGroupV4Converter {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceGroupV1ToInstanceGroupV4Converter.class);

    @Inject
    private InstanceTemplateV1ToInstanceTemplateV4Converter instanceTemplateConverter;

    @Inject
    private InstanceGroupParameterConverter instanceGroupParameterConverter;

    @Inject
    private InstanceGroupNetworkV1ToInstanceGroupNetworkV4Converter networkConverter;

    public List<InstanceGroupV4Request> convertTo(NetworkV4Request network, Set<InstanceGroupV1Request> instanceGroups,
        DetailedEnvironmentResponse environment) {
        return instanceGroups.stream().map(ig -> convert(network, ig, environment)).collect(Collectors.toList());
    }

    public Set<InstanceGroupV1Request> convertFrom(NetworkV4Request network, List<InstanceGroupV4Request> instanceGroups,
        DetailedEnvironmentResponse environment) {
        return instanceGroups.stream().map(ig -> convert(network, ig, environment)).collect(Collectors.toSet());
    }

    private InstanceGroupV4Request convert(NetworkV4Request network, InstanceGroupV1Request source, DetailedEnvironmentResponse environment) {
        InstanceGroupV4Request response = new InstanceGroupV4Request();
        response.setNodeCount(source.getNodeCount());
        response.setType(source.getType());
        response.setCloudPlatform(source.getCloudPlatform());
        response.setName(source.getName());
        response.setTemplate(getIfNotNull(source.getTemplate(), environment, instanceTemplateConverter::convert));
        response.setRecoveryMode(source.getRecoveryMode());
        response.setScalabilityOption(source.getScalabilityOption());
        response.setSecurityGroup(createSecurityGroupFromEnvironment(source.getType(), environment));
        response.setRecipeNames(source.getRecipeNames());
        response.setMinimumNodeCount(source.getMinimumNodeCount());
        response.setAws(getIfNotNull(source.getAws(), instanceGroupParameterConverter::convert));
        response.setAzure(getIfNotNull(source.getAzure(), instanceGroupParameterConverter::convert));
        response.setGcp(getIfNotNull(source.getGcp(), instanceGroupParameterConverter::convert));
        response.setYarn(getIfNotNull(source.getYarn(), instanceGroupParameterConverter::convert));
        response.setNetwork(getInstanceGroupNetworkV4Request(network, source, environment));
        return response;
    }

    private InstanceGroupV1Request convert(NetworkV4Request stackNetwork, InstanceGroupV4Request source, DetailedEnvironmentResponse environment) {
        InstanceGroupV1Request response = new InstanceGroupV1Request();
        response.setNodeCount(source.getNodeCount());
        response.setType(source.getType());
        response.setName(source.getName());
        response.setTemplate(getIfNotNull(source.getTemplate(), instanceTemplateConverter::convert));
        response.setRecoveryMode(source.getRecoveryMode());
        response.setRecipeNames(source.getRecipeNames());
        response.setScalabilityOption(source.getScalabilityOption());
        response.setAws(getIfNotNull(source.getAws(), instanceGroupParameterConverter::convert));
        response.setAzure(getIfNotNull(source.getAzure(), instanceGroupParameterConverter::convert));
        response.setGcp(getIfNotNull(source.getGcp(), instanceGroupParameterConverter::convert));
        response.setNetwork(getInstanceGroupNetworkV1Request(stackNetwork, environment));
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
        if (InstanceGroupType.GATEWAY == type) {
            ret.add("443");
        }
        return ret;
    }

    private void setupSecurityAccess(InstanceGroupType type, SecurityAccessResponse securityAccess, SecurityGroupV4Request securityGroup) {
        String securityGroupIdForKnox = securityAccess.getSecurityGroupIdForKnox();
        String defaultSecurityGroupId = securityAccess.getDefaultSecurityGroupId();
        String cidrs = securityAccess.getCidr();
        if (InstanceGroupType.GATEWAY == type) {
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

    private InstanceGroupNetworkV4Request getInstanceGroupNetworkV4Request(NetworkV4Request distroxNetwork, InstanceGroupV1Request source,
        DetailedEnvironmentResponse environment) {
        if (requestContainsSingleAvailabilityZone(distroxNetwork, environment) || distroxNetwork == null) {
            source.setNetwork(getInstanceGroupNetworkV1Request(distroxNetwork, environment));
        }
        if (source.getNetwork() != null) {
            InstanceGroupNetworkV4Request network =
                    networkConverter.convertToInstanceGroupNetworkV4Request(new ImmutablePair<>(source.getNetwork(), environment));
            validateSubnetIds(network, environment);
            return network;
        }
        return null;

    }

    private InstanceGroupNetworkV1Request getInstanceGroupNetworkV1Request(NetworkV4Request distroxNetwork, DetailedEnvironmentResponse environment) {
        InstanceGroupNetworkV1Request request = null;
        if (environment != null && distroxNetwork != null) {
            request = getInstanceGroupNetworkV1RequestByProvider(distroxNetwork, environment);
        }
        return request;
    }

    private InstanceGroupNetworkV1Request getInstanceGroupNetworkV1RequestByProvider(NetworkV4Request distroxNetwork,
        DetailedEnvironmentResponse environment) {
        InstanceGroupNetworkV1Request request = null;
        switch (environment.getCloudPlatform()) {
            case "AWS":
                if (distroxNetwork.getAws() != null) {
                    request = new InstanceGroupNetworkV1Request();
                    InstanceGroupAwsNetworkV1Parameters aws = new InstanceGroupAwsNetworkV1Parameters();
                    aws.setSubnetIds(getSubnetIds(distroxNetwork.getAws().getSubnetId()));
                    request.setAws(aws);
                }
                break;
            case "AZURE":
                if (distroxNetwork.getAzure() != null) {
                    request = new InstanceGroupNetworkV1Request();
                    InstanceGroupAzureNetworkV1Parameters azure = new InstanceGroupAzureNetworkV1Parameters();
                    azure.setSubnetIds(getSubnetIds(distroxNetwork.getAzure().getSubnetId()));
                    request.setAzure(azure);
                }
                break;
            case "MOCK":
                if (distroxNetwork.getMock() != null) {
                    request = new InstanceGroupNetworkV1Request();
                    InstanceGroupMockNetworkV1Parameters mock = new InstanceGroupMockNetworkV1Parameters();
                    mock.setSubnetIds(getSubnetIds(distroxNetwork.getMock().getSubnetId()));
                    request.setMock(mock);
                }
                break;
            case "GCP":
                if (distroxNetwork.getGcp() != null) {
                    request = new InstanceGroupNetworkV1Request();
                    InstanceGroupGcpNetworkV1Parameters gcp = new InstanceGroupGcpNetworkV1Parameters();
                    gcp.setSubnetIds(getSubnetIds(distroxNetwork.getGcp().getSubnetId()));
                    request.setGcp(gcp);
                }
                break;
            default:
        }
        return request;
    }

    private List<String> getSubnetIds(String subnet) {
        if (Strings.isNullOrEmpty(subnet)) {
            return List.of();
        } else {
            return List.of(subnet);
        }
    }

    private boolean requestContainsSingleAvailabilityZone(NetworkV4Request distroxNetwork, DetailedEnvironmentResponse environment) {
        boolean requestContainsSingleAvailabilityZone = false;
        if (distroxNetwork != null && environment != null && !Strings.isNullOrEmpty(environment.getCloudPlatform())) {
            switch (environment.getCloudPlatform()) {
                case "AWS":
                    requestContainsSingleAvailabilityZone = !Strings.isNullOrEmpty(distroxNetwork.getAws().getSubnetId());
                    break;
                case "AZURE":
                    requestContainsSingleAvailabilityZone = !Strings.isNullOrEmpty(distroxNetwork.getAzure().getSubnetId());
                    break;
                case "GCP":
                    requestContainsSingleAvailabilityZone = !Strings.isNullOrEmpty(distroxNetwork.getGcp().getSubnetId());
                    break;
                case "MOCK":
                    requestContainsSingleAvailabilityZone = !Strings.isNullOrEmpty(distroxNetwork.getMock().getSubnetId());
                    break;
                default:
            }
        }
        return requestContainsSingleAvailabilityZone;
    }

    private void validateSubnetIds(InstanceGroupNetworkV4Request network, DetailedEnvironmentResponse environment) {
        if (environment != null) {
            switch (environment.getCloudPlatform()) {
                case "AWS":
                    validateSubnet(network, environment, network.getAws().getSubnetIds());
                    break;
                case "AZURE":
                    validateSubnet(network, environment, network.getAzure().getSubnetIds());
                    break;
                case "GCP":
                    validateSubnet(network, environment, network.getGcp().getSubnetIds());
                    break;
                default:
            }
        }
    }

    private void validateSubnet(InstanceGroupNetworkV4Request network, DetailedEnvironmentResponse environment, List<String> subnetIds) {
        if (subnetIds != null && !subnetIds.isEmpty()
                && (environment.getNetwork() == null
                || environment.getNetwork().getSubnetIds() == null
                || !environment.getNetwork().getSubnetIds().containsAll(subnetIds))) {
            LOGGER.info("The given subnet ID [{}] is not attached to the Environment [{}]. Network request: [{}]", subnetIds, environment, network);
            throw new BadRequestException(String.format("The given subnet IDs (%s) are not attached to the Environment (%s)",
                    subnetIds, environment.getName()));
        }
    }
}
