package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_PREPARE_DEKERBERIZING_ERROR;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.DISABLE_KERBEROS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.PREPARE_DEKERBERIZING;
import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterSecurityService implements ClusterSecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterSecurityService.class);

    private static final String ADMIN = "admin";

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientFactory clientFactory;

    @Inject
    private AmbariUserHandler ambariUserHandler;

    @Inject
    private AmbariClusterConnectorPollingResultChecker ambariClusterConnectorPollingResultChecker;

    @Inject
    private AmbariOperationService ambariOperationService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Override
    public void prepareSecurity(Stack stack) {
        try {
            AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
            Map<String, Integer> operationRequests = new HashMap<>();
            Stream.of("ZOOKEEPER", "HDFS", "YARN", "MAPREDUCE2", "KERBEROS").forEach(s -> {
                int opId = s.equals("ZOOKEEPER") ? ambariClient.startService(s) : stopServiceIfAvailable(ambariClient, s);
                if (opId != -1) {
                    operationRequests.put(s + "_SERVICE_STATE", opId);
                }
            });
            if (operationRequests.isEmpty()) {
                return;
            }
            Pair<PollingResult, Exception> pair = ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, PREPARE_DEKERBERIZING);
            ambariClusterConnectorPollingResultChecker.checkPollingResult(
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
        } catch (Exception e) {
            //because groovy does not declare the throws
            if (e instanceof HttpResponseException && ((HttpResponseException) e).getResponse().getStatus() == HttpStatus.NOT_FOUND.value()) {
                LOGGER.info("Cannot stop service [{}], becasue it does not exists.", s);
                return -1;
            } else {
                throw e;
            }
        }
    }

    @Override
    public void disableSecurity(Stack stack) {
        try {
            AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
            int opId = ambariClient.disableKerberos();
            if (opId == -1) {
                return;
            }
            Map<String, Integer> operationRequests = singletonMap("DISABLE_KERBEROS_REQUEST", opId);
            Pair<PollingResult, Exception> pair = ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, DISABLE_KERBEROS_STATE);
            ambariClusterConnectorPollingResultChecker.checkPollingResult(
                    pair.getLeft(), cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED.code()));
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (Exception e) {
            throw new AmbariOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void replaceUserNamePassword(Stack stack, String newUserName, String newPassword) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        String userName = cluster.getUserName().getRaw();
        String password = cluster.getPassword().getRaw();
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, userName, password);
        ambariClient = ambariUserHandler.createAmbariUser(newUserName, newPassword, stack, ambariClient);
        ambariClient.deleteUser(userName);
    }

    @Override
    public void updateUserNamePassword(Stack stack, String newPassword) throws CloudbreakException {
        Cluster cluster = clusterService.getById(stack.getCluster().getId());
        String userName = cluster.getUserName().getRaw();
        String password = cluster.getPassword().getRaw();
        AmbariClient client = clientFactory.getAmbariClient(stack, userName, password);
        ambariUserHandler.changeAmbariPassword(userName, password, newPassword, stack, client);
    }

    @Override
    public void changeOriginalAmbariCredentialsAndCreateCloudbreakUser(Stack stack) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        LOGGER.info("Changing ambari credentials for cluster: {}, ambari ip: {}", cluster.getName(), cluster.getAmbariIp());
        AmbariClient client = clientFactory.getDefaultAmbariClient(stack);
        String cloudbreakUserName = ambariSecurityConfigProvider.getAmbariUserName(cluster);
        String cloudbreakPassword = ambariSecurityConfigProvider.getAmbariPassword(cluster);
        ambariUserHandler.createAmbariUser(cloudbreakUserName, cloudbreakPassword, stack, client);
        String userName = cluster.getUserName().getRaw();
        String password = cluster.getPassword().getRaw();
        if (ADMIN.equals(userName)) {
            if (!ADMIN.equals(password)) {
                ambariUserHandler.changeAmbariPassword(ADMIN, ADMIN, password, stack, client);
            }
        } else {
            client = ambariUserHandler.createAmbariUser(userName, password, stack, client);
            client.deleteUser(ADMIN);
        }
    }
}
