package com.sequenceiq.freeipa.service.freeipa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.util.LdapAgentAvailabilityChecker;

@ExtendWith(MockitoExtension.class)
class LdapAgentConfigProviderTest {

    @Mock
    private LdapAgentAvailabilityChecker ldapAgentAvailabilityChecker;

    @InjectMocks
    private LdapAgentConfigProvider underTest;

    @Test
    void testGenerateConfig() {
        Stack stack = new Stack();
        when(ldapAgentAvailabilityChecker.isLdapAgentTlsSupportAvailable(stack)).thenReturn(true);

        Map<String, SaltPillarProperties> result = underTest.generateConfig(stack, "test.dom.org");

        SaltPillarProperties properties = result.get("ldapagent");
        assertEquals("/ldapagent/init.sls", properties.getPath());
        Map<String, Object> ldapConfigMap = (Map<String, Object>) properties.getProperties().get("ldapagent");
        assertEquals(5, ldapConfigMap.size());
        assertEquals("dc=test,dc=dom,dc=org", ldapConfigMap.get("baseDn"));
        assertEquals(6080, ldapConfigMap.get("port"));
        assertEquals("localhost", ldapConfigMap.get("host"));
        assertEquals("localhost", ldapConfigMap.get("ldapHost"));
        assertTrue((Boolean) ldapConfigMap.get("useTls"));
    }
}