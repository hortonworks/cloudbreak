package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

public class ConsulServiceCheckerTask implements StatusCheckerTask<ConsulContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulServiceCheckerTask.class);

    @Override
    public boolean checkStatus(ConsulContext consulContext) {
        MDCBuilder.buildMdcContext(consulContext.getStack());
        String serviceName = consulContext.getTargets().get(0);
        List<ConsulClient> clients = consulContext.getConsulClients();
        LOGGER.info("Checking '{}' different hosts for service registration of '{}'", clients.size(), serviceName);
        List<CatalogService> service = ConsulUtils.getService(clients, serviceName);
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
        throw new InternalServerException(String.format("Operation timed out. Consul service is not registered %s", t.getTargets()));
    }

    @Override
    public String successMessage(ConsulContext t) {
        return String.format("Consul service successfully registered '%s'", t.getTargets());
    }

}