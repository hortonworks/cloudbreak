package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DEMO_SECRET;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.FINALIZE_FAILURE_KEY;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.POSTVALIDATE_FAILURE_KEY;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.PREVALIDATE_FAILURE_KEY;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.ROLLBACK_FAILURE_KEY;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.ROTATION_FAILURE_KEY;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.getCustomJobRunnableByProperties;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class DatahubDemoSecretRotationContextProvider implements RotationContextProvider {

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContextsWithProperties(String resourceCrn, Map<String, String> additionalProperties) {
        return Map.of(VAULT, VaultRotationContext.builder()
                        .withResourceCrn(resourceCrn)
                        .withVaultPathSecretMap(Map.of(stackDtoService.getByCrn(resourceCrn).getCluster().getAttributesSecret().getSecret(), "{}"))
                        .build(),
                CUSTOM_JOB, CustomJobRotationContext.builder()
                        .withResourceCrn(resourceCrn)
                        .withRotationJob(getCustomJobRunnableByProperties(ROTATION_FAILURE_KEY, resourceCrn, additionalProperties))
                        .withRollbackJob(getCustomJobRunnableByProperties(ROLLBACK_FAILURE_KEY, resourceCrn, additionalProperties))
                        .withFinalizeJob(getCustomJobRunnableByProperties(FINALIZE_FAILURE_KEY, resourceCrn, additionalProperties))
                        .withPreValidateJob(getCustomJobRunnableByProperties(PREVALIDATE_FAILURE_KEY, resourceCrn, additionalProperties))
                        .withPostValidateJob(getCustomJobRunnableByProperties(POSTVALIDATE_FAILURE_KEY, resourceCrn, additionalProperties))
                        .build());
    }

    @Override
    public SecretType getSecret() {
        return DEMO_SECRET;
    }
}