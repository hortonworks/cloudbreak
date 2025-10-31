package com.sequenceiq.cloudbreak.service.publicendpoint;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

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

    public void provision(StackDtoDelegate stack) {
        gatewayPublicEndpointManagementService.generateCertAndSaveForStackAndUpdateDnsEntry(stack);
        dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.createOrUpdate(stack));
    }

    public void provisionLoadBalancer(StackDtoDelegate stack) {
        gatewayPublicEndpointManagementService.renewCertificate(stack);
        LOGGER.info("Certificate updated with load balancer SAN in PEM service.");
        gatewayPublicEndpointManagementService.updateDnsEntryForLoadBalancers(stack);
    }

    public void terminate(StackDtoDelegate stack) {
        gatewayPublicEndpointManagementService.deleteDnsEntry(stack, null);
        gatewayPublicEndpointManagementService.deleteLoadBalancerDnsEntry(stack, null);
        dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.deregister(stack));
        freeIPAEndpointManagementService.deleteLoadBalancerDomainFromFreeIPA(stack);
    }

    public void upscale(StackDtoDelegate stack, Map<String, String> newAddressesByHostname) {
        changeGatewayAddress(stack, newAddressesByHostname);
        dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.createOrUpdateCandidates(stack, newAddressesByHostname));
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
        if (gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem(stack.getStack())) {
            result = gatewayPublicEndpointManagementService.updateDnsEntryForCluster(stack);
        }
        return StringUtils.isNoneEmpty(result);
    }

    public void renewCertificate(StackDtoDelegate stack) {
        if (gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stack.getStack())) {
            gatewayPublicEndpointManagementService.renewCertificate(stack);
        }
    }

    public void refreshDnsEntries(StackDtoDelegate stack) {
        if (gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem(stack.getStack())) {
            LOGGER.info("Updating DNS entries of a restarted cluster: '{}'", stack.getName());
            gatewayPublicEndpointManagementService.updateDnsEntry(stack, null);
            dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.createOrUpdate(stack));
        }
    }

    public boolean manageCertificateAndDnsInPem(StackView stackView) {
        return gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem(stackView);
    }

    private void changeGatewayAddress(StackDtoDelegate stackDto, Map<String, String> newAddressesByHostname) {
        InstanceMetadataView gatewayInstanceMetadata = stackDto.getPrimaryGatewayInstance();
        String ipWrapper = gatewayInstanceMetadata.getPublicIpWrapper();

        if (newAddressesByHostname.containsValue(ipWrapper)) {
            LOGGER.info("Gateway's DNS entry needs to be updated because primary gateway IP has been updated to: '{}'", ipWrapper);
            changeGateway(stackDto);
        }
    }

    public void registerLoadBalancerWithFreeIPA(StackView stack) {
        freeIPAEndpointManagementService.registerLoadBalancerDomainWithFreeIPA(stack);
    }
}
