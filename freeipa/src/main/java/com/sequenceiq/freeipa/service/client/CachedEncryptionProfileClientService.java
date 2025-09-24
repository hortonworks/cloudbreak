package com.sequenceiq.freeipa.service.client;

import static com.sequenceiq.freeipa.cache.EncryptionProfileCache.FREEIPA_ENCRYPTION_PROFILE_CACHE;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

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
    public EncryptionProfileResponse getByName(String encryptionProfileName) {
        LOGGER.debug("Retrieving encryption profile named {}", encryptionProfileName);
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> encryptionProfileEndpoint.getByName(encryptionProfileName));
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }

    @CacheEvict(value = FREEIPA_ENCRYPTION_PROFILE_CACHE, key = "#encryptionProfileName")
    public void evictCache(String encryptionProfileName) {
    }
}
