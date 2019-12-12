package com.sequenceiq.cloudbreak.service.publicendpoint;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Service
public class ClusterPublicEndpointManagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPublicEndpointManagementService.class);

    @Inject
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @Inject
    private KafkaBrokerPublicDnsEntryService kafkaBrokerPublicDnsEntryService;

    public boolean provision(Stack stack) {
        boolean certGenerationWasSuccessful = false;
        certGenerationWasSuccessful = gatewayPublicEndpointManagementService.generateCertAndSaveForStackAndUpdateDnsEntry(stack);
        kafkaBrokerPublicDnsEntryService.register(stack);
        return certGenerationWasSuccessful;
    }

    public void terminate(Stack stack) {
        gatewayPublicEndpointManagementService.deleteDnsEntry(stack, null);
        kafkaBrokerPublicDnsEntryService.deregister(stack);
    }

    public Map<String, String> upscale(Stack stack, Map<String, String> newAddressesByFqdn) {
        changeGatewayAddress(stack, newAddressesByFqdn);
        return kafkaBrokerPublicDnsEntryService.register(stack, newAddressesByFqdn);
    }

    public Map<String, String> downscale(Stack stack, Map<String, String> downscaledAddressesByFqdn) {
        return kafkaBrokerPublicDnsEntryService.deregister(stack, downscaledAddressesByFqdn);
    }

    public boolean changeGateway(Stack stack, String newGatewayIp) {
        String result = null;
        if (gatewayPublicEndpointManagementService.isCertGenerationEnabled()) {
            result = gatewayPublicEndpointManagementService.updateDnsEntry(stack, newGatewayIp);
        }
        return StringUtils.isNoneEmpty(result);
    }

    public boolean renewCertificate(Stack stack) {
        boolean result = false;
        if (gatewayPublicEndpointManagementService.isCertGenerationEnabled()) {
            result = gatewayPublicEndpointManagementService.renewCertificate(stack);
        }
        return result;
    }

    private void changeGatewayAddress(Stack stack, Map<String, String> newAddressesByFqdn) {
        InstanceMetaData gatewayInstanceMetadata = stack.getPrimaryGatewayInstance();
        String ipWrapper = gatewayInstanceMetadata.getPublicIpWrapper();

        if (newAddressesByFqdn.containsValue(ipWrapper)) {
            LOGGER.info("Gateway's DNS entry needs to be updated because primary gateway IP has been updated to: '{}'", ipWrapper);
            changeGateway(stack, ipWrapper);
        }
    }
}
