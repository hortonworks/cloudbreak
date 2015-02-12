package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class ConsulAgentLeaveCheckerTask extends StackBasedStatusCheckerTask<ConsulContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulAgentLeaveCheckerTask.class);

    @Override
    public boolean checkStatus(ConsulContext consulContext) {
        MDCBuilder.buildMdcContext(consulContext.getStack());
        String nodeName = consulContext.getTargets().get(0);
        List<ConsulClient> clients = consulContext.getConsulClients();
        LOGGER.info("Trying to remove node: {} from consul", nodeName);
        ConsulUtils.agentForceLeave(clients, nodeName);
        Collection<String> leftMembers = ConsulUtils.getLeftMembers(clients).values();
        return leftMembers.contains(nodeName);
    }

    @Override
    public void handleTimeout(ConsulContext t) {
        throw new InternalServerException(String.format("Operation timed out. Consul agents didn't leave in time %s", t.getTargets()));
    }

    @Override
    public String successMessage(ConsulContext t) {
        return String.format("Consul agent successfully left '%s'", t.getTargets());
    }

}
