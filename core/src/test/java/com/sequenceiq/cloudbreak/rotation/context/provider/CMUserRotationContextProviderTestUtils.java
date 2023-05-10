package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

public class CMUserRotationContextProviderTestUtils {

    private CMUserRotationContextProviderTestUtils() {

    }

    public static void testGetContext(StackDtoService stackService, SecretType secretType,
            CMUserRotationContextProvider underTest) throws IllegalAccessException {
        StackDto stack = new StackDto();
        Cluster cluster = new Cluster();
        FieldUtils.writeDeclaredField(cluster, "cloudbreakClusterManagerUser", new Secret("cbuser", createSecret("cbuser")), true);
        FieldUtils.writeDeclaredField(cluster, "cloudbreakClusterManagerPassword", new Secret("cbpassword", createSecret("cbpassword")), true);
        FieldUtils.writeDeclaredField(cluster, "cloudbreakAmbariUser", new Secret("cbolduser", createSecret("cbolduser")), true);
        FieldUtils.writeDeclaredField(cluster, "cloudbreakAmbariPassword", new Secret("cboldpassword", createSecret("cboldpassword")), true);
        FieldUtils.writeDeclaredField(cluster, "dpClusterManagerUser", new Secret("dpuser", createSecret("dpuser")), true);
        FieldUtils.writeDeclaredField(cluster, "dpClusterManagerPassword", new Secret("dppassword", createSecret("dppassword")), true);
        FieldUtils.writeDeclaredField(stack, "cluster", cluster, true);
        Stack stackView = new Stack();
        stackView.setResourceCrn("resource");
        FieldUtils.writeDeclaredField(stack, "stack", stackView, true);
        when(stackService.getByCrn(anyString())).thenReturn(stack);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts("resource");
        secretType.getSteps().forEach(step -> {
            assertTrue(contexts.containsKey(step));
            assertNotNull(contexts.get(step));
        });
    }

    private static String createSecret(String secret) {
        try {
            return JsonUtil.writeValueAsString(new VaultSecret(secret, secret, secret));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
