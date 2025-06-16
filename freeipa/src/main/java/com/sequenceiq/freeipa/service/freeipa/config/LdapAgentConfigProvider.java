package com.sequenceiq.freeipa.service.freeipa.config;

import static java.util.Collections.singletonMap;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.util.LdapAgentAvailabilityChecker;

@Component
public class LdapAgentConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapAgentConfigProvider.class);

    private static final String LDAP_AGENT_CONFIG_KEY = "ldapagent";

    private static final String LDAP_AGENT_PILLAR_PATH = "/ldapagent/init.sls";

    @Inject
    private LdapAgentAvailabilityChecker ldapAgentAvailabilityChecker;

    public Map<String, SaltPillarProperties> generateConfig(Stack stack, String domain) {
        Objects.requireNonNull(domain, "FreeIPA domain can't be null");
        StringJoiner dcJoiner = new StringJoiner(",dc=", "dc=", "");
        for (String domainPart :domain.split("\\.")) {
            dcJoiner.add(domainPart);
        }
        boolean ldapAgentTlsSupportAvailable = ldapAgentAvailabilityChecker.isLdapAgentTlsSupportAvailable(stack);
        LdapAgentConfigView ldapAgentConfigView = new LdapAgentConfigView(dcJoiner.toString(), ldapAgentTlsSupportAvailable);
        LOGGER.debug("Generated LDAP agent config: {}", ldapAgentConfigView);
        return Map.of(LDAP_AGENT_CONFIG_KEY,
                new SaltPillarProperties(LDAP_AGENT_PILLAR_PATH, singletonMap(LDAP_AGENT_CONFIG_KEY, ldapAgentConfigView.toMap())));
    }
}
