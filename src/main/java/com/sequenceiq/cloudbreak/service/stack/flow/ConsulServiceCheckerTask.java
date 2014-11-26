package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

public class ConsulServiceCheckerTask implements StatusCheckerTask<ConsulService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulServiceCheckerTask.class);

    @Override
    public boolean checkStatus(ConsulService consulService) {
        ConsulClient consul = consulService.getConsulClient();
        String serviceName = consulService.getServiceName();
        try {
            List<CatalogService> services = consul.getCatalogService(serviceName, QueryParams.DEFAULT).getValue();
            return services.size() > 0;
        } catch (Exception e) {
            LOGGER.info("Consul service '{}' is not registered yet", serviceName);
            return false;
        }
    }

    @Override
    public void handleTimeout(ConsulService t) {
        throw new InternalServerException(String.format("Operation timed out. Consul service is not registered %s", t.getServiceName()));
    }

    @Override
    public String successMessage(ConsulService t) {
        return String.format("Consul service successfully registered '%s'", t.getServiceName());
    }

}
