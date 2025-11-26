package com.sequenceiq.freeipa.service.client;

import static com.sequenceiq.freeipa.cache.EncryptionProfileCache.FREEIPA_ENCRYPTION_PROFILE_CACHE;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
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

    @Cacheable(cacheNames = FREEIPA_ENCRYPTION_PROFILE_CACHE, key = "#encryptionProfileCrn != null ? #encryptionProfileCrn : 'cdp_default_fips_v1'")
    public EncryptionProfileResponse getByCrnOrDefaultIfEmpty(String encryptionProfileCrn) {
        LOGGER.debug("Retrieving encryption profile CRN {}", encryptionProfileCrn);
        try {
            if (StringUtils.isNotEmpty(encryptionProfileCrn) && Crn.isCrn(encryptionProfileCrn)) {
                return ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> encryptionProfileEndpoint.getByCrn(encryptionProfileCrn));
            } else {
                LOGGER.debug("Using default encryption profile");
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
}
