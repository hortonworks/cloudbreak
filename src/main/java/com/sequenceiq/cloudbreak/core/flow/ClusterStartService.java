package com.sequenceiq.cloudbreak.core.flow;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ClusterStartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartService.class);

    @Autowired
    private AmbariClusterConnector ambariClusterConnector;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    public FlowContext start(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext startContext = (StackStatusUpdateContext) context;
        long stackId = startContext.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        MDCBuilder.buildMdcContext(cluster);
        boolean started = ambariClusterConnector.startCluster(stack);
        if (started) {
            LOGGER.info("Successfully started Hadoop services, setting cluster state to: {}", Status.AVAILABLE);
            cluster.setUpSince(new Date().getTime());
            cluster.setStatus(Status.AVAILABLE);
            cloudbreakEventService.fireCloudbreakEvent(stackId, Status.AVAILABLE.name(), "Cluster started successfully.");
        } else {
            throw new CloudbreakException("Failed to start cluster, some of the services could not started.");
        }
        clusterRepository.save(cluster);
        return context;
    }

    public FlowContext handleClusterStartError(FlowContext context) {
        StackStatusUpdateContext startContext = (StackStatusUpdateContext) context;
        LOGGER.info("Failed to start Hadoop services, setting cluster state to: {}", Status.STOPPED);
        Stack stack = stackRepository.findOneWithLists(startContext.getStackId());
        Cluster cluster = stack.getCluster();
        cluster.setStatus(Status.STOPPED);
        stackUpdater.updateStackStatus(startContext.getStackId(), Status.AVAILABLE, startContext.getStatusReason());
        clusterRepository.save(cluster);
        return context;
    }
}
