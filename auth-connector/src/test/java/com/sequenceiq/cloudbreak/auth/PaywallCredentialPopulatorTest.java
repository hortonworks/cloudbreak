package com.sequenceiq.cloudbreak.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.WebTarget;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.client.RestClientUtil;

@ExtendWith(MockitoExtension.class)
public class PaywallCredentialPopulatorTest {

    @Mock
    private ClouderaManagerLicenseProvider clouderaManagerLicenseProvider;

    @InjectMocks
    private PaywallCredentialPopulator underTest;

    @Test
    public void testPopulateWebTargetShouldAddCredentialsWhenTheUrlIsPointsToArchive() {
        String baseUrl = "http://archive.cloudera.com/parcel1/";
        WebTarget webTarget = createWebTarget(baseUrl);
        JsonCMLicense license = mock(JsonCMLicense.class);

        when(clouderaManagerLicenseProvider.getLicense(any())).thenReturn(license);
        when(license.getPaywallUsername()).thenReturn("user");
        when(license.getPaywallPassword()).thenReturn("pw");

        underTest.populateWebTarget(baseUrl, webTarget);

        assertEquals(2, webTarget.getConfiguration().getInstances().size());
        verify(clouderaManagerLicenseProvider).getLicense(any());
        verify(license).getPaywallUsername();
        verify(license).getPaywallPassword();
    }

    @Test
    public void testPopulateWebTargetShouldNotAddCredentialsWhenTheUrlIsNotPointsToArchive() {
        String baseUrl = "http://random.cloudera.com/parcel1/";
        WebTarget webTarget = createWebTarget(baseUrl);

        underTest.populateWebTarget(baseUrl, webTarget);

        assertEquals(1, webTarget.getConfiguration().getInstances().size());
        verifyNoInteractions(clouderaManagerLicenseProvider);
    }

    private WebTarget createWebTarget(String baseUrl) {
        return RestClientUtil.get().target(StringUtils.stripEnd(baseUrl, "/") + "/manifest.json");
    }

}
