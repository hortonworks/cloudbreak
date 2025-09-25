package com.sequenceiq.environment.encryptionprofile.v1.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.defaults.CrnsByCategory;
import com.sequenceiq.environment.encryptionprofile.cache.DefaultEncryptionProfileProvider;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@ExtendWith(MockitoExtension.class)
public class DefaultEncryptionProfileCheckerTest {
    private static final String DEFAULT_ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default";

    private static final String CUSTOM_ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:account:encryptionProfile:custom";

    @Mock
    private DefaultEncryptionProfileProvider defaultEncryptionProfileProvider;

    @InjectMocks
    private DefaultEncryptionProfileChecker underTest;

    @BeforeEach
    public void setUp() {
        Map<String, EncryptionProfile> defaultEncryptionProfiles = Map.of(DEFAULT_ENCRYPTION_PROFILE_CRN, new EncryptionProfile());
        lenient().when(defaultEncryptionProfileProvider.defaultEncryptionProfilesByCrn()).thenReturn(defaultEncryptionProfiles);
    }

    @Test
    public void testGetResourceType() {
        AuthorizationResourceType result = underTest.getResourceType();
        assertEquals(AuthorizationResourceType.ENCRYPTION_PROFILE, result);
    }

    @Test
    public void testIsAllowedAction() {
        boolean result = underTest.isAllowedAction(DESCRIBE_ENCRYPTION_PROFILE);
        assertTrue(result);
    }

    @Test
    public void shouldReturnDefaultCrnsWithLegacy() {
        CrnsByCategory result = underTest.getDefaultResourceCrns(List.of(DEFAULT_ENCRYPTION_PROFILE_CRN, CUSTOM_ENCRYPTION_PROFILE_CRN));

        assertEquals(List.of(DEFAULT_ENCRYPTION_PROFILE_CRN), result.getDefaultResourceCrns());
        assertEquals(List.of(CUSTOM_ENCRYPTION_PROFILE_CRN), result.getNotDefaultResourceCrns());
    }

}