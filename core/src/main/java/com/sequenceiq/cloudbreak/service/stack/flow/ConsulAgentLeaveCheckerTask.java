package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class ConsulAgentLeaveCheckerTask extends StackBasedStatusCheckerTask<ConsulContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulAgentLeaveCheckerTask.class);

    @Override
    public boolean checkStatus(ConsulContext consulContext) {
        String nodeName = consulContext.getTargets().get(0);
        ConsulClient client = consulContext.getConsulClient();
        LOGGER.info("Trying to remove node: {} from consul", nodeName);
        ConsulUtils.agentForceLeave(Arrays.asList(client), nodeName);
        Collection<String> leftMembers = ConsulUtils.getLeftMembers(Arrays.asList(client)).values();
        return leftMembers.contains(nodeName);
    }

    @Override
    public void handleTimeout(ConsulContext t) {
        throw new CloudbreakServiceException(String.format("Operation timed out. Consul agents didn't leave in time %s", t.getTargets()));
    }

    @Override
    public String successMessage(ConsulContext t) {
        return String.format("Consul agent successfully left '%s'", t.getTargets());
    }

}
