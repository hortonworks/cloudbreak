package com.sequenceiq.cloudbreak.core.flow;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.core.flow.context.SwarmContext;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class SwarmCheckerTask extends StackBasedStatusCheckerTask<SwarmContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwarmCheckerTask.class);

    @Override
    public boolean checkStatus(SwarmContext swarmContext) {
        LOGGER.info("Checking if Swarm manager is available and if the agents are registered.");
        try {
            List<Object> driverStatus = swarmContext.getDockerClient().infoCmd().exec().getDriverStatuses();
            LOGGER.debug("Swarm manager is available, checking registered agents.");
            for (Object element : driverStatus) {
                try {
                    List objects = (ArrayList) element;
                    if (objects.get(0).toString().endsWith("Nodes") && Integer.valueOf(objects.get(1).toString()) == swarmContext.getNodeCount()) {
                        return true;
                    }
                } catch (Exception e) {
                    LOGGER.error(String.format("Docker info returned an unexpected element: %s", element), e);
                }
            }
        } catch (Throwable t) {
            return false;
        }
        return false;
    }

    @Override
    public void handleTimeout(SwarmContext t) {
        throw new InternalServerException("Operation timed out. Swarm manager couldn't start or the agents didn't join in time.");
    }

    @Override
    public String successMessage(SwarmContext t) {
        return String.format("Swarm is available and the agents are registered.");
    }
}
