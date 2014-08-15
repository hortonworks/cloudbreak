package com.sequenceiq.cloudbreak.service.cluster;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.InvalidHostGroupHostAssociation;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.HadoopConfigurationProvider;

import groovyx.net.http.HttpResponseException;
import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class AmbariClusterInstaller {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterInstaller.class);

    private static final int POLLING_INTERVAL = 3000;
    private static final int MS_PER_SEC = 1000;
    private static final int SEC_PER_MIN = 60;
    private static final int MAX_RECOMMEND_POLLING_ATTEMPTS = SEC_PER_MIN / (POLLING_INTERVAL / MS_PER_SEC) * 20;

    private static final BigDecimal COMPLETED = new BigDecimal(100.0);
    private static final BigDecimal FAILED = new BigDecimal(-1.0);

    private static final String UNHANDLED_EXCEPTION_MSG = "Unhandled exception occurred while installing Ambari cluster.";

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private Reactor reactor;

    @Resource
    private Map<CloudPlatform, HadoopConfigurationProvider> hadoopConfigurationProviders;

    public void installAmbariCluster(Stack stack) {
        Cluster cluster = stack.getCluster();
        try {
            LOGGER.info("Starting Ambari cluster installation for stack '{}' [Ambari server address: {}]", stack.getId(), stack.getAmbariIp());
            cluster.setCreationStarted(new Date().getTime());
            cluster = clusterRepository.save(cluster);
            Blueprint blueprint = cluster.getBlueprint();
            addBlueprint(stack, blueprint);
            AmbariClient ambariClient = createAmbariClient(stack.getAmbariIp());
            Map<String, List<String>> hostGroupMappings = recommend(stack, ambariClient, blueprint.getBlueprintName());
            LOGGER.info("recommended host-hostGroup mappings for stack {}: {}", stack.getId(), hostGroupMappings);
            ambariClient.createCluster(cluster.getName(), blueprint.getBlueprintName(), hostGroupMappings);
            BigDecimal installProgress = new BigDecimal(0);
            while (installProgress.compareTo(COMPLETED) != 0 && installProgress.compareTo(FAILED) != 0) {
                sleep(POLLING_INTERVAL);
                installProgress = ambariClient.getInstallProgress();
                LOGGER.info("Ambari Cluster installing. [Stack: '{}', Cluster: '{}', Progress: {}]", stack.getId(), cluster.getName(), installProgress);
            }
            if (installProgress.compareTo(COMPLETED) == 0) {
                clusterCreateSuccess(cluster, new Date().getTime(), stack.getAmbariIp());
            } else if (installProgress.compareTo(FAILED) == 0) {
                throw new ClusterInstallFailedException("Ambari failed to install services.");
            }
        } catch (AmbariHostsUnavailableException | ClusterInstallFailedException | InvalidHostGroupHostAssociation e) {
            LOGGER.error(e.getMessage(), e);
            clusterCreateFailed(cluster, e.getMessage());
        } catch (Exception e) {
            LOGGER.error(UNHANDLED_EXCEPTION_MSG, e);
            clusterCreateFailed(cluster, UNHANDLED_EXCEPTION_MSG);
        }
    }

    private Map<String, List<String>> recommend(Stack stack, AmbariClient ambariClient, String blueprintName) throws InvalidHostGroupHostAssociation {
        int nodeCount = 0;
        int pollingAttempt = 0;
        LOGGER.info("Waiting for hosts to connect. [Stack: {}, Ambari server address: {}]", stack.getId(), stack.getAmbariIp());
        while (nodeCount != stack.getNodeCount() && !(pollingAttempt >= MAX_RECOMMEND_POLLING_ATTEMPTS)) {
            nodeCount = 0;
            Map<String, String> hostNames = ambariClient.getHostNames();
            nodeCount = hostNames.size();
            LOGGER.info("Ambari client found {} hosts. [Stack: {}, Ambari server address: {}]", nodeCount, stack.getId(), stack.getAmbariIp());
            sleep(POLLING_INTERVAL);
            pollingAttempt++;
        }
        if (pollingAttempt >= MAX_RECOMMEND_POLLING_ATTEMPTS) {
            String errorMessage = String.format("Operation timed out. Failed to find all Ambari hosts in %s seconds. Found %s instead of %s",
                    MAX_RECOMMEND_POLLING_ATTEMPTS * POLLING_INTERVAL / MS_PER_SEC, nodeCount, stack.getNodeCount());
            throw new AmbariHostsUnavailableException(errorMessage);
        }
        LOGGER.info("Asking Ambari client to recommend host-hostGroup mapping [Stack: {}, Ambari server address: {}]", stack.getId(), stack.getAmbariIp());
        return ambariClient.recommendAssignments(blueprintName);
    }

    private void addBlueprint(Stack stack, Blueprint blueprint) {
        String ambariIp = stack.getAmbariIp();
        AmbariClient ambariClient = createAmbariClient(ambariIp);
        try {
            ambariClient.addBlueprint(blueprint.getBlueprintText(), getExtendConfig(stack));
            LOGGER.info("Blueprint added [Ambari server: {}, blueprint: '{}']", ambariIp, blueprint.getId());
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Ambari blueprint already exists.", e);
            } else if ("Bad Request".equals(e.getMessage())) {
                throw new BadRequestException("Failed to validate Ambari blueprint.", e);
            } else {
                throw new InternalServerException("Something went wrong", e);
            }
        }
    }

    private Map<String, Map<String, String>> getExtendConfig(Stack stack) {
        Map<String, Map<String, String>> extendConfig = new HashMap<>();
        HadoopConfigurationProvider hadoopConfigurationProvider = hadoopConfigurationProviders.get(stack.getTemplate().cloudPlatform());
        extendConfig.put(HadoopConfiguration.YARN_SITE.getKey(), hadoopConfigurationProvider.getYarnSiteConfigs(stack));
        extendConfig.put(HadoopConfiguration.HDFS_SITE.getKey(), hadoopConfigurationProvider.getHdfsSiteConfigs(stack));
        return extendConfig;
    }

    private void clusterCreateSuccess(Cluster cluster, long creationFinished, String ambariIp) {
        LOGGER.info("Publishing {} event [ClusterId: '{}']", ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT, cluster.getId());
        reactor.notify(ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT, Event.wrap(new ClusterCreationSuccess(cluster.getId(), creationFinished, ambariIp)));
    }

    private void clusterCreateFailed(Cluster cluster, String message) {
        LOGGER.info("Publishing {} event [ClusterId: '{}']", ReactorConfig.CLUSTER_CREATE_FAILED_EVENT, cluster.getId());
        reactor.notify(ReactorConfig.CLUSTER_CREATE_FAILED_EVENT, Event.wrap(new ClusterCreationFailure(cluster.getId(), message)));
    }

    public void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            LOGGER.info("Interrupted exception occured during polling.", e);
            Thread.currentThread().interrupt();
        }
    }

    @VisibleForTesting
    protected AmbariClient createAmbariClient(String ambariIp) {
        return new AmbariClient(ambariIp, AmbariClusterService.PORT);
    }
}
