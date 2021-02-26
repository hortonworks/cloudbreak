package com.sequenceiq.cloudbreak.service.publicendpoint;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.publicendpoint.dns.BaseDnsEntryService;

@Service
public class ClusterPublicEndpointManagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPublicEndpointManagementService.class);

    @Inject
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @Inject
    private List<BaseDnsEntryService> dnsEntryServices;

    @Inject
    private FreeIPAEndpointManagementService freeIPAEndpointManagementService;

    public boolean provision(Stack stack) {
        boolean certGenerationWasSuccessful = gatewayPublicEndpointManagementService.generateCertAndSaveForStackAndUpdateDnsEntry(stack);
        dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.createOrUpdate(stack));
        return certGenerationWasSuccessful;
    }

    public boolean provisionLoadBalancer(Stack stack) {
        if (!gatewayPublicEndpointManagementService.renewCertificate(stack)) {
            LOGGER.warn("Certificate was not updated with load balancer SAN in PEM service.");
        } else {
            LOGGER.info("Certificated updated with load balancer SAN in PEM service.");
        }
        return gatewayPublicEndpointManagementService.updateDnsEntryForLoadBalancers(stack);
    }

    public void terminate(Stack stack) {
        gatewayPublicEndpointManagementService.deleteDnsEntry(stack, null);
        gatewayPublicEndpointManagementService.deleteLoadBalancerDnsEntry(stack, null);
        dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.deregister(stack));
        freeIPAEndpointManagementService.deleteLoadBalancerDomainFromFreeIPA(stack);
    }

    public void upscale(Stack stack, Map<String, String> newAddressesByFqdn) {
        changeGatewayAddress(stack, newAddressesByFqdn);
        dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.createOrUpdateCandidates(stack, newAddressesByFqdn));
    }

    public void downscale(Stack stack, Map<String, String> downscaledAddressesByFqdn) {
        dnsEntryServices.forEach(dnsEntryService -> dnsEntryService.deregister(stack, downscaledAddressesByFqdn));
    }

    public boolean changeGateway(Stack stack, String newGatewayIp) {
        String result = null;
        if (gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem()) {
            result = gatewayPublicEndpointManagementService.updateDnsEntry(stack, newGatewayIp);
        }
        return StringUtils.isNoneEmpty(result);
    }

    public boolean renewCertificate(Stack stack) {
        boolean result = false;
        if (gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stack)) {
            result = gatewayPublicEndpointManagementService.renewCertificate(stack);
        }
        return result;
    }

    public void start(Stack stack) {
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

    private void changeGatewayAddress(Stack stack, Map<String, String> newAddressesByFqdn) {
        InstanceMetaData gatewayInstanceMetadata = stack.getPrimaryGatewayInstance();
        String ipWrapper = gatewayInstanceMetadata.getPublicIpWrapper();

        if (newAddressesByFqdn.containsValue(ipWrapper)) {
            LOGGER.info("Gateway's DNS entry needs to be updated because primary gateway IP has been updated to: '{}'", ipWrapper);
            changeGateway(stack, ipWrapper);
        }
    }

    public void registerLoadBalancerWithFreeIPA(Stack stack) {
        freeIPAEndpointManagementService.registerLoadBalancerDomainWithFreeIPA(stack);
    }
}
