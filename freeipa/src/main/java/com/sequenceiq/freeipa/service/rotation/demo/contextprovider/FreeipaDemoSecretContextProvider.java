package com.sequenceiq.freeipa.service.rotation.demo.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.FINALIZE_FAILURE_KEY;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.POSTVALIDATE_FAILURE_KEY;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.PREVALIDATE_FAILURE_KEY;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.ROLLBACK_FAILURE_KEY;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.ROTATION_FAILURE_KEY;
import static com.sequenceiq.cloudbreak.rotation.secret.demo.DemoSecretExceptionProviderUtil.getCustomJobRunnableByProperties;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.DEMO_SECRET;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;

@Component
public class FreeipaDemoSecretContextProvider implements RotationContextProvider {

    @Override
    public Map<SecretRotationStep, RotationContext> getContextsWithProperties(String resourceCrn, Map<String, String> additionalProperties) {
        return Map.of(CUSTOM_JOB, CustomJobRotationContext.builder()
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