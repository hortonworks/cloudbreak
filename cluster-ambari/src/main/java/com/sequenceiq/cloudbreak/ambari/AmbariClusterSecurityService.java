package com.sequenceiq.cloudbreak.ambari;

import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_PREPARE_DEKERBERIZING_ERROR;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.DISABLE_KERBEROS_STATE;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.PREPARE_DEKERBERIZING;
import static com.sequenceiq.cloudbreak.ambari.AmbariRepositoryVersionService.AMBARI_VERSION_2_7_0_0;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cluster.service.ClusterConnectorPollingResultChecker;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

import groovyx.net.http.HttpResponseException;

@Service
@Scope("prototype")
public class AmbariClusterSecurityService implements ClusterSecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterSecurityService.class);

    private static final String ADMIN = "admin";

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    @Inject
    private AmbariClientFactory ambariClientFactory;

    @Inject
    private AmbariUserHandler ambariUserHandler;

    @Inject
    private ClusterConnectorPollingResultChecker clusterConnectorPollingResultChecker;

    @Inject
    private AmbariOperationService ambariOperationService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Inject
    private AmbariLdapService ambariLdapService;

    @Inject
    private AmbariSSOService ambariSSOService;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    private AmbariClient ambariClient;

    public AmbariClusterSecurityService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initAmbariClient() {
        ambariClient = ambariClientFactory.getAmbariClient(stack, stack.getCluster(), clientConfig);
    }

    @Override
    public void prepareSecurity() {
        try {
            Map<String, Integer> operationRequests = new HashMap<>();
            Stream.of("ZOOKEEPER", "HDFS", "YARN", "MAPREDUCE2", "KERBEROS").forEach(s -> {
                int opId = 0;
                try {
                    opId = "ZOOKEEPER".equals(s) ? ambariClient.startService(s) : stopServiceIfAvailable(ambariClient, s);
                } catch (URISyntaxException | IOException e) {
                    throw new RuntimeException(e);
                }
                if (opId != -1) {
                    operationRequests.put(s + "_SERVICE_STATE", opId);
                }
            });
            if (operationRequests.isEmpty()) {
                return;
            }
            Pair<PollingResult, Exception> pair = ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, PREPARE_DEKERBERIZING);
            clusterConnectorPollingResultChecker.checkPollingResult(
                    pair.getLeft(), cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED.code()));
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (Exception e) {
            throw new AmbariOperationFailedException(cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_PREPARE_DEKERBERIZING_ERROR.code()), e);
        }
    }

    private int stopServiceIfAvailable(AmbariClient ambariClient, String s) {
        try {
            return ambariClient.stopService(s);
        }  catch (HttpResponseException e) {
            if (e.getResponse().getStatus() == HttpStatus.NOT_FOUND.value()) {
                LOGGER.debug("Cannot stop service [{}], becasue it does not exists.", s);
                return -1;
            }
            throw new AmbariOperationFailedException("Failed to stop services", e);
        } catch (IOException | URISyntaxException e) {
            throw new AmbariOperationFailedException("Failed to connect Ambari server", e);
        }
    }

    @Override
    public void disableSecurity() {
        try {
            int opId = ambariClient.disableKerberos();
            if (opId == -1) {
                return;
            }
            Map<String, Integer> operationRequests = singletonMap("DISABLE_KERBEROS_REQUEST", opId);
            Pair<PollingResult, Exception> pair = ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE);
            clusterConnectorPollingResultChecker.checkPollingResult(
                    pair.getLeft(), cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED.code()));
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (Exception e) {
            throw new AmbariOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void replaceUserNamePassword(String newUserName, String newPassword) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        String userName = cluster.getUserName();
        ambariClient = ambariUserHandler.createAmbariUser(newUserName, newPassword, stack, ambariClient, clientConfig);
        try {
            ambariClient.deleteUser(userName);
        } catch (URISyntaxException | IOException e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public void updateUserNamePassword(String newPassword) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        String userName = cluster.getUserName();
        String password = cluster.getPassword();
        ambariUserHandler.changeAmbariPassword(userName, password, newPassword, stack, ambariClient, clientConfig);
    }

    @Override
    public void changeOriginalCredentialsAndCreateCloudbreakUser() throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        LOGGER.debug("Changing ambari credentials for cluster: {}, ambari ip: {}", cluster.getName(), cluster.getAmbariIp());
        AmbariClient client = ambariClientFactory.getDefaultAmbariClient(stack, clientConfig);
        String cloudbreakUserName = ambariSecurityConfigProvider.getCloudbreakClusterUserName(cluster);
        String cloudbreakPassword = ambariSecurityConfigProvider.getCloudbreakClusterPassword(cluster);
        ambariUserHandler.createAmbariUser(cloudbreakUserName, cloudbreakPassword, stack, client, clientConfig);
        String dpAmbariUserName = ambariSecurityConfigProvider.getDataplaneClusterUserName(cluster);
        String dpAmbariPassword = ambariSecurityConfigProvider.getDataplaneClusterPassword(cluster);
        if (isNotEmpty(dpAmbariUserName) && isNotEmpty(dpAmbariPassword)) {
            ambariUserHandler.createAmbariUser(dpAmbariUserName, dpAmbariPassword, stack, client, clientConfig);
        }
        String userName = cluster.getUserName();
        String password = cluster.getPassword();
        if (ADMIN.equals(userName)) {
            if (!ADMIN.equals(password)) {
                ambariUserHandler.changeAmbariPassword(ADMIN, ADMIN, password, stack, ambariClient, clientConfig);
            }
        } else {
            client = ambariUserHandler.createAmbariUser(userName, password, stack, client, clientConfig);
            try {
                client.deleteUser(ADMIN);
            } catch (URISyntaxException | IOException e) {
                throw new CloudbreakException(e);
            }
        }
    }

    @Override
    public void setupLdapAndSSO(String primaryGatewayPublicAddress) {
        AmbariRepo ambariRepo = clusterComponentConfigProvider.getAmbariRepo(stack.getCluster().getId());
        if (ambariRepo != null && ambariRepositoryVersionService.setupLdapAndSsoOnApi(ambariRepo)) {
            LOGGER.debug("Setup LDAP and SSO on API");
            try {
                ambariLdapService.setupLdap(stack, stack.getCluster(), ambariRepo, ambariClient);
                ambariLdapService.syncLdap(stack, ambariClient);
                ambariSSOService.setupSSO(ambariClient, stack.getCluster(), primaryGatewayPublicAddress);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.debug("Can not setup LDAP and SSO on API, Ambari too old or because Ambari repo is not found");
        }
    }

    @Override
    public boolean isLdapAndSSOReady(AmbariRepo ambariRepo) {
        return ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(ambariRepo::getVersion, AMBARI_VERSION_2_7_0_0);
    }

    @Override
    public String getCloudbreakClusterUserName() {
        return ambariSecurityConfigProvider.getCloudbreakClusterUserName(stack.getCluster());
    }

    @Override
    public String getCloudbreakClusterPassword() {
        return ambariSecurityConfigProvider.getCloudbreakClusterPassword(stack.getCluster());
    }

    @Override
    public String getDataplaneClusterUserName() {
        return ambariSecurityConfigProvider.getDataplaneClusterUserName(stack.getCluster());
    }

    @Override
    public String getDataplaneClusterPassword() {
        return ambariSecurityConfigProvider.getDataplaneClusterPassword(stack.getCluster());
    }

    @Override
    public String getClusterUserProvidedPassword() {
        return ambariSecurityConfigProvider.getClusterUserProvidedPassword(stack.getCluster());
    }

    @Override
    public String getCertPath() {
        return ambariSecurityConfigProvider.getCertPath();
    }

    @Override
    public String getKeystorePath() {
        return ambariSecurityConfigProvider.getKeystorePath();
    }

    @Override
    public String getKeystorePassword() {
        return ambariSecurityConfigProvider.getKeystorePassword();
    }

    @Override
    public String getMasterKey() {
        return ambariSecurityConfigProvider.getMasterKey(stack.getCluster());
    }
}
