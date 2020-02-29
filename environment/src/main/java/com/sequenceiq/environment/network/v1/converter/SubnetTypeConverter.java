package com.sequenceiq.environment.network.v1.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;

@Component
public class SubnetTypeConverter {

    public void convertSubnets(BaseNetwork source, NetworkDto.Builder targetBuilder) {
        // TODO: add legacy conversion for RegistrationType.CREATE_NEW networks
        if (source.getRegistrationType() == RegistrationType.EXISTING) {
            useAllSubnetsForAll(source, targetBuilder);
        } else {
            Map<String, CloudSubnet> publicSubnet = collectBySubnetType(source, SubnetType.PUBLIC);
            Map<String, CloudSubnet> privateSubnet = collectBySubnetType(source, SubnetType.PRIVATE);
            targetBuilder.withCbSubnets(mergeMaps(publicSubnet, privateSubnet));
            targetBuilder.withDwxSubnets(collectBySubnetType(source, SubnetType.DWX));
            targetBuilder.withMlxSubnets(collectBySubnetType(source, SubnetType.MLX));
        }
    }

    private void useAllSubnetsForAll(BaseNetwork source, NetworkDto.Builder targetBuilder) {
        targetBuilder.withCbSubnets(source.getSubnetMetas());
        targetBuilder.withDwxSubnets(source.getSubnetMetas());
        targetBuilder.withMlxSubnets(source.getSubnetMetas());
    }

    private Map<String, CloudSubnet> collectBySubnetType(BaseNetwork source, SubnetType subnetType) {
        return source.getSubnetMetas().entrySet().stream()
                .filter(entry -> entry.getValue().getType() == null || subnetType.equals(entry.getValue().getType()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, CloudSubnet> mergeMaps(Map<String, CloudSubnet> map1, Map<String, CloudSubnet> map2) {
        Map<String, CloudSubnet> merged = new HashMap<>(map1);
        merged.putAll(map2);
        return merged;
    }
}
