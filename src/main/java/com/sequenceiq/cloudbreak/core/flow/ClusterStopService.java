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

@Service
public class ClusterStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStopService.class);

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

    public FlowContext stop(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext startContext = (StackStatusUpdateContext) context;
        long stackId = startContext.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        MDCBuilder.buildMdcContext(cluster);

        cloudbreakEventService.fireCloudbreakEvent(stackId, Status.STOP_IN_PROGRESS.name(), "Services are stopping.");
        boolean stopped = ambariClusterConnector.stopCluster(stack);
        if (stopped) {
            cluster.setStatus(Status.STOPPED);
            cloudbreakEventService.fireCloudbreakEvent(stackId, Status.AVAILABLE.name(), "Services have been stopped successfully.");
            if (Status.STOP_REQUESTED.equals(stackRepository.findOneWithLists(stackId).getStatus())) {
                LOGGER.info("Hadoop services stopped, stack stop requested.");
            }
        } else {
            throw new CloudbreakException("Failed to stop cluster, some of the services could not stopped.");
        }
        clusterRepository.save(cluster);
        return context;
    }

    public FlowContext handleClusterStopError(FlowContext context) {
        StackStatusUpdateContext startContext = (StackStatusUpdateContext) context;
        long stackId = startContext.getStackId();
        LOGGER.info("Failed to stop Hadoop services, setting cluster state to: {}", Status.AVAILABLE);
        stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, startContext.getStatusReason());
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        cluster.setStatus(Status.AVAILABLE);
        clusterRepository.save(cluster);
        return context;
    }
}
