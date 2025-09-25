package com.sequenceiq.freeipa.service.client;

import static com.sequenceiq.freeipa.cache.EncryptionProfileCache.FREEIPA_ENCRYPTION_PROFILE_CACHE;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;

@Service
public class CachedEncryptionProfileClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedEncryptionProfileClientService.class);

    @Inject
    private EncryptionProfileEndpoint encryptionProfileEndpoint;

    @Inject
    private WebApplicationExceptionHandler webApplicationExceptionHandler;

    @Cacheable(cacheNames = FREEIPA_ENCRYPTION_PROFILE_CACHE, key = "#encryptionProfileName")
    public EncryptionProfileResponse getByNameOrDefaultIfEmpty(String encryptionProfileName) {
        LOGGER.debug("Retrieving encryption profile named {}", encryptionProfileName);
        try {
            if (StringUtils.isNotEmpty(encryptionProfileName)) {
                return ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> encryptionProfileEndpoint.getByName(encryptionProfileName));
            } else {
                return getDefaultEncryptionProfile();
            }
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }

    private EncryptionProfileResponse getDefaultEncryptionProfile() {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> encryptionProfileEndpoint.getDefaultEncryptionProfile());
    }

    @CacheEvict(value = FREEIPA_ENCRYPTION_PROFILE_CACHE, key = "#encryptionProfileName")
    public void evictCache(String encryptionProfileName) {
    }
}
