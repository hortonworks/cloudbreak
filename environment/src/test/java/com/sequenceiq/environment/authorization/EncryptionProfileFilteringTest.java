package com.sequenceiq.environment.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponses;
import com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.service.EncryptionProfileService;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileToEncryptionProfileResponseConverter;

class EncryptionProfileFilteringTest {

    private static final EncryptionProfile ENCRYPTION_PROFILE = EncryptionProfileTestConstants.getTestEncryptionProfile();

    private static final EncryptionProfile DEFAULT_ENCRYPTION_PROFILE =
            EncryptionProfileTestConstants.getTestEncryptionProfile("cdp_default", ResourceStatus.DEFAULT);

    @Mock
    private EncryptionProfileService encryptionProfileService;

    @Mock
    private EncryptionProfileToEncryptionProfileResponseConverter encryptionProfileResponseConverter;

    private EncryptionProfileFiltering underTest;

    @BeforeEach
    void setUp() {
        encryptionProfileResponseConverter = mock(EncryptionProfileToEncryptionProfileResponseConverter.class);
        encryptionProfileService = mock(EncryptionProfileService.class);
        EncryptionProfileResponse mockResponse = new EncryptionProfileResponse();
        mockResponse.setName(EncryptionProfileTestConstants.NAME);
        mockResponse.setCrn(EncryptionProfileTestConstants.ENCRYPTION_PROFILE_CRN);

        when(encryptionProfileResponseConverter.convert(any()))
                .thenReturn(mockResponse);
        when(encryptionProfileService.getEncryptionProfilesAsAuthorizationResources())
                .thenReturn(Arrays.asList(
                        new ResourceWithId(1L, "test-crn-1"),
                        new ResourceWithId(2L, "test-crn-2")
                ));
        when(encryptionProfileService.listAll())
                .thenReturn(Arrays.asList(ENCRYPTION_PROFILE, ENCRYPTION_PROFILE));
        when(encryptionProfileService.findAllById(anyList()))
                .thenReturn(Arrays.asList(ENCRYPTION_PROFILE, ENCRYPTION_PROFILE));
        when(encryptionProfileService.getAllDefaultEncryptionProfiles()).thenReturn(List.of(DEFAULT_ENCRYPTION_PROFILE));

        underTest = new EncryptionProfileFiltering(encryptionProfileService, encryptionProfileResponseConverter);
    }

    @Test
    void testGetAllResources() {
        List<ResourceWithId> resources = underTest.getAllResources(Collections.EMPTY_MAP);
        assertThat(resources).hasSize(2);
        assertThat(resources.get(0).getId()).isEqualTo(1L);
        assertThat(resources.get(0).getResourceCrn()).isEqualTo("test-crn-1");
        assertThat(resources.get(1).getId()).isEqualTo(2L);
        assertThat(resources.get(1).getResourceCrn()).isEqualTo("test-crn-2");
    }

    @Test
    void testGetAll() {
        EncryptionProfileResponses response = underTest.getAll(Collections.EMPTY_MAP);
        assertThat(response.getResponses()).hasSize(3);
        assertThat(response.getResponses().iterator().next().getName())
                .isEqualTo(EncryptionProfileTestConstants.NAME);
    }

    @Test
    void testFilterByIds() {
        EncryptionProfileResponses response = underTest.filterByIds(Arrays.asList(1L, 2L), Collections.EMPTY_MAP);
        assertThat(response.getResponses()).hasSize(3);
        assertThat(response.getResponses().iterator().next().getName())
                .isEqualTo(EncryptionProfileTestConstants.NAME);
    }

    @Test
    void testGetAllWithDefaultEncryptionProfile() {
        EncryptionProfileResponses response = underTest.getAll(Collections.emptyMap());

        assertThat(response.getResponses()).hasSize(3);
        ArgumentCaptor<EncryptionProfile> captor = ArgumentCaptor.forClass(EncryptionProfile.class);
        verify(encryptionProfileResponseConverter, times(3)).convert(captor.capture());
        List<EncryptionProfile> arguments = captor.getAllValues();
        assertThat(arguments.get(0).getResourceStatus()).isEqualTo(ResourceStatus.USER_MANAGED);
        assertThat(arguments.get(1).getResourceStatus()).isEqualTo(ResourceStatus.USER_MANAGED);
        assertThat(arguments.get(2).getResourceStatus()).isEqualTo(ResourceStatus.DEFAULT);
    }
}