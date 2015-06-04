package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.model.SerialPortOutput;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class GcpConsoleOutputCheckerTask extends StackBasedStatusCheckerTask<GcpConsoleOutputContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpConsoleOutputCheckerTask.class);

    @Override
    public boolean checkStatus(GcpConsoleOutputContext consoleOutputContext) {
        try {
            SerialPortOutput execute = consoleOutputContext.getSerialPortOutput().execute();
            if (execute != null) {
                String[] split = execute.getContents().split("cb: -----BEGIN SSH HOST KEY FINGERPRINTS-----|cb: -----END SSH HOST KEY FINGERPRINTS-----");
                if (split.length > 3) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Console output is not yet available.");
            return false;
        }
        LOGGER.warn("Console output is not yet available.");
        return false;

    }

    @Override
    public void handleTimeout(GcpConsoleOutputContext consoleOutputContext) {
        LOGGER.error("Operation timed out: Couldn't get console output of gateway instance.");
    }

    @Override
    public String successMessage(GcpConsoleOutputContext consoleOutputContext) {
        return "Console output of instance successfully retrieved.";
    }
}