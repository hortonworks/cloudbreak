package com.sequenceiq.cloudbreak.controller.validation.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentAttachV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;

public class EnvironmentAttachValidatorTest {

    private final EnvironmentAttachValidator environmentAttachValidator = new EnvironmentAttachValidator();

    @Test
    public void testValidation() {
        EnvironmentAttachV4Request request = getEnvironmentAttachRequest();
        Set<LdapConfig> ldapsToAttach = getLdapConfigs();
        Set<ProxyConfig> proxiesToAttach = getProxyConfigs();
        ValidationResult validationResult = environmentAttachValidator.validate(request, ldapsToAttach, proxiesToAttach);
        assertEquals(2, validationResult.getErrors().size());
        assertTrue(validationResult.getErrors().get(0).contains("ldap-nonexsistent"));
        assertTrue(validationResult.getErrors().get(1).contains("proxy-nonexistent1"));
        assertTrue(validationResult.getErrors().get(1).contains("proxy-nonexistent2"));
    }

    private EnvironmentAttachV4Request getEnvironmentAttachRequest() {
        EnvironmentAttachV4Request request = new EnvironmentAttachV4Request();
        request.getLdaps().add("ldap1");
        request.getLdaps().add("ldap2");
        request.getLdaps().add("ldap-nonexsistent");
        request.getProxies().add("proxy1");
        request.getProxies().add("proxy-nonexistent1");
        request.getProxies().add("proxy-nonexistent2");
        return request;
    }

    private Set<LdapConfig> getLdapConfigs() {
        Set<LdapConfig> ldapsToAttach = new HashSet<>();
        LdapConfig ldap1 = new LdapConfig();
        ldap1.setName("ldap1");
        ldap1.setId(1L);
        LdapConfig ldap2 = new LdapConfig();
        ldap2.setName("ldap2");
        ldap2.setId(2L);
        ldapsToAttach.add(ldap1);
        ldapsToAttach.add(ldap2);
        return ldapsToAttach;
    }

    private Set<ProxyConfig> getProxyConfigs() {
        Set<ProxyConfig> proxiesToAttach = new HashSet<>();
        ProxyConfig proxy1 = new ProxyConfig();
        proxy1.setName("proxy1");
        proxy1.setId(1L);
        proxiesToAttach.add(proxy1);
        return proxiesToAttach;
    }
}