package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

@Component
@Scope("prototype")
public class DNDecommissionStatusCheckerTask implements StatusCheckerTask<AmbariOperations> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DNDecommissionStatusCheckerTask.class);

    @Autowired
    private StackRepository stackRepository;

    @Override
    public boolean checkStatus(AmbariOperations t) {
        MDCBuilder.buildMdcContext(t.getStack());
        AmbariClient ambariClient = t.getAmbariClient();
        Map<String, Long> dataNodes = ambariClient.getDecommissioningDataNodes();
        boolean finished = dataNodes.isEmpty();
        if (!finished) {
            LOGGER.info("DataNode decommission is in progress: {}", dataNodes);
        }
        return finished;
    }

    @Override
    public void handleTimeout(AmbariOperations t) {
        throw new IllegalStateException("DataNode decommission timed out");

    }

    @Override
    public boolean exitPoller(AmbariOperations ambariOperations) {
        try {
            Stack byId = stackRepository.findById(ambariOperations.getStack().getId());
            if (byId == null || byId.getStatus().equals(Status.DELETE_IN_PROGRESS)) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public String successMessage(AmbariOperations t) {
        return "Requested DataNode decommission operations completed";
    }

}
