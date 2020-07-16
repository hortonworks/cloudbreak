package com.sequenceiq.cloudbreak.service.freeipa;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class InstanceMetadataProcessor {

    public Set<String> extractIps(Stack stack) {
        return extractIps(stack.getInstanceMetaDataAsList());
    }

    public Set<String> extractIps(Collection<InstanceMetaData> instanceMetaData) {
        return collectDataFromInstanceMetaDataList(instanceMetaData, InstanceMetaData::getPrivateIp);
    }

    public Set<String> extractFqdn(Stack stack) {
        return extractFqdn(stack.getInstanceMetaDataAsList());
    }

    public Set<String> extractFqdn(Collection<InstanceMetaData> instanceMetaData) {
        return collectDataFromInstanceMetaDataList(instanceMetaData, InstanceMetaData::getDiscoveryFQDN);
    }

    private Set<String> collectDataFromInstanceMetaDataList(Collection<InstanceMetaData> instanceMetaData,
            Function<InstanceMetaData, String> instanceMetaDataStringFunction) {
        return instanceMetaData.stream().map(instanceMetaDataStringFunction).filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }
}
