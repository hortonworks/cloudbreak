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
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

public class CMUserRotationContextProviderTestUtils {

    private CMUserRotationContextProviderTestUtils() {

    }

    public static void testGetContext(StackDtoService stackService, SecretType secretType,
            CMUserRotationContextProvider underTest) throws IllegalAccessException {
        StackDto stack = new StackDto();
        ClusterView cluster = new Cluster() {

            @Override
            public Secret getDpClusterManagerUserSecret() {
                return createSecret("dpuser");
            }

            @Override
            public Secret getDpClusterManagerPasswordSecret() {
                return createSecret("dppassword");
            }

            @Override
            public Secret getDpAmbariUserSecret() {
                return createSecret("dpolduser");
            }

            @Override
            public Secret getDpAmbariPasswordSecret() {
                return createSecret("dpoldpassword");
            }

            @Override
            public Secret getCloudbreakClusterManagerUserSecretObject() {
                return createSecret("cbuser");
            }

            @Override
            public Secret getCloudbreakClusterManagerPasswordSecretObject() {
                return createSecret("cbpassword");
            }

            @Override
            public Secret getCloudbreakAmbariUserSecret() {
                return createSecret("cbolduser");
            }

            @Override
            public Secret getCloudbreakAmbariPasswordSecret() {
                return createSecret("cboldpassword");
            }
        };
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

    private static Secret createSecret(String secret) {
        try {
            return new Secret(secret, JsonUtil.writeValueAsString(new VaultSecret(secret, secret, secret)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
