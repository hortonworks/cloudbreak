package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.KERBEROS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine.DEFAULT_RECIPE_TIMEOUT;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;

@Service
public class ClusterSecurityService {

    public static final String KERBEROS_CLIENT = "KERBEROS_CLIENT";
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSecurityService.class);
    private static final String KERBEROS_SERVICE = "KERBEROS";
    private static final String REALM = "NODE.CONSUL";
    private static final String DOMAIN = "node.consul";
    private static final String INSTALLED_STATE = "INSTALLED";

    @Inject
    private AmbariClientProvider ambariClientProvider;
    @Inject
    private AmbariOperationService ambariOperationService;
    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private PluginManager pluginManager;
    @Inject
    private TlsSecurityService tlsSecurityService;

    public void enableKerberosSecurity(Stack stack) throws CloudbreakException {
        try {
            createAndStartKDC(stack);
            Cluster cluster = stack.getCluster();
            TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(clientConfig, cluster);
            ambariClient.addService(KERBEROS_SERVICE);
            ambariClient.addServiceComponent(KERBEROS_SERVICE, KERBEROS_CLIENT);
            ambariClient.addComponentsToHosts(ambariClient.getClusterHosts(), asList(KERBEROS_CLIENT));
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            InstanceMetaData metaData = new ArrayList<>(gateway.getInstanceMetaData()).get(0);
            String kdcHost = metaData.getDiscoveryFQDN();
            ambariClient.createKerberosConfig(kdcHost, REALM, DOMAIN);
            int installReqId = ambariClient.setServiceState(KERBEROS_SERVICE, INSTALLED_STATE);
            PollingResult pollingResult = waitForOperation(stack, ambariClient, singletonMap("INSTALL_KERBEROS", installReqId));
            if (isContinue(pollingResult)) {
                pollingResult = waitForOperation(stack, ambariClient, singletonMap("STOP_SERVICES", ambariClient.stopAllServices()));
                if (isContinue(pollingResult)) {
                    ambariClient.createKerberosDescriptor(REALM);
                    ambariClientProvider.setKerberosSession(ambariClient, cluster);
                    pollingResult = waitForOperation(stack, ambariClient, singletonMap("ENABLE_KERBEROS", ambariClient.enableKerberos()));
                    if (isContinue(pollingResult)) {
                        waitForOperation(stack, ambariClient, singletonMap("START_SERVICES", ambariClient.startAllServices()));
                    }
                }
            }
        } catch (InterruptedException ie) {
            throw new CloudbreakException(ie);
        } catch (Exception e) {
            LOGGER.error("Error occurred during enabling the kerberos security", e);
        }
    }

    private void createAndStartKDC(Stack stack) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        Set<String> gatewayHosts = new HashSet<>();
        for (InstanceMetaData gwNode : gateway.getInstanceMetaData()) {
            gatewayHosts.add(gwNode.getDiscoveryFQDN());
        }
        List<String> payload = Arrays.asList(cluster.getKerberosAdmin(), cluster.getKerberosPassword(), cluster.getKerberosMasterKey(), REALM);
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.CREATE_KERBEROS_KDC, DEFAULT_RECIPE_TIMEOUT, KERBEROS, payload, gatewayHosts);
    }

    private boolean isContinue(PollingResult result) throws InterruptedException {
        if (isExited(result)) {
            throw new InterruptedException("Interrupt enabling kerberos flow");
        }
        return true;
    }

    private PollingResult waitForOperation(Stack stack, AmbariClient ambariClient, Map<String, Integer> requests) {
        return ambariOperationService.waitForAmbariOperations(stack, ambariClient, requests);
    }

}
