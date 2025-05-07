package com.sequenceiq.cloudbreak.service.ad;

import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;

@Service
public class AdCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdCleanupService.class);

    private static final String AD_CLEANUP_COMMAND = "/opt/salt/scripts/remove_cm_principals.sh";

    private static final String STATE = "sssd/ad-cleanup";

    private static final String AD_CLEANUP_NODES = "ad-cleanup-nodes";

    private static final int DEFAULT_RETRY = 5;

    private static final String KERBEROS = "kerberos";

    private static final String LDAP = "ldap";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    public List<String> cleanUpAd(Set<String> hostnames, StackDtoDelegate stack) throws CloudbreakOrchestratorFailedException {
        List errors = new ArrayList<>();
        if (hostnames == null || hostnames.isEmpty()) {
            LOGGER.info("No hostnames provided, skip AD Cleanup.");
            return errors;
        }
        Optional<KerberosConfig> kerberosConfigOptional = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName());
        if (kerberosConfigOptional.isEmpty()) {
            LOGGER.error("Kerberos config not found for environment: {}", stack.getEnvironmentCrn());
            errors.add("AD Cleanup failed. Kerberos config not found.");
        } else {
            KerberosConfig kerberosConfig = kerberosConfigOptional.get();
            Optional<LdapView> ldapView = ldapConfigService.get(stack.getEnvironmentCrn(), stack.getName());
            if (ldapView.isPresent()) {
                LdapView ldap = ldapView.get();
                OrchestratorStateParams orchestratorStateParams = createOrchestratorStateParams(stack);
                orchestratorStateParams.setStateParams(getStateParamsForCleanup(hostnames, kerberosConfig, ldap));
                hostOrchestrator.runOrchestratorState(orchestratorStateParams);
            } else {
                errors.add("AD Cleanup failed, LDAP config not found for stack: " + stack.getName());
            }
        }
        return errors;
    }

    private OrchestratorStateParams createOrchestratorStateParams(StackDtoDelegate stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        OrchestratorStateParams stateParameters = new OrchestratorStateParams();
        stateParameters.setPrimaryGatewayConfig(primaryGatewayConfig);
        stateParameters.setState(STATE);
        stateParameters.setTargetHostNames(Set.of(primaryGatewayConfig.getHostname()));
        OrchestratorStateRetryParams retryParams = new OrchestratorStateRetryParams();
        retryParams.setMaxRetry(DEFAULT_RETRY);
        retryParams.setMaxRetryOnError(DEFAULT_RETRY);
        stateParameters.setStateRetryParams(retryParams);
        LOGGER.debug("Created OrchestratorStateParams for running AD Cleanup: {}", stateParameters);
        return stateParameters;
    }

    private Map<String, Object> getStateParamsForCleanup(Set<String> hostnames, KerberosConfig kerberosConfig, LdapView ldap) {
        Map<String, Object> stateParams = new HashMap<>();
        stateParams.put(AD_CLEANUP_NODES, getHostnamesParam(hostnames));
        stateParams.put(KERBEROS, getKerberosParams(kerberosConfig));
        stateParams.put(LDAP, getLdapParams(ldap, (Map<String, String>) stateParams.get(KERBEROS)));
        return stateParams;
    }

    private Map<String, String> getHostnamesParam(Set<String> hostnames) {
        String allNodeFqdns = String.join(" ", hostnames);
        LOGGER.debug("Adding hostnames to Salt pillar: {}", allNodeFqdns);
        return singletonMap("all_hostnames", allNodeFqdns);
    }

    private Map<String, String> getKerberosParams(KerberosConfig kerberosConfig) {
        Map<String, String> kerberosConf = new HashMap<>();
        kerberosConf.put("realm", kerberosConfig.getRealm());
        kerberosConf.put("nameservers", kerberosConfig.getNameServers());
        kerberosConf.put("container-dn", kerberosConfig.getContainerDn());
        return kerberosConf;

    }

    private Map<String, String> getLdapParams(LdapView ldap, Map<String, String> kerberosParams) {
        Map<String, String> ldapConf = new HashMap<>();
        ldapConf.put("bindDn", ldap.getBindDn());
        ldapConf.put("bindPassword", ldap.getBindPassword());
        String protocol = ldap.getProtocol();
        Integer serverPort = ldap.getServerPort();
        String connectionUrlWithIp = protocol + "://" + kerberosParams.get("nameservers") + ":" + serverPort.toString();
        ldapConf.put("connectionURL", connectionUrlWithIp);
        return ldapConf;
    }
}
