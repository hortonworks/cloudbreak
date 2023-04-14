package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretLocationType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

// TODO WIP
@ExtendWith(MockitoExtension.class)
public class ServicesDBPasswordsRotationContextProviderTest {

    @Mock
    private StackDtoService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @InjectMocks
    private ServicesDBPasswordsRotationContextProvider underTest;

    @Test
    public void testGetContext() throws IllegalAccessException {
        StackDto stack = new StackDto();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        FieldUtils.writeDeclaredField(stack, "cluster", cluster, true);
        when(stackService.getByCrn(anyString())).thenReturn(stack);

        RDSConfig rdsConfig1 = rdsConfig(1L, "user1", "pass1");
        RDSConfig rdsConfig2 = rdsConfig(2L, "user2", "pass2");
        when(rdsConfigService.findByClusterId(anyLong())).thenReturn(Set.of(rdsConfig1, rdsConfig2));

        Map<SecretLocationType, RotationContext> contexts = underTest.getContexts("resource");
        SecretType.SERVICES_DB_PASSWORDS.getRotations().forEach(rotationType -> {
            assertTrue(contexts.containsKey(rotationType));
            assertNotNull(contexts.get(rotationType));
            assertEquals(2, contexts.get(rotationType).getUserPasswordSecrets().size());
        });
    }

    private RDSConfig rdsConfig(Long id, String user, String password) throws IllegalAccessException {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(id);
        FieldUtils.writeDeclaredField(rdsConfig, "connectionUserName", new Secret(user, user), true);
        FieldUtils.writeDeclaredField(rdsConfig, "connectionPassword", new Secret(password, password), true);
        return rdsConfig;
    }
}
