package com.sequenceiq.cloudbreak.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.ws.rs.client.WebTarget;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.client.RestClientUtil;

@RunWith(MockitoJUnitRunner.class)
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

        assertFalse(webTarget.getConfiguration().getInstances().isEmpty());
        verify(clouderaManagerLicenseProvider).getLicense(any());
        verify(license).getPaywallUsername();
        verify(license).getPaywallPassword();
    }

    @Test
    public void testPopulateWebTargetShouldNotAddCredentialsWhenTheUrlIsNotPointsToArchive() {
        String baseUrl = "http://random.cloudera.com/parcel1/";
        WebTarget webTarget = createWebTarget(baseUrl);

        underTest.populateWebTarget(baseUrl, webTarget);

        assertTrue(webTarget.getConfiguration().getInstances().isEmpty());
        verifyNoInteractions(clouderaManagerLicenseProvider);
    }

    private WebTarget createWebTarget(String baseUrl) {
        return RestClientUtil.get().target(StringUtils.stripEnd(baseUrl, "/") + "/manifest.json");
    }

}