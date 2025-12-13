package com.sequenceiq.environment.environment.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.tag.AccountTagValidationFailed;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.tags.service.AccountTagService;
import com.sequenceiq.environment.tags.service.DefaultInternalAccountTagService;
import com.sequenceiq.environment.tags.v1.converter.AccountTagToAccountTagResponsesConverter;

@ExtendWith(MockitoExtension.class)
class EnvironmentTagsDtoConverterTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String ACCOUNT_ID = "accountId";

    private static final String CLOUD_PLATFORM_AWS = "AWS";

    private static final String LOCATION = "location";

    private static final String LOCATION_DISPLAY_NAME = "locationDisplayName";

    private static final Double LONGITUDE = 12.34;

    private static final Double LATITUDE = -56.78;

    private static final String CREATOR = "creator";

    private static final Integer FREE_IPA_INSTANCE_COUNT_BY_GROUP = 2;

    private static final String USER_NAME = "userName";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AccountTagService accountTagService;

    @Mock
    private DefaultInternalAccountTagService defaultInternalAccountTagService;

    @Mock
    private AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter;

    @Mock
    private CostTagging costTagging;

    @Mock
    private CrnUserDetailsService crnUserDetailsService;

    @InjectMocks
    private EnvironmentTagsDtoConverter underTest;

    @Test
    public void testGetTagsWithCreationDto() {
        EnvironmentCreationDto creationDto = createMockEnvironmentCreationDto();

        when(entitlementService.internalTenant(any())).thenReturn(true);
        when(accountTagService.get(any())).thenReturn(new HashSet<>());
        when(crnUserDetailsService.getUmsUser(any())).thenReturn(createMockUserDetails());
        when(costTagging.prepareDefaultTags(any())).thenReturn(new HashMap<>());

        Json tags = underTest.getTags(creationDto);

        verify(entitlementService).internalTenant(anyString());
        verify(accountTagService).get(anyString());
        verify(crnUserDetailsService).getUmsUser(anyString());
        verify(costTagging).prepareDefaultTags(any());

        Map<String, String> expectedUserDefinedTags = createMockUserDefinedTags();
        Map<String, String> expectedDefaultTags = new HashMap<>();
        EnvironmentTags expectedEnvironmentTags = new EnvironmentTags(expectedUserDefinedTags, expectedDefaultTags);

        assertEquals(new Json(expectedEnvironmentTags), tags);
    }

    @Test
    void creationDtoToEnvironmentTestWhenErrorAndOtherException() {
        LocationDto location = LocationDto.builder()
                .withLatitude(LATITUDE)
                .withLongitude(LONGITUDE)
                .withName(LOCATION)
                .withDisplayName(LOCATION_DISPLAY_NAME)
                .build();
        FreeIpaCreationDto freeIpaCreation = FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP)
                .build();
        EnvironmentCreationDto creationDto = EnvironmentCreationDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withCreator(CREATOR)
                .withCloudPlatform(CLOUD_PLATFORM_AWS)
                .withLocation(location)
                .withFreeIpaCreation(freeIpaCreation)
                .withTags(null)
                .withCrn(RESOURCE_CRN)
                .build();

        when(crnUserDetailsService.getUmsUser(CREATOR)).thenReturn(new CrnUser(null, CREATOR, USER_NAME, null, ACCOUNT_ID, null));
        UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException("This operation is not supported");
        when(costTagging.prepareDefaultTags(any(CDPTagGenerationRequest.class))).thenThrow(unsupportedOperationException);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.getTags(creationDto));

        assertThat(badRequestException).hasMessage("Failed to convert dynamic userDefinedTags. This operation is not supported");
        assertThat(badRequestException).hasCauseReference(unsupportedOperationException);
    }

    @Test
    void creationDtoToEnvironmentTestWhenErrorAndAccountTagValidationFailed() {
        LocationDto location = LocationDto.builder()
                .withLatitude(LATITUDE)
                .withLongitude(LONGITUDE)
                .withName(LOCATION)
                .withDisplayName(LOCATION_DISPLAY_NAME)
                .build();
        FreeIpaCreationDto freeIpaCreation = FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP)
                .build();
        EnvironmentCreationDto creationDto = EnvironmentCreationDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withCreator(CREATOR)
                .withCloudPlatform(CLOUD_PLATFORM_AWS)
                .withLocation(location)
                .withFreeIpaCreation(freeIpaCreation)
                .withTags(null)
                .withCrn(RESOURCE_CRN)
                .build();

        when(crnUserDetailsService.getUmsUser(CREATOR)).thenReturn(new CrnUser(null, CREATOR, USER_NAME, null, ACCOUNT_ID, null));
        when(costTagging.prepareDefaultTags(any(CDPTagGenerationRequest.class))).thenThrow(new AccountTagValidationFailed("Error validating tags"));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.getTags(creationDto));

        assertThat(badRequestException).hasMessage("Error validating tags");
    }

    private EnvironmentCreationDto createMockEnvironmentCreationDto() {
        return EnvironmentCreationDto.builder()
                .withAccountId("accountid")
                .withCreator("creator")
                .withCrn("crn")
                .withCloudPlatform("platform")
                .withTags(Map.of("user1", "value1"))
                .build();
    }

    private Map<String, String> createMockUserDefinedTags() {
        return Map.of("user1", "value1");
    }

    private AccountTagResponse createMockAccountTagResponse() {
        return null;
    }

    private CrnUser createMockUserDetails() {
        return new CrnUser("userId", "userCrn", "username", "email", "tenant", "role");
    }

}