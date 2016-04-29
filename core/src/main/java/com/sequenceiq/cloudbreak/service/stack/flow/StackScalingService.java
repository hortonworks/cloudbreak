package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_FAILED;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.flow.service.AmbariHostsRemover;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariDecommissioner;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;

@Service
public class StackScalingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackScalingService.class);
    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;

    @Inject
    private StackService stackService;
    @Inject
    private PollingService<ConsulContext> consulPollingService;
    @Inject
    private ConsulAgentLeaveCheckerTask consulAgentLeaveCheckerTask;
    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private AmbariHostsRemover ambariHostsRemover;
    @Inject
    private InstanceGroupRepository instanceGroupRepository;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private HostMetadataRepository hostMetadataRepository;
    @Inject
    private TlsSecurityService tlsSecurityService;
    @Inject
    private AmbariClusterConnector ambariClusterConnector;
    @Inject
    private AmbariDecommissioner ambariDecommissioner;
    @Inject
    private ServiceProviderConnectorAdapter connector;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    private enum Msg {
        STACK_SCALING_HOST_DELETED("stack.scaling.host.deleted"),
        STACK_SCALING_HOST_DELETE_FAILED("stack.scaling.host.delete.failed"),
        STACK_SCALING_HOST_NOT_FOUND("stack.scaling.host.not.found"),
        STACK_SCALING_BILLING_CHANGED("stack.scaling.billing.changed");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    public Set<Resource> addInstances(Long stackId, String instanceGroupName, Integer scalingAdjustment) throws Exception {
        Stack stack = stackService.getById(stackId);
        return connector.addInstances(stack, scalingAdjustment, instanceGroupName);
    }

    public void removeHostmetadataIfExists(Stack stack, InstanceMetaData instanceMetaData, HostMetadata hostMetadata) {
        if (hostMetadata != null) {
            try {
                ambariDecommissioner.deleteHostFromAmbari(stack, hostMetadata);
                hostMetadataRepository.delete(hostMetadata);
                eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                        cloudbreakMessagesService.getMessage(Msg.STACK_SCALING_HOST_DELETED.code(),
                                Arrays.asList(instanceMetaData.getInstanceId())));
            } catch (Exception e) {
                LOGGER.error("Host cannot be deleted from cluster: ", e);
                eventService.fireCloudbreakEvent(stack.getId(), DELETE_FAILED.name(),
                        cloudbreakMessagesService.getMessage(Msg.STACK_SCALING_HOST_DELETE_FAILED.code(),
                                Arrays.asList(instanceMetaData.getInstanceId())));

            }
        } else {
            LOGGER.info("Host cannot be deleted because it is not exist: ", instanceMetaData.getInstanceId());
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SCALING_HOST_NOT_FOUND.code(),
                            Arrays.asList(instanceMetaData.getInstanceId())));

        }
    }

    public void updateRemovedResourcesState(Stack stack, Set<String> instanceIds, InstanceGroup instanceGroup) throws CloudbreakSecuritySetupException {
        int nodeCount = instanceGroup.getNodeCount() - instanceIds.size();
        instanceGroup.setNodeCount(nodeCount);
        instanceGroupRepository.save(instanceGroup);

        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), gatewayInstance.getPublicIpWrapper());
        ConsulClient client = ConsulUtils.createClient(clientConfig);

        for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
            if (instanceIds.contains(instanceMetaData.getInstanceId())) {
                removeAgentFromConsul(stack, client, instanceMetaData);
                long timeInMillis = Calendar.getInstance().getTimeInMillis();
                instanceMetaData.setTerminationDate(timeInMillis);
                instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                instanceMetaDataRepository.save(instanceMetaData);
            }
        }
        LOGGER.info("Successfully terminated metadata of instances '{}' in stack.", instanceIds);
        eventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_CHANGED.name(),
                cloudbreakMessagesService.getMessage(Msg.STACK_SCALING_BILLING_CHANGED.code()));
    }

    public Map<String, String> getUnusedInstanceIds(String instanceGroupName, Integer scalingAdjustment, Stack stack) {
        Map<String, String> instanceIds = new HashMap<>();

        int i = 0;
        for (InstanceMetaData metaData : stack.getInstanceGroupByInstanceGroupName(instanceGroupName).getInstanceMetaData()) {
            if (!metaData.getAmbariServer() && !metaData.getConsulServer()
                    && (metaData.isDecommissioned() || metaData.isUnRegistered() || metaData.isCreated() || metaData.isFailed())) {
                instanceIds.put(metaData.getInstanceId(), metaData.getDiscoveryFQDN());
                if (++i >= scalingAdjustment * -1) {
                    break;
                }
            }
        }
        return instanceIds;
    }

    private void removeAgentFromConsul(Stack stack, ConsulClient client, InstanceMetaData metaData) {
        String nodeName = metaData.getDiscoveryFQDN().replace(ConsulUtils.CONSUL_DOMAIN, "");
        consulPollingService.pollWithTimeoutSingleFailure(
                consulAgentLeaveCheckerTask,
                new ConsulContext(stack, client, Collections.singletonList(nodeName)),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS);
    }
}
