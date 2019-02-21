package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isTimeout;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;

@Service
@Scope("prototype")
public class ClouderaManagerModificationService implements ClusterModificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerModificationService.class);

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private CloudbreakEventService eventService;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private ApiClient client;

    public ClouderaManagerModificationService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() {
        client = clouderaManagerClientFactory.getClient(stack, stack.getCluster(), clientConfig);
    }

    @Override
    public void upscaleCluster(HostGroup hostGroup, Collection<HostMetadata> hostMetadata, List<InstanceMetaData> metas) throws CloudbreakException {

    }

    @Override
    public void stopCluster() throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        ClustersResourceApi clustersResourceApi = new ClustersResourceApi(client);
        try {
            LOGGER.debug("Stop all Hadoop services");
            eventService
                    .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                            cloudbreakMessagesService.getMessage(ClouderaManagerMessages.CM_CLUSTER_SERVICES_STOPPING.code()));
            ApiCommand apiCommand = clustersResourceApi.stopCommand(cluster.getName());
            PollingResult pollingResult = clouderaManagerPollingServiceProvider.stopPollingService(stack, client, apiCommand.getId());
            if (isExited(pollingResult)) {
                throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
            } else if (isTimeout(pollingResult)) {
                throw new CloudbreakException("Timeout while stopping Cloudera Manager services.");
            }
            eventService
                    .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                            cloudbreakMessagesService.getMessage(ClouderaManagerMessages.CM_CLUSTER_SERVICES_STOPPED.code()));
        } catch (ApiException e) {
            LOGGER.info("Couldn't stop ClouderaManager services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public int startCluster(Set<HostMetadata> hostsInCluster) throws CloudbreakException {
        try {
            startClouderaManager(stack, client);
            startAgents(stack, client);
            return startServices(stack, client).getId().intValue();
        } catch (ApiException e) {
            LOGGER.info("Couldn't start Cloudera Manager services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public ClusterStatus getStatus(boolean blueprintPresent) {
        return null;
    }

    @Override
    public Map<String, String> getComponentsByCategory(String blueprintName, String hostGroupName) {
        return null;
    }

    @Override
    public String getStackRepositoryJson(StackRepoDetails repoDetails, String stackRepoId) {
        return null;
    }

    private ApiCommand startServices(Stack stack, ApiClient client) throws ApiException, CloudbreakException {
        Cluster cluster = stack.getCluster();
        ClustersResourceApi apiInstance = new ClustersResourceApi(client);
        String clusterName = cluster.getName();
        LOGGER.debug("Starting all services for cluster.");
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                        cloudbreakMessagesService.getMessage(ClouderaManagerMessages.CM_CLUSTER_SERVICES_STARTING.code()));
        ApiCommand apiCommand = apiInstance.startCommand(clusterName);
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingService(stack, client, apiCommand.getId());
        if (isExited(pollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
        } else if (isTimeout(pollingResult)) {
            throw new CloudbreakException("Timeout while stopping Cloudera Manager services.");
        }
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                        cloudbreakMessagesService.getMessage(ClouderaManagerMessages.CM_CLUSTER_SERVICES_STARTED.code()));
        return apiCommand;
    }

    private void startAgents(Stack stack, ApiClient client) {
        LOGGER.debug("Starting Cloudera Manager agents on the hosts.");
        PollingResult hostsJoinedResult = clouderaManagerPollingServiceProvider.hostsPollingService(stack, client);
        if (isExited(hostsJoinedResult)) {
            throw new CancellationException("Cluster was terminated while starting Cloudera Manager agents.");
        }
    }

    private void startClouderaManager(Stack stack, ApiClient client) throws CloudbreakException {
        PollingResult healthCheckResult = clouderaManagerPollingServiceProvider.clouderaManagerStartupPollerObjectPollingService(stack, client);
        if (isExited(healthCheckResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Cloudera Manager to start.");
        } else if (isTimeout(healthCheckResult)) {
            throw new CloudbreakException("Cloudera Manager server was not restarted properly.");
        }
    }

    @Override
    public Map<String, String> gatherInstalledComponents(String hostname) {
        return Map.of();
    }

    @Override
    public void stopComponents(Map<String, String> components, String hostname) throws CloudbreakException {

    }

    @Override
    public void ensureComponentsAreStopped(Map<String, String> components, String hostname) throws CloudbreakException {

    }

    @Override
    public void initComponents(Map<String, String> components, String hostname) throws CloudbreakException {

    }

    @Override
    public void installComponents(Map<String, String> components, String hostname) throws CloudbreakException {

    }

    @Override
    public void regenerateKerberosKeytabs(String hostname) throws CloudbreakException {

    }

    @Override
    public void startComponents(Map<String, String> components, String hostname) throws CloudbreakException {

    }

    @Override
    public void restartAll() throws CloudbreakException {

    }
}
