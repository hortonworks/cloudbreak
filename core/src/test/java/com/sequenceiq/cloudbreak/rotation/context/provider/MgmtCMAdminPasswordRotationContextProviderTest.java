package com.sequenceiq.cloudbreak.rotation.context.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class MgmtCMAdminPasswordRotationContextProviderTest {

    @Mock
    private StackDtoService stackService;

    @Mock
    private SecretService secretService;

    @InjectMocks
    private MgmtCMAdminPasswordRotationContextProvider underTest;

    @Test
    public void testGetContext() throws IllegalAccessException {
        CMUserRotationContextProviderTestUtils.testGetContext(stackService, CloudbreakSecretType.CLOUDBREAK_CM_ADMIN_PASSWORD, underTest);
    }
}
