package com.sequenceiq.cloudbreak.service.stack.flow;

import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class HostMetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostMetadataSetup.class);
    private static final String HOSTNAME_ENDPOINT = "saltboot/hostname/distribute";
    private static final String DEFAULT_DOMAIN = ".example.com";

    @Value("${cb.host.discovery.custom.domain:}")
    private String customDomain;

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public void setupHostMetadata(Long stackId) throws CloudbreakSecuritySetupException {
        LOGGER.info("Setting up host metadata for the cluster.");
        Stack stack = stackService.getById(stackId);
        if (!OrchestratorConstants.MARATHON.equals(stack.getOrchestrator().getType())) {
            Set<InstanceMetaData> allInstanceMetaData = stack.getRunningInstanceMetaData();
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stackId, gatewayInstance.getPublicIpWrapper());
            updateWithHostData(clientConfig, stack, Collections.emptySet());
            instanceMetaDataRepository.save(allInstanceMetaData);
        }
    }

    public void setupNewHostMetadata(Long stackId, Set<String> newAddresses) throws CloudbreakSecuritySetupException {
        LOGGER.info("Extending host metadata.");
        Stack stack = stackService.getById(stackId);
        if (!OrchestratorConstants.MARATHON.equals(stack.getOrchestrator().getType())) {
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stackId, gatewayInstance.getPublicIpWrapper());
            Set<InstanceMetaData> newInstanceMetadata = stack.getRunningInstanceMetaData().stream()
                    .filter(instanceMetaData -> newAddresses.contains(instanceMetaData.getPrivateIp()))
                    .collect(Collectors.toSet());
            updateWithHostData(clientConfig, stack, newInstanceMetadata);
            instanceMetaDataRepository.save(newInstanceMetadata);
        }
    }

    private void updateWithHostData(HttpClientConfig clientConfig, Stack stack, Set<InstanceMetaData> newInstanceMetadata)
            throws CloudbreakSecuritySetupException {
        Client restClient = null;
        try {
            restClient = RestClientUtil.createClient(clientConfig.getServerCert(), clientConfig.getClientCert(), clientConfig.getClientKey());
            Set<InstanceMetaData> metadataToUpdate;
            if (newInstanceMetadata == null || newInstanceMetadata.isEmpty()) {
                metadataToUpdate = stack.getRunningInstanceMetaData();
            } else {
                metadataToUpdate = newInstanceMetadata;
            }
            List<String> privateIps = metadataToUpdate.stream().map(InstanceMetaData::getPrivateIp).collect(Collectors.toList());
            WebTarget target = RestClientUtil.createSaltBootstrapTarget(restClient, stack.getSecurityConfig().getSaltBootPassword(),
                    clientConfig.getApiAddress(), clientConfig.getApiPort());
            GenericResponses responses = target.path(HOSTNAME_ENDPOINT).request()
                    .post(Entity.json(singletonMap("clients", privateIps))).readEntity(GenericResponses.class);
            Map<String, String> members = responses.getResponses().stream().collect(Collectors.toMap(GenericResponse::getAddress, GenericResponse::getStatus));
            LOGGER.info("Received host names from hosts: {}, original targets: {}", members.keySet(), privateIps);
            for (InstanceMetaData instanceMetaData : metadataToUpdate) {
                String privateIp = instanceMetaData.getPrivateIp();
                String address = members.get(privateIp);
                // TODO remove column
                instanceMetaData.setConsulServer(false);
                String fqdn = determineFqdn(instanceMetaData.getInstanceId(), instanceMetaData.getPrivateIp(), address);
                instanceMetaData.setDiscoveryFQDN(fqdn);
                LOGGER.info("Domain used for isntance: {} original: {}, fqdn: {}", instanceMetaData.getInstanceId(), address,
                        instanceMetaData.getDiscoveryFQDN());
            }
        } catch (Exception e) {
            throw new CloudbreakSecuritySetupException(e);
        } finally {
            if (restClient != null) {
                restClient.close();
            }
        }
    }

    private String determineFqdn(String id, String ip, String address) {
        String fqdn;
        if (StringUtils.isEmpty(customDomain)) {
            if (address.split("\\.").length == 1) {
                //if there is no domain, we need to add one since ambari fails without domain, actually it does nothing just hangs...
                fqdn = address + DEFAULT_DOMAIN;
                LOGGER.warn("Default domain is used, since there is no proper domain configured for instance: {}, ip: {}, original: {}, fqdn: {}",
                        id, ip, address, fqdn);
            } else {
                fqdn = address;
            }
        } else {
            String hostname = address.split("\\.")[0];
            // this is just for convenience
            if (customDomain.startsWith(".")) {
                fqdn = hostname + customDomain;
            } else {
                fqdn = hostname + "." + customDomain;
            }
            LOGGER.warn("Domain of instance will be overwritten instance: {}, ip: {}, original: {}, fqdn: {}",
                    id, ip, address, fqdn);
        }
        return fqdn;
    }

}