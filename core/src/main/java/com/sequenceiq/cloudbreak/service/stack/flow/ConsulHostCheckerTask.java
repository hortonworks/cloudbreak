package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class ConsulHostCheckerTask extends StackBasedStatusCheckerTask<ConsulContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulHostCheckerTask.class);

    @Override
    public boolean checkStatus(ConsulContext consulContext) {
        List<String> privateIps = consulContext.getTargets();
        ConsulClient client = consulContext.getConsulClient();
        LOGGER.info("Checking consul agents: '{}'", privateIps);
        Map<String, String> members = ConsulUtils.getAliveMembers(Arrays.asList(client));
        for (String ip : privateIps) {
            if (members.get(ip) == null) {
                LOGGER.info("Consul agent didn't join on host: {}", ip);
                return false;
            }
        }
        return true;
    }

    @Override
    public void handleTimeout(ConsulContext t) {
        throw new CloudbreakServiceException(String.format("Operation timed out. Consul agents didn't join in time %s", t.getTargets()));
    }

    @Override
    public String successMessage(ConsulContext t) {
        return String.format("Consul agents successfully joined '%s'", t.getTargets());
    }

}
