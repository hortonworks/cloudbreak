package com.sequenceiq.freeipa.kerberosmgmt.v1;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.freeipa.entity.KeytabCache;
import com.sequenceiq.freeipa.repository.KeytabCacheRepository;

@Service
public class KeytabCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeytabCacheService.class);

    @Inject
    private KeytabCacheRepository keytabCacheRepository;

    public KeytabCache save(String environmentCrn, String principal, String hostname, String keytab) {
        String accountId = Crn.safeFromString(environmentCrn).getAccountId();
        KeytabCache keytabCache = new KeytabCache();
        keytabCache.setKeytab(keytab);
        keytabCache.setPrincipal(principal);
        keytabCache.setEnvironmentCrn(environmentCrn);
        keytabCache.setAccountId(accountId);
        keytabCache.setPrincipalHash(hashPrincipal(principal));
        keytabCache.setHostName(hostname);
        LOGGER.debug("Saving keytab in env [{}] for principal hash: [{}]", environmentCrn, keytabCache.getPrincipalHash());
        return keytabCacheRepository.save(keytabCache);
    }

    public KeytabCache saveOrUpdate(String environmentCrn, String principal, String hostname, String keytab) {
        Optional<KeytabCache> keytabCache = findByEnvironmentCrnAndPrincipal(environmentCrn, principal);
        if (keytabCache.isPresent()) {
            KeytabCache cached = keytabCache.get();
            if (Objects.equals(cached.getKeytab().getRaw(), keytab)) {
                LOGGER.debug("Keytab exists in cache with the same value");
                return cached;
            } else {
                LOGGER.debug("Keytab exists in cache with different value, updating");
                cached.setKeytab(keytab);
                return keytabCacheRepository.save(cached);
            }
        } else {
            LOGGER.debug("Keytab doesn't exist in cache, saving.");
            return save(environmentCrn, principal, hostname, keytab);
        }
    }

    public Optional<KeytabCache> findByEnvironmentCrnAndPrincipal(String environmentCrn, String principal) {
        return keytabCacheRepository.findByEnvironmentCrnAndPrincipalHash(environmentCrn, hashPrincipal(principal));
    }

    public int deleteByEnvironmentCrnAndPrincipal(String environmentCrn, String principal) {
        return keytabCacheRepository.deleteByEnvironmentCrnAndPrincipalHash(environmentCrn, hashPrincipal(principal));
    }

    public int deleteByEnvironmentCrnAndHostname(String environmentCrn, String hostName) {
        return keytabCacheRepository.deleteByEnvironmentCrnAndHostName(environmentCrn, hostName);
    }

    public int deleteByEnvironmentCrn(String environmentCrn) {
        return keytabCacheRepository.deleteByEnvironmentCrn(environmentCrn);
    }

    private String hashPrincipal(String principal) {
        return DigestUtils.sha256Hex(principal);
    }
}
