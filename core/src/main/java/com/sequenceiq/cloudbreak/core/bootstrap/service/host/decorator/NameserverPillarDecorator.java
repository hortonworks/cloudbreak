package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class NameserverPillarDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NameserverPillarDecorator.class);

    @Inject
    private DatalakeService datalakeService;

    private static List<InstanceMetadataView> getGatewayInstanceMetadata(Stack dataLakeStack) {
        return dataLakeStack.getNotTerminatedAndNotZombieGatewayInstanceMetadata();
    }

    public void decorateServicePillarWithNameservers(StackDto stackDto, KerberosConfig kerberosConfig, Map<String, SaltPillarProperties> servicePillar) {
        if (kerberosConfig != null && StringUtils.hasText(kerberosConfig.getDomain()) && StringUtils.hasText(kerberosConfig.getNameServers())) {
            LOGGER.debug("Add nameserver config to pillar based on kerberos config.");
            List<String> ipList = Lists.newArrayList(kerberosConfig.getNameServers().split(","));
            servicePillar.put("forwarder-zones", new SaltPillarProperties("/unbound/forwarders.sls",
                    singletonMap("forwarder-zones", singletonMap(kerberosConfig.getDomain(), singletonMap("nameservers", ipList)))));
        } else if (kerberosConfig == null || kerberosConfig.getType() != KerberosType.FREEIPA && kerberosConfig.getType() != KerberosType.ACTIVE_DIRECTORY) {
            decorateServicePillarWithDatalakeNameservers(stackDto, servicePillar);
        } else {
            LOGGER.debug("Skip to add nameserver config for pillar because kerberos config type is {}", kerberosConfig.getType());
        }
    }

    private void decorateServicePillarWithDatalakeNameservers(StackDto stackDto, Map<String, SaltPillarProperties> servicePillar) {
        Optional<Stack> datalakeStackOptional = datalakeService.getDatalakeStackByDatahubStack(stackDto.getStack());
        if (datalakeStackOptional.isPresent()) {
            LOGGER.debug("Add nameserver config to pillar based on datalake gateway addresses.");
            List<InstanceMetadataView> gatewayInstanceMetadata = getGatewayInstanceMetadata(datalakeStackOptional.get());
            String datalakeDomain = gatewayInstanceMetadata.get(0).getDomain();
            List<String> ipList = getDatalakeGatewayPrivateIps(gatewayInstanceMetadata);
            servicePillar.put("forwarder-zones", new SaltPillarProperties("/unbound/forwarders.sls",
                    singletonMap("forwarder-zones", singletonMap(datalakeDomain, singletonMap("nameservers", ipList)))));
        } else {
            LOGGER.debug("Skip to add nameserver config for pillar because Datalake stack is not present.");
        }
    }

    private List<String> getDatalakeGatewayPrivateIps(List<InstanceMetadataView> gatewayInstanceMetadata) {
        return gatewayInstanceMetadata.stream()
                .map(InstanceMetadataView::getPrivateIp)
                .filter(StringUtils::hasText)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Optional::of))
                .filter(ipList -> !ipList.isEmpty())
                .orElseThrow(() -> new CloudbreakServiceException("Unable to setup nameservers because there is no IP address present."));
    }
}
