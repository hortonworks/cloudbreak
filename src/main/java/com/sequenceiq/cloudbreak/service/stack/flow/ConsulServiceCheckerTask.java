package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

public class ConsulServiceCheckerTask implements StatusCheckerTask<ConsulServiceContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulServiceCheckerTask.class);

    @Override
    public boolean checkStatus(ConsulServiceContext consulServiceContext) {
        MDCBuilder.buildMdcContext(consulServiceContext.getStack());
        String serviceName = consulServiceContext.getServiceName();
        List<ConsulClient> clients = consulServiceContext.getConsulClients();
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
    public void handleTimeout(ConsulServiceContext t) {
        throw new InternalServerException(String.format("Operation timed out. Consul service is not registered %s", t.getServiceName()));
    }

    @Override
    public String successMessage(ConsulServiceContext t) {
        return String.format("Consul service successfully registered '%s'", t.getServiceName());
    }

}
