package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_DEMO_SECRET;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class DatahubDemoSecretRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubDemoSecretRotationContextProvider.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CUSTOM_JOB, CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withRotationJob(userCrnBasedJob(resourceCrn, "dh.rotation"))
                .withRollbackJob(userCrnBasedJob(resourceCrn, "dh.rollback"))
                .withFinalizeJob(userCrnBasedJob(resourceCrn, "dh.finalize"))
                .build());
    }

    @Override
    public SecretType getSecret() {
        return DATAHUB_DEMO_SECRET;
    }

    private Runnable userCrnBasedJob(String resourceCrn, String command) {
        return () -> {
            StackDto stackDto = stackDtoService.getByCrn(resourceCrn);
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
            try {
                hostOrchestrator.runCommandOnAllHosts(primaryGatewayConfig, command);
            } catch (Exception e) {
                LOGGER.info("{} salt command failed.", command);
                throw new CloudbreakServiceException(String.format("%s salt command failed.", command));
            }
        };
    }
}