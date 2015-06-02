package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class ConsulServiceCheckerTask extends StackBasedStatusCheckerTask<ConsulContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulServiceCheckerTask.class);

    @Override
    public boolean checkStatus(ConsulContext consulContext) {
        String serviceName = consulContext.getTargets().get(0);
        ConsulClient client = consulContext.getConsulClient();
        LOGGER.info("Checking consul service registration of '{}'", serviceName);
        List<CatalogService> service = ConsulUtils.getService(Arrays.asList(client), serviceName);
        if (service.isEmpty()) {
            LOGGER.info("Consul service '{}' is not registered yet", serviceName);
            return false;
        } else {
            LOGGER.info("Consul service '{}' found on '{}'", serviceName, service.get(0).getNode());
            return true;
        }
    }

    @Override
    public void handleTimeout(ConsulContext t) {
        throw new CloudbreakServiceException(String.format("Operation timed out. Consul service is not registered %s", t.getTargets()));
    }

    @Override
    public String successMessage(ConsulContext t) {
        return String.format("Consul service successfully registered '%s'", t.getTargets());
    }

}