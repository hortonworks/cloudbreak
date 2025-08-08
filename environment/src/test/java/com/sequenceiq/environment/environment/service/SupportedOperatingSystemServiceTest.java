package com.sequenceiq.environment.environment.service;


import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static com.sequenceiq.common.model.OsType.RHEL9;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.environment.api.v1.environment.OsTypeToOsTypeResponseConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.SupportedOperatingSystemResponse;

@ExtendWith(MockitoExtension.class)
class SupportedOperatingSystemServiceTest {

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ProviderPreferencesService providerPreferencesService;

    @Spy
    private OsTypeToOsTypeResponseConverter osTypeToOsTypeResponseConverter;

    @InjectMocks
    private SupportedOperatingSystemService underTest;

    @Test
    void listSupportedOperatingSystemRHEL8Default() {
        when(providerPreferencesService.isGovCloudDeployment()).thenReturn(false);

        SupportedOperatingSystemResponse response = underTest.listSupportedOperatingSystem("account-id", null);

        assertEquals("redhat8", response.getDefaultOs());
        assertEquals(RHEL8.getOs(), response.getDefaultOs());
        assertEquals(response.getOsTypes(), List.of(osTypeToOsTypeResponseConverter.convert(CENTOS7), osTypeToOsTypeResponseConverter.convert(RHEL8),
                osTypeToOsTypeResponseConverter.convert(RHEL9)));
    }

    @Test
    void listSupportedOperatingSystemGovCloudOnly() {
        when(providerPreferencesService.isGovCloudDeployment()).thenReturn(true);

        SupportedOperatingSystemResponse response = underTest.listSupportedOperatingSystem("account-id", null);

        assertEquals("redhat8", response.getDefaultOs());
        assertEquals(RHEL8.getOs(), response.getDefaultOs());
        assertEquals(response.getOsTypes(), List.of(osTypeToOsTypeResponseConverter.convert(RHEL8)));
    }

}