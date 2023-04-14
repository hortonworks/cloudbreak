package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretLocationType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class CBCMAdminPasswordRotationContextProviderTest {

    @Mock
    private StackDtoService stackService;

    @InjectMocks
    private CBCMAdminPasswordRotationContextProvider underTest;

    @Test
    public void testGetContext() throws IllegalAccessException {
        StackDto stack = new StackDto();
        Cluster cluster = new Cluster();
        FieldUtils.writeDeclaredField(cluster, "cloudbreakClusterManagerUser", new Secret("cbuser", "cbuser"), true);
        FieldUtils.writeDeclaredField(cluster, "cloudbreakClusterManagerPassword", new Secret("cbpassword", "cbpassword"), true);
        FieldUtils.writeDeclaredField(cluster, "cloudbreakAmbariUser", new Secret("cbolduser", "cbolduser"), true);
        FieldUtils.writeDeclaredField(cluster, "cloudbreakAmbariPassword", new Secret("cboldpassword", "cboldpassword"), true);
        FieldUtils.writeDeclaredField(cluster, "dpClusterManagerUser", new Secret("dpuser", "dpuser"), true);
        FieldUtils.writeDeclaredField(cluster, "dpClusterManagerPassword", new Secret("dppassword", "dppassword"), true);
        FieldUtils.writeDeclaredField(stack, "cluster", cluster, true);
        Stack stackView = new Stack();
        stackView.setResourceCrn("resource");
        FieldUtils.writeDeclaredField(stack, "stack", stackView, true);
        when(stackService.getByCrn(anyString())).thenReturn(stack);

        Map<SecretLocationType, RotationContext> contexts = underTest.getContexts("resource");
        SecretType.CLOUDBREAK_CM_ADMIN_PASSWORD.getRotations().forEach(rotationType -> {
            assertTrue(contexts.containsKey(rotationType));
            assertNotNull(contexts.get(rotationType));
        });
    }
}
