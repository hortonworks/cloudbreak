package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;
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
        FieldUtils.writeField(getUnderTest(), "rotationExecutorMap", Map.of(STEP, executor), true);
        FieldUtils.writeField(getUnderTest(), "rotationContextProviderMap", Map.of(TestSecretType.TEST, contextProvider), true);
        generateTestContexts();
    }

    private void generateTestContexts() {
        Map<SecretRotationStep, RotationContext> contextMap = Map.of(STEP, new TestRotationContext("resource"));
        lenient().when(contextProvider.getContexts(any())).thenReturn(contextMap);
    }

    protected abstract Object getUnderTest();
}
