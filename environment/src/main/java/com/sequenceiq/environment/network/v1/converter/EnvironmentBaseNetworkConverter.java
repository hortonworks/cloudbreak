package com.sequenceiq.environment.network.v1.converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.common.api.type.DeploymentRestriction;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {

    private final EnvironmentViewConverter environmentViewConverter;

    private final EntitlementService entitlementService;

    protected EnvironmentBaseNetworkConverter(EnvironmentViewConverter environmentViewConverter, EntitlementService entitlementService) {
        this.environmentViewConverter = environmentViewConverter;
        this.entitlementService = entitlementService;
    }

    @Override
    public BaseNetwork convert(Environment environment, NetworkDto creationDto, Map<String, CloudSubnet> subnetMetas,
            Map<String, CloudSubnet> gatewayEndpointSubnetMetas) {
        BaseNetwork result = createProviderSpecificNetwork(creationDto);
        result.setName(creationDto.getNetworkName() != null ? creationDto.getNetworkName() : environment.getName());
        result.setNetworkCidr(creationDto.getNetworkCidr());
        result.setNetworkCidrs(StringUtils.join(creationDto.getNetworkCidrs(), ","));
        result.setEnvironments(convertEnvToView(environment));
        result.setPrivateSubnetCreation(creationDto.getPrivateSubnetCreation());
        result.setServiceEndpointCreation(creationDto.getServiceEndpointCreation());
        result.setOutboundInternetTraffic(creationDto.getOutboundInternetTraffic());
        setRegistrationType(result, creationDto);
        result.setSubnetMetas(subnetMetas);
        result.setPublicEndpointAccessGateway(creationDto.getPublicEndpointAccessGateway());
        result.setLoadBalancerCreation(creationDto.getLoadBalancerCreation());
        result.setEndpointGatewaySubnetMetas(gatewayEndpointSubnetMetas);
        return result;
    }

    @Override
    public NetworkDto convertToDto(BaseNetwork source) {
        NetworkDto.Builder builder = NetworkDto.builder()
                .withId(source.getId())
                .withName(source.getName())
                .withSubnetMetas(setDefaultDeploymentRestrictions(source.getSubnetMetas()))
                .withNetworkCidr(source.getNetworkCidr())
                .withNetworkCidrs(getNetworkCidrs(source))
                .withResourceCrn(source.getResourceCrn())
                .withPrivateSubnetCreation(source.getPrivateSubnetCreation())
                .withServiceEndpointCreation(source.getServiceEndpointCreation())
                .withOutboundInternetTraffic(source.getOutboundInternetTraffic())
                .withRegistrationType(source.getRegistrationType())
                .withNetworkId(source.getNetworkId())
                .withPublicEndpointAccessGateway(source.getPublicEndpointAccessGateway())
                .withLoadBalancerCreation(source.getLoadBalancerCreation())
                .withEndpointGatewaySubnetMetas(setDefaultDeploymentRestrictionsForEndpointAccessGateway(source.getEndpointGatewaySubnetMetas()));

        convertSubnets(source, builder);

        return setProviderSpecificFields(builder, source);
    }

    /**
     * Provides on the fly deployment restrictions categorization for environments created earlier, where the DeploymentRestrictions is not set
     * @param subnetMetas subnet metadata of the environment
     * @return updated subnet metadata with deployment restrictions
     */
    private Map<String, CloudSubnet> setDefaultDeploymentRestrictions(Map<String, CloudSubnet> subnetMetas) {
        subnetMetas.entrySet().stream()
                .filter(m -> CollectionUtils.isEmpty(m.getValue().getDeploymentRestrictions()))
                .forEach(m -> m.getValue().setDeploymentRestrictions(m.getValue().isPrivateSubnet()
                        ? getDeploymentRestrictionForPrivateSubnet(m.getValue().getType())
                        : DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS));
        return subnetMetas;
    }

    protected Set<DeploymentRestriction> getDeploymentRestrictionForPrivateSubnet(SubnetType type) {
        return entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId())
                ? DeploymentRestriction.ALL
                : DeploymentRestriction.NON_ENDPOINT_ACCESS_GATEWAYS;
    }

    private Map<String, CloudSubnet> setDefaultDeploymentRestrictionsForEndpointAccessGateway(Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
        endpointGatewaySubnetMetas.entrySet().stream()
                .filter(m -> CollectionUtils.isEmpty(m.getValue().getDeploymentRestrictions()))
                .forEach(m -> m.getValue().setDeploymentRestrictions(DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS));
        return endpointGatewaySubnetMetas;
    }

    Set<String> getNetworkCidrs(BaseNetwork source) {
        if (Strings.isNullOrEmpty(source.getNetworkCidrs())) {
            if (Strings.isNullOrEmpty(source.getNetworkCidr())) {
                return Set.of();
            } else {
                return Set.of(source.getNetworkCidr());
            }
        } else {
            return Arrays.stream(source.getNetworkCidrs().split(",")).collect(Collectors.toSet());
        }
    }

    public void convertSubnets(BaseNetwork source, NetworkDto.Builder targetBuilder) {
        // TODO: add legacy conversion for RegistrationType.CREATE_NEW networks
        if (source.getRegistrationType() == RegistrationType.EXISTING) {
            useAllSubnetsForAll(source, targetBuilder);
        } else {
            Map<String, CloudSubnet> publicSubnet = collectBySubnetType(source, SubnetType.PUBLIC);
            Map<String, CloudSubnet> privateSubnet = collectBySubnetType(source, SubnetType.PRIVATE);
            targetBuilder.withCbSubnets(mergeMaps(publicSubnet, privateSubnet));
            targetBuilder.withDwxSubnets(collectDwxSubnetType(source));
            targetBuilder.withMlxSubnets(collectMlxSubnetType(source));
            targetBuilder.withLiftieSubnets(collectMlxSubnetType(source));
        }
    }

    private void useAllSubnetsForAll(BaseNetwork source, NetworkDto.Builder targetBuilder) {
        targetBuilder.withCbSubnets(source.getSubnetMetas());
        targetBuilder.withDwxSubnets(source.getSubnetMetas());
        targetBuilder.withMlxSubnets(source.getSubnetMetas());
        targetBuilder.withLiftieSubnets(source.getSubnetMetas());
    }

    private Map<String, CloudSubnet> collectBySubnetType(BaseNetwork source, SubnetType subnetType) {
        return source.getSubnetMetas().entrySet().stream()
                .filter(entry -> entry.getValue().getType() == null || subnetType.equals(entry.getValue().getType()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, CloudSubnet> collectDwxSubnetType(BaseNetwork source) {
        return source.getSubnetMetas().entrySet().stream()
                .filter(entry -> isApplicableForDwx(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, CloudSubnet> collectMlxSubnetType(BaseNetwork source) {
        return source.getSubnetMetas().entrySet().stream()
                .filter(entry -> isApplicableForMlx(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, CloudSubnet> mergeMaps(Map<String, CloudSubnet> map1, Map<String, CloudSubnet> map2) {
        Map<String, CloudSubnet> merged = new HashMap<>(map1);
        merged.putAll(map2);
        return merged;
    }

    public abstract boolean isApplicableForDwx(CloudSubnet cloudSubnet);

    public abstract boolean isApplicableForMlx(CloudSubnet cloudSubnet);

    private Set<EnvironmentView> convertEnvToView(Environment environment) {
        return Collections.singleton(environmentViewConverter.convert(environment));
    }

    @Override
    public Network convertToNetwork(BaseNetwork baseNetwork) {
        return null;
    }

    abstract BaseNetwork createProviderSpecificNetwork(NetworkDto network);

    abstract NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork source);

    abstract void setRegistrationType(BaseNetwork result, NetworkDto networkDto);
}
