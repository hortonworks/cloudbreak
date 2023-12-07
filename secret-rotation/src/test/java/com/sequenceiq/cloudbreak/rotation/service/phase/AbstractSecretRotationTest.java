package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP2;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP3;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.TestRotationContext;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;

public abstract class AbstractSecretRotationTest {

    @Mock
    protected AbstractRotationExecutor executor;

    @Mock
    protected RotationContextProvider contextProvider;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeField(getUnderTest(), "rotationExecutorMap", Map.of(STEP, executor, STEP2, executor, STEP3, executor), true);
        FieldUtils.writeField(getUnderTest(), "rotationContextProviderMap", Map.of(TestSecretType.TEST, contextProvider), true);
        generateTestContexts();
    }

    private void generateTestContexts() {
        Map<SecretRotationStep, RotationContext> contextMap = Map.of(STEP, new TestRotationContext("resource"),
                STEP2, new TestRotationContext("resource"),
                STEP3, new TestRotationContext("resource"));
        lenient().when(contextProvider.getContextsWithProperties(any(), any())).then(invocation -> contextMap);
    }

    protected abstract Object getUnderTest();
}
