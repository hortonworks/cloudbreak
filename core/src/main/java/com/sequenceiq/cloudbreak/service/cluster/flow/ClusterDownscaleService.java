package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterDownscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDownscaleService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariDecommissioner ambariDecommissioner;

    public Set<String> decommission(Long stackId, String hostGroupName, Integer scalingAdjustment) throws CloudbreakException {
        Stack stack = stackService.getById(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Start decommission");
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
        return ambariDecommissioner.decommissionAmbariNodes(stack, hostGroupName, scalingAdjustment);
    }

    public void updateMetadata(Long stackId, Set<String> hostNames) {
        Stack stack = stackService.getById(stackId);
        if (!CloudConstants.BYOS.equals(stack.cloudPlatform())) {
            MDCBuilder.buildMdcContext(stack);
            LOGGER.info("Start updating metadata");
            for (String hostName : hostNames) {
                stackService.updateMetaDataStatus(stack.getId(), hostName, InstanceStatus.DECOMMISSIONED);
            }
        }
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
    }
}