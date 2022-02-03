package com.sequenceiq.cloudbreak.service.publicendpoint.dns;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Service
public class AllHostPublicDnsEntryService extends BaseDnsEntryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllHostPublicDnsEntryService.class);

    @Override
    protected Map<String, List<String>> getComponentLocation(Stack stack) {
        Map<String, List<String>> result = new HashMap<>();
        Optional<InstanceMetaData> gateway = Optional.ofNullable(stack.getPrimaryGatewayInstance());
        if (gateway.isEmpty()) {
            LOGGER.info("No running gateway or all node is terminated, we skip the dns entry deletion.");
        } else {
            InstanceMetaData primaryGatewayInstance = gateway.get();
            Map<String, List<String>> hostnamesByInstanceGroupName = stack.getNotTerminatedGatewayInstanceMetadata()
                    .stream()
                    .filter(im -> StringUtils.isNoneEmpty(im.getDiscoveryFQDN()) && !im.getDiscoveryFQDN().equals(primaryGatewayInstance.getDiscoveryFQDN()))
                    .collect(groupingBy(InstanceMetaData::getInstanceGroupName, mapping(InstanceMetaData::getDiscoveryFQDN, toList())));

            result.putAll(hostnamesByInstanceGroupName);
        }
        return result;
    }

    @Override
    protected String logName() {
        return "all nodes except primary gateway node";
    }
}
