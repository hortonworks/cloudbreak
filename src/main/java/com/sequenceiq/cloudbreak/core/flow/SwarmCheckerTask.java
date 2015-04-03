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
        LOGGER.info("Checking swarm if available.");
        try {
            List<Object> driverStatuses = swarmContext.getDockerClient().infoCmd().exec().getDriverStatuses();
            for (Object driverStatuse : driverStatuses) {
                try {
                    List objects = (ArrayList) driverStatuse;
                    if (objects.get(0).toString().endsWith("Nodes") && Integer.valueOf(objects.get(1).toString()) == swarmContext.getNodeCount()) {
                        return true;
                    }
                } catch (Exception ex) {
                    LOGGER.info("Type cast was not success when try to reach the swarm api.");
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }

    @Override
    public void handleTimeout(SwarmContext t) {
        throw new InternalServerException(String.format("Operation timed out. Could not reach swarm nodes in time."));
    }

    @Override
    public String successMessage(SwarmContext t) {
        return String.format("Swarm nodes available successfully ");
    }
}
