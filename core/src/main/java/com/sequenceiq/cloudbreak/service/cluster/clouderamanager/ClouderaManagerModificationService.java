package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isTimeout;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STOPPED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STOPPING;

import java.util.Collection;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Service
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

    @Override
    public void upscaleCluster(Stack stack, HostGroup hostGroup, Collection<HostMetadata> hostMetadata) throws CloudbreakException {

    }

    @Override
    public void stopCluster(Stack stack) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        ApiClient client = clouderaManagerClientFactory.getClient(stack, cluster);
        ClustersResourceApi clustersResourceApi = new ClustersResourceApi(client);
        try {
            LOGGER.debug("Stop all Hadoop services");
            eventService
                    .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                            cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STOPPING.code()));
            ApiCommand apiCommand = clustersResourceApi.stopCommand(cluster.getName());
            PollingResult pollingResult = clouderaManagerPollingServiceProvider.stopPollingService(stack, client, apiCommand.getId());
            if (isExited(pollingResult)) {
                throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
            } else if (isTimeout(pollingResult)) {
                throw new CloudbreakException("Timeout while stopping Ambari services.");
            }
            eventService
                    .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                            cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STOPPED.code()));
        } catch (ApiException e) {
            LOGGER.info("Couldn't stop ClouderaManager services", e);
            throw new AmbariOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public int startCluster(Stack stack) throws CloudbreakException {
        return 0;
    }
}
