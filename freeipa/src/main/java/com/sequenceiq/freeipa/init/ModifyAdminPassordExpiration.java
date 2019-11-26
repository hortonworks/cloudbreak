package com.sequenceiq.freeipa.init;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaPostInstallService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class ModifyAdminPassordExpiration implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyAdminPassordExpiration.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private AsyncTaskExecutor asyncTaskExecutor;

    @Inject
    private FreeIpaPostInstallService freeIpaPostInstallService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        asyncTaskExecutor.submit(this::modifyPasswordExpirationForAllStack);
    }

    private void modifyPasswordExpirationForAllStack() {
        try {
            MDCBuilder.addRequestId(UUID.randomUUID().toString());
            List<Stack> stacks = stackService.findAllRunning();
            for (Stack stack : stacks) {
                try {
                    MDCBuilder.buildMdcContext(stack);
                    LOGGER.debug(String.format("Start of admin password expiration setting for stack [%s]", stack.getName()));
                    FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);
                    freeIpaPostInstallService.modifyAdminPasswordExpirationIfNeeded(client);
                } catch (Exception e) {
                    LOGGER.error("Failed to set admin password expiration for stack [{}]", stack.getName(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error during setting FreeIPA admin password", e);
        }
    }
}
