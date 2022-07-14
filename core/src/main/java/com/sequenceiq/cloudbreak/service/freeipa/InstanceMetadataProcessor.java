package com.sequenceiq.cloudbreak.service.freeipa;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class InstanceMetadataProcessor {

    public Set<String> extractIps(Collection<InstanceMetadataView> instanceMetaData) {
        return collectDataFromInstanceMetaDataList(instanceMetaData, InstanceMetadataView::getPrivateIp);
    }

    public Set<String> extractFqdn(StackDtoDelegate stackDto) {
        return extractFqdn(stackDto.getNotTerminatedInstanceMetaData());
    }

    public Set<String> extractFqdn(Collection<InstanceMetadataView> instanceMetaData) {
        return collectDataFromInstanceMetaDataList(instanceMetaData, InstanceMetadataView::getDiscoveryFQDN);
    }

    private Set<String> collectDataFromInstanceMetaDataList(Collection<InstanceMetadataView> instanceMetaData,
            Function<InstanceMetadataView, String> instanceMetaDataStringFunction) {
        return instanceMetaData.stream().map(instanceMetaDataStringFunction).filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }
}
