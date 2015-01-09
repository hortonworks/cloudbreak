package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.PluginFailureException;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils;

public class ConsulKVCheckerTask implements StatusCheckerTask<ConsulKVCheckerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulKVCheckerTask.class);

    @Override
    public boolean checkStatus(ConsulKVCheckerContext context) {
        MDCBuilder.buildMdcContext(context.getStack());
        List<String> keys = context.getKeys();
        String expectedValue = context.getExpectedValue();
        List<ConsulClient> clients = context.getConsulClients();
        LOGGER.info("Checking '{}' different hosts if keys in Consul's key-value store have the expected value", clients.size(), keys, expectedValue);
        boolean keysFound = true;
        StringBuilder result = new StringBuilder();
        for (String key : keys) {
            String value = ConsulUtils.getKVValue(clients, key, null);
            if (value != null) {
                if (!value.equals(expectedValue)) {
                    result.append(String.format("Key '%s' found in KV store, but doesn't match the expected value.", key)).append("\n");
                    keysFound = false;
                } else {
                    result.append(String.format("Key '%s' found in KV store, and matches the expected value!", key)).append("\n");
                }
            } else {
                result.append(String.format("Key '%s' cannot be found in KV store!", key)).append("\n");
                keysFound = false;
            }
        }
        LOGGER.info(result.toString());
        return keysFound;
    }

    @Override
    public void handleTimeout(ConsulKVCheckerContext ctx) {
        throw new PluginFailureException(String.format("Operation timed out. Keys '%s' not found or don't have the expected value '%s'.",
                ctx.getKeys(), ctx.getExpectedValue()));
    }

    @Override
    public String successMessage(ConsulKVCheckerContext ctx) {
        return String.format("Keys '%s' found and have the expected value '%s'.", ctx.getKeys(), ctx.getExpectedValue());
    }

}

