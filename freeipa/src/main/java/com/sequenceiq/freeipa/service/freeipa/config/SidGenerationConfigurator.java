package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientService;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.host.Rhel8ClientHelper;

@Component
public class SidGenerationConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SidGenerationConfigurator.class);

    @Inject
    private Rhel8ClientHelper rhel8ClientHelper;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private RetryableFreeIpaClientService retryService;

    public void enableAndTriggerSidGeneration(Stack stack, FreeIpaClient freeIpaClient) {
        try {
            boolean clientConnectedToRhel8 = rhel8ClientHelper.isClientConnectedToRhel8(stack, freeIpaClient);
            if (clientConnectedToRhel8) {
                LOGGER.info("Enable and start SID generation");
                retryService.invokeWithRetries(freeIpaClient::enableAndTriggerSidGeneration);
            } else {
                Optional<String> rhel8Instance = rhel8ClientHelper.findRhel8Instance(stack);
                if (rhel8Instance.isPresent()) {
                    FreeIpaClient rhel8FreeIpaClient = retryService.invokeWithRetries(()
                            -> freeIpaClientFactory.getFreeIpaClientForInstance(stack, rhel8Instance.get()));
                    LOGGER.info("Enable and start SID generation on host: {}", rhel8Instance.get());
                    retryService.invokeWithRetries(rhel8FreeIpaClient::enableAndTriggerSidGeneration);
                } else {
                    LOGGER.warn("Couldn't found RHEL8 FreeIPA instance to enable and start SID generation");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Sid generation failed", e);
        }
    }
}
