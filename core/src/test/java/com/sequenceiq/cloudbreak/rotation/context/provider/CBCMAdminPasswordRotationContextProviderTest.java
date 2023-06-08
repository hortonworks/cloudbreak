package com.sequenceiq.cloudbreak.rotation.context.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class CBCMAdminPasswordRotationContextProviderTest {

    @Mock
    private StackDtoService stackService;

    @InjectMocks
    private CBCMAdminPasswordRotationContextProvider underTest;

    @Test
    public void testGetContext() throws IllegalAccessException {
        CMUserRotationContextProviderTestUtils.testGetContext(stackService, CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD, underTest);
    }
}
