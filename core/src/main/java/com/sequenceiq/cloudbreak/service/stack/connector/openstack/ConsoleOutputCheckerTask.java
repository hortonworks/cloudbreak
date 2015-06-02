package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.OSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component("openStackConsoleOutputCheckerTask")
@Qualifier("openstack")
public class ConsoleOutputCheckerTask extends StackBasedStatusCheckerTask<ConsoleOutputContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleOutputCheckerTask.class);
    private static final String EOF_INDICATOR = "login:";

    @Override
    public boolean checkStatus(ConsoleOutputContext consoleOutputContext) {
        String instanceId = consoleOutputContext.getInstanceId();
        LOGGER.info("Trying to retrieve console output of gateway instance, id: {}", instanceId);
        OSClient osClient = consoleOutputContext.getOsClient();
        String output = osClient.compute().servers().getConsoleOutput(instanceId, OpenStackConnector.CONSOLE_OUTPUT_LINES);
        if (StringUtils.isEmpty(output) || !output.contains(EOF_INDICATOR)) {
            LOGGER.info("Console output is not ready yet for gateway instance, id: {}", instanceId);
            return false;
        }
        return true;
    }

    @Override
    public void handleTimeout(ConsoleOutputContext consoleOutputContext) {
        LOGGER.error("Operation timed out: Couldn't get console output of gateway instance.");
    }

    @Override
    public String successMessage(ConsoleOutputContext consoleOutputContext) {
        return "Console output of instance successfully retrieved.";
    }
}
