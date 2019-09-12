package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

public class CleanupFreeIpaTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupFreeIpaTask.class);

    private static final String KERBEROS_USER_PREFIX = "krbbind-";

    private static final String KEYTAB_USER_PREFIX = "kerberosbind-";

    private static final String LDAP_USER_PREFIX = "ldapbind-";

    private static final String ROLE_NAME_PREFIX = "hadoopadminrole-";

    private final Stack stack;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final String userCrn;

    private final Map<String, String> mdcContextMap;

    private final ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    public CleanupFreeIpaTask(Stack stack, FreeIpaV1Endpoint freeIpaV1Endpoint, ThreadBasedUserCrnProvider threadBasedUserCrnProvider, String userCrn,
            Map<String, String> mdcContextMap) {
        this.stack = stack;
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
        this.threadBasedUserCrnProvider = threadBasedUserCrnProvider;
        this.userCrn = userCrn;
        this.mdcContextMap = mdcContextMap;
    }

    @Override
    public void run() {
        MDCBuilder.buildMdcContextFromMap(mdcContextMap);
        threadBasedUserCrnProvider.setUserCrn(userCrn);
        Set<String> fqdns = stack.getInstanceMetaDataAsList().stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toSet());
        CleanupRequest cleanupRequest = new CleanupRequest();
        cleanupRequest.setHosts(fqdns);
        cleanupRequest.setEnvironmentCrn(stack.getEnvironmentCrn());
        cleanupRequest.setUsers(Set.of(KERBEROS_USER_PREFIX + stack.getName(), KEYTAB_USER_PREFIX + stack.getName(), LDAP_USER_PREFIX + stack.getName()));
        cleanupRequest.setRoles(Set.of(ROLE_NAME_PREFIX + stack.getName()));
        LOGGER.info("Sending cleanup request to FreeIPA: [{}]", cleanupRequest);
        try {
            CleanupResponse cleanupResponse = freeIpaV1Endpoint.cleanup(cleanupRequest);
            LOGGER.info("FreeIPA cleanup finished: [{}]", cleanupResponse);
        } catch (Exception e) {
            LOGGER.error("FreeIPA cleanup failed", e);
        }
    }
}
