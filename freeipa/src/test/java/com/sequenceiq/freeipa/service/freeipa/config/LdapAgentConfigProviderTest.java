package com.sequenceiq.freeipa.service.freeipa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

class LdapAgentConfigProviderTest {

    private final LdapAgentConfigProvider underTest = new LdapAgentConfigProvider();

    @Test
    void testGenerateConfig() {
        Map<String, SaltPillarProperties> result = underTest.generateConfig("test.dom.org");

        SaltPillarProperties properties = result.get("ldapagent");
        assertEquals("/ldapagent/init.sls", properties.getPath());
        Map<String, Object> ldapConfigMap = (Map<String, Object>) properties.getProperties().get("ldapagent");
        assertEquals(4, ldapConfigMap.size());
        assertEquals("dc=test,dc=dom,dc=org", ldapConfigMap.get("baseDn"));
        assertEquals(6080, ldapConfigMap.get("port"));
        assertEquals("localhost", ldapConfigMap.get("host"));
        assertEquals("localhost", ldapConfigMap.get("ldapHost"));
    }
}