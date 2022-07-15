package com.sequenceiq.cloudbreak.service.publicendpoint;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.publicendpoint.dns.BaseDnsEntryService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterPublicEndpointManagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPublicEndpointManagementService.class);

    @Inject
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @Inject
    private List<BaseDnsEntryService> dnsEntryServices;

    @Inject
    private FreeIPAEndpointManagementService freeIPAEndpointManagementService;

    public boolean provision(StackDtoDelegate stack) {
        boolean certGenerationWasSuccessful = gatewayPublicEndpointManagementService.generateCertAndSaveForStackAndUpdateDnsEntry(stack);
        dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.createOrUpdate(stack));
        return certGenerationWasSuccessful;
    }

    public boolean provisionLoadBalancer(StackDtoDelegate stack) {
        if (!gatewayPublicEndpointManagementService.renewCertificate(stack)) {
            LOGGER.warn("Certificate was not updated with load balancer SAN in PEM service.");
        } else {
            LOGGER.info("Certificated updated with load balancer SAN in PEM service.");
        }
        return gatewayPublicEndpointManagementService.updateDnsEntryForLoadBalancers(stack);
    }

    public void terminate(StackDtoDelegate stack) {
        gatewayPublicEndpointManagementService.deleteDnsEntry(stack, null);
        gatewayPublicEndpointManagementService.deleteLoadBalancerDnsEntry(stack, null);
        dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.deregister(stack));
        freeIPAEndpointManagementService.deleteLoadBalancerDomainFromFreeIPA(stack);
    }

    public void upscale(StackDtoDelegate stack, Map<String, String> newAddressesByFqdn) {
        changeGatewayAddress(stack, newAddressesByFqdn);
        dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.createOrUpdateCandidates(stack, newAddressesByFqdn));
    }

    public void downscale(StackDtoDelegate stack, Map<String, String> downscaledAddressesByFqdn) {
        if (MapUtils.isNotEmpty(downscaledAddressesByFqdn)) {
            LOGGER.info("Downscale candidate addresses to be de-registered from PEM: {}", String.join(", ", downscaledAddressesByFqdn.keySet()));
            dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.deregister(stack, downscaledAddressesByFqdn));
        } else {
            LOGGER.info("There is no downscale candidate address specified, no need for de-registering DNS entries at this downscale attempt.");
        }
    }

    public boolean changeGateway(StackDtoDelegate stack) {
        String result = null;
        if (gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem()) {
            result = gatewayPublicEndpointManagementService.updateDnsEntryForCluster(stack);
        }
        return StringUtils.isNoneEmpty(result);
    }

    public boolean renewCertificate(StackDtoDelegate stack) {
        boolean result = false;
        if (gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stack.getStack())) {
            result = gatewayPublicEndpointManagementService.renewCertificate(stack);
        }
        return result;
    }

    public void start(StackDtoDelegate stack) {
        if (gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem()) {
            try {
                LOGGER.info("Updating DNS entries of a restarted cluster: '{}'", stack.getName());
                gatewayPublicEndpointManagementService.updateDnsEntry(stack, null);
                dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.createOrUpdate(stack));
            } catch (Exception ex) {
                LOGGER.warn("Failed to update DNS entries of cluster in Public Endpoint Management service:", ex);
            }
        }
    }

    public boolean manageCertificateAndDnsInPem() {
        return gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem();
    }

    private void changeGatewayAddress(StackDtoDelegate stackDto, Map<String, String> newAddressesByFqdn) {
        InstanceMetadataView gatewayInstanceMetadata = stackDto.getPrimaryGatewayInstance();
        String ipWrapper = gatewayInstanceMetadata.getPublicIpWrapper();

        if (newAddressesByFqdn.containsValue(ipWrapper)) {
            LOGGER.info("Gateway's DNS entry needs to be updated because primary gateway IP has been updated to: '{}'", ipWrapper);
            changeGateway(stackDto);
        }
    }

    public void registerLoadBalancerWithFreeIPA(StackView stack) {
        freeIPAEndpointManagementService.registerLoadBalancerDomainWithFreeIPA(stack);
    }
}
