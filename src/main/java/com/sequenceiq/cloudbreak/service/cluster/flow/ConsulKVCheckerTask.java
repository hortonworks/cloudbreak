package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.PluginFailureException;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils;

@Component
public class ConsulKVCheckerTask extends StackBasedStatusCheckerTask<ConsulKVCheckerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulKVCheckerTask.class);

    @Override
    public boolean checkStatus(ConsulKVCheckerContext context) {
        MDCBuilder.buildMdcContext(context.getStack());
        List<String> keys = context.getKeys();
        String expectedValue = context.getExpectedValue();
        String failValue = context.getFailValue();
        List<ConsulClient> clients = context.getConsulClients();
        LOGGER.info("Checking '{}' different hosts if keys in Consul's key-value store have the expected value '{}'", clients.size(), expectedValue);
        Set<String> failedKeys = new HashSet<>();
        int matchingKeys = 0;
        int notFoundKeys = 0;
        for (String key : keys) {
            String value = ConsulUtils.getKVValue(clients, key, null);
            if (value != null) {
                if (value.equals(failValue)) {
                    failedKeys.add(key);
                } else if (value.equals(expectedValue)) {
                    matchingKeys++;
                }
            } else {
                notFoundKeys++;
            }
        }
        LOGGER.info("Keys: [Total: {}, {}: {}, Not {}: {}, Not found: {}, {}: {}]",
                keys.size(),
                expectedValue, matchingKeys,
                expectedValue, keys.size() - matchingKeys - notFoundKeys - failedKeys.size(),
                notFoundKeys,
                failValue, failedKeys.size());
        if (!failedKeys.isEmpty()) {
            throw new PluginFailureException(String.format("Found failure signal at keys: %s", failedKeys));
        }
        return matchingKeys == keys.size();
    }

    @Override
    public void handleTimeout(ConsulKVCheckerContext ctx) {
        throw new PluginFailureException(String.format("Operation timed out. Keys not found or don't have the expected value '%s'.", ctx.getExpectedValue()));
    }

    @Override
    public String successMessage(ConsulKVCheckerContext ctx) {
        return String.format("All %s keys found and have the expected value '%s'.", ctx.getKeys().size(), ctx.getExpectedValue());
    }

}

