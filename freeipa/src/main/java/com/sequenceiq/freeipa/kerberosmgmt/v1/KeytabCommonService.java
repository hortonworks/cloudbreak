package com.sequenceiq.freeipa.kerberosmgmt.v1;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.RoleRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Keytab;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.KeytabCache;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.exception.KeytabCreationException;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class KeytabCommonService {

    public static final String ROLE_NOT_ALLOWED = "The role request is not allowed when retrieving a keytab";

    public static final String PRIVILEGE_DOES_NOT_EXIST = "At least one privilege in the role request does not exist.";

    private static final String EMPTY_REALM = "Failed to create service as realm was empty.";

    private static final String KEYTAB_GENERATION_FAILED = "Failed to create keytab.";

    private static final String KEYTAB_FETCH_FAILED = "Failed to fetch keytab.";

    private static final String HOST_ALLOW_FAILURE = "Request to allow the host keytab retrieval failed.";

    private static final String HOST_CREATION_FAILED = "Failed to create host.";

    private static final Logger LOGGER = LoggerFactory.getLogger(KeytabCommonService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private KeytabCacheService keytabCacheService;

    @Inject
    private KerberosMgmtRoleComponent roleComponent;

    public Stack getFreeIpaStackWithMdcContext(String envCrn, String accountId) {
        return stackService.getFreeIpaStackWithMdcContext(envCrn, accountId);
    }

    public String getRealm(Stack stack) {
        try {
            FreeIpa freeIpa = freeIpaService.findByStack(stack);
            if (StringUtils.isNotBlank(freeIpa.getDomain())) {
                LOGGER.debug("Realm of IPA Server: {}", freeIpa.getDomain().toUpperCase());
                return freeIpa.getDomain().toUpperCase();
            } else {
                LOGGER.warn("Domain in FreeIPA is empty");
                throw new KeytabCreationException(EMPTY_REALM);
            }
        } catch (NotFoundException notfound) {
            LOGGER.error("FreeIPA not found for stack", notfound);
            throw new KeytabCreationException(EMPTY_REALM);
        }
    }

    public KeytabCache getKeytab(String environmentCrn, String canonicalPrincipal, String hostName, FreeIpaClient ipaClient)
            throws FreeIpaClientException, KeytabCreationException {
        try {
            LOGGER.debug("Fetching keytab from FreeIPA");
            Keytab keytab = ipaClient.getKeytab(canonicalPrincipal);
            return keytabCacheService.saveOrUpdate(environmentCrn, canonicalPrincipal, hostName, keytab.getKeytab());
        } catch (RetryableFreeIpaClientException e) {
            LOGGER.error(KEYTAB_GENERATION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new RetryableFreeIpaClientException(KEYTAB_GENERATION_FAILED, e, new KeytabCreationException(KEYTAB_GENERATION_FAILED));
        } catch (FreeIpaClientException e) {
            LOGGER.error(KEYTAB_GENERATION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(KEYTAB_GENERATION_FAILED);
        }
    }

    public KeytabCache getExistingKeytab(String environmentCrn, String canonicalPrincipal, String hostName, FreeIpaClient ipaClient)
            throws FreeIpaClientException, KeytabCreationException {
        try {
            Optional<KeytabCache> keytabCache = keytabCacheService.findByEnvironmentCrnAndPrincipal(environmentCrn, canonicalPrincipal);
            if (keytabCache.isPresent()) {
                LOGGER.debug("Returning keytab from cache");
                return keytabCache.get();
            } else {
                LOGGER.debug("Keytab is not found in cache, fetching existing from FreeIPA");
                Keytab keytab = ipaClient.getExistingKeytab(canonicalPrincipal);
                return keytabCacheService.saveOrUpdate(environmentCrn, canonicalPrincipal, hostName, keytab.getKeytab());
            }
        } catch (RetryableFreeIpaClientException e) {
            LOGGER.error(KEYTAB_FETCH_FAILED + " " + e.getLocalizedMessage(), e);
            throw new RetryableFreeIpaClientException(KEYTAB_FETCH_FAILED, e, new KeytabCreationException(KEYTAB_FETCH_FAILED));
        } catch (FreeIpaClientException e) {
            LOGGER.error(KEYTAB_FETCH_FAILED + " " + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(KEYTAB_FETCH_FAILED);
        }
    }

    public Host addHost(String hostname, RoleRequest roleRequest, FreeIpaClient ipaClient) throws FreeIpaClientException, KeytabCreationException {
        try {
            Host host = fetchOrCreateHost(hostname, ipaClient);
            allowHostKeytabRetrieval(hostname, ipaClient);
            roleComponent.addRoleAndPrivileges(Optional.empty(), Optional.of(host), roleRequest, ipaClient);
            return host;
        } catch (RetryableFreeIpaClientException e) {
            LOGGER.error(HOST_CREATION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new RetryableFreeIpaClientException(HOST_CREATION_FAILED, e, new KeytabCreationException(HOST_CREATION_FAILED));
        } catch (FreeIpaClientException e) {
            LOGGER.error(HOST_CREATION_FAILED + " " + e.getLocalizedMessage(), e);
            throw new KeytabCreationException(HOST_CREATION_FAILED);
        }
    }

    private void allowHostKeytabRetrieval(String fqdn, FreeIpaClient ipaClient) throws FreeIpaClientException {
        try {
            ipaClient.allowHostKeytabRetrieval(fqdn, FreeIpaClientFactory.ADMIN_USER);
        } catch (FreeIpaClientException e) {
            LOGGER.error(HOST_ALLOW_FAILURE + " " + e.getLocalizedMessage(), e);
            throw e;
        }
    }

    private Host fetchOrCreateHost(String hostname, FreeIpaClient ipaClient) throws FreeIpaClientException {
        try {
            Optional<Host> optionalHost = fetchHostIfExists(hostname, ipaClient);
            LOGGER.debug("Fetch host: {}", optionalHost);
            return optionalHost.isEmpty() ? ipaClient.addHost(hostname) : optionalHost.get();
        } catch (RetryableFreeIpaClientException e) {
            throw e;
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                LOGGER.debug("Host [{}] was already created while trying to create it", hostname);
                return ipaClient.showHost(hostname);
            } else {
                LOGGER.error(HOST_CREATION_FAILED + " " + e.getLocalizedMessage(), e);
                throw new KeytabCreationException(HOST_CREATION_FAILED);
            }
        }
    }

    private Optional<Host> fetchHostIfExists(String hostname, FreeIpaClient ipaClient) throws FreeIpaClientException {
        return FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue(() -> ipaClient.showHost(hostname), "Host not found with FQDN: [{}]", hostname);
    }

    public String constructPrincipal(String serviceName, String hostName, String realm) {
        LOGGER.debug("Construct principal from servicename [{}], hostname [{}] and realm [{}]", serviceName, hostName, realm);
        return serviceName + "/" + hostName + "@" + realm;
    }
}
