package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationRequest;
import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationResponse;
import com.sequenceiq.cloudbreak.common.service.url.UrlAccessValidationService;
import com.sequenceiq.cloudbreak.service.credential.PaywallCredentialService;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryConfigValidationServiceTest {

    private static final String RPM_REPO_REPODATA_PATH = "/repodata/repomd.xml";

    @Mock
    private UrlAccessValidationService urlAccessValidationService;

    @Mock
    private PaywallCredentialService paywallCredentialService;

    @InjectMocks
    private RepositoryConfigValidationService underTest;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headerCaptor;

    @Before
    public void setUp() {
        when(paywallCredentialService.paywallCredentialAvailable()).thenReturn(false);
    }

    @Test
    public void testValidateForRequestsIsNull() {
        RepoConfigValidationResponse result = underTest.validate(null);

        assertNull(result.getAmbariBaseUrl());
        assertNull(result.getAmbariGpgKeyUrl());
        assertNull(result.getVersionDefinitionFileUrl());
        assertNull(result.getMpackUrl());
        assertNull(result.getStackBaseURL());
        assertNull(result.getUtilsBaseURL());
    }

    @Test
    public void testValidateForRequestsWithNullValuedFields() {
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        RepoConfigValidationResponse result = underTest.validate(request);

        assertNull(result.getAmbariBaseUrl());
        assertNull(result.getAmbariGpgKeyUrl());
        assertNull(result.getVersionDefinitionFileUrl());
        assertNull(result.getMpackUrl());
        assertNull(result.getStackBaseURL());
        assertNull(result.getUtilsBaseURL());
    }

    @Test
    public void testValidateForAmbariBaseUrlWhenTheRPMRepoIsReachable() {
        String ambariBaseUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setAmbariBaseUrl(ambariBaseUrl);
        when(urlAccessValidationService.isAccessible(anyString(), any())).thenReturn(true);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(anyString(), any());
        assertTrue(result.getAmbariBaseUrl());
        assertNull(result.getAmbariGpgKeyUrl());
        assertNull(result.getVersionDefinitionFileUrl());
        assertNull(result.getMpackUrl());
        assertNull(result.getStackBaseURL());
        assertNull(result.getUtilsBaseURL());
    }

    @Test
    public void testValidateForAmbariBaseUrlWhenTheDebRepoIsReachable() {
        String ambariBaseUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/ubuntu14/2.x/BUILDS/2.6.1.0-143";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setAmbariBaseUrl(ambariBaseUrl);
        String rpmRepoDataTarget = ambariBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = ambariBaseUrl + "/dists/Ambari/InRelease";
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget, null)).thenReturn(false);
        when(urlAccessValidationService.isAccessible(debRepoDataTarget, null)).thenReturn(true);

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture(), any());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertTrue(result.getAmbariBaseUrl());
    }

    @Test
    public void testValidateForAmbariBaseUrlWhenNoRPMOrDebRepoIsReachable() {
        String ambariBaseUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setAmbariBaseUrl(ambariBaseUrl);
        when(urlAccessValidationService.isAccessible(anyString(), any())).thenReturn(false);

        RepoConfigValidationResponse result = underTest.validate(request);

        String rpmRepoDataTarget = ambariBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = ambariBaseUrl + "/dists/Ambari/InRelease";
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture(), any());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertFalse(result.getAmbariBaseUrl());
    }

    @Test
    public void testValidateForAmbariGpgKeyUrlWhenTheUrlIsReachable() {
        String ambariGpgKeyUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setAmbariGpgKeyUrl(ambariGpgKeyUrl);
        when(urlAccessValidationService.isAccessible(ambariGpgKeyUrl, null)).thenReturn(true);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(ambariGpgKeyUrl, null);
        assertTrue(result.getAmbariGpgKeyUrl());
    }

    @Test
    public void testValidateForAmbariGpgUrlWhenTheUrlIsNotReachable() {
        String ambariGpgKeyUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setAmbariGpgKeyUrl(ambariGpgKeyUrl);
        when(urlAccessValidationService.isAccessible(ambariGpgKeyUrl, null)).thenReturn(false);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(ambariGpgKeyUrl, null);
        assertFalse(result.getAmbariGpgKeyUrl());
    }

    @Test
    public void testValidateForStackBaseUrlWhenRPMRepoIsReachable() {
        String stackBaseUrl = "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.5.0";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setStackBaseURL(stackBaseUrl);
        String rpmRepoDataTarget = stackBaseUrl + RPM_REPO_REPODATA_PATH;
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget, null)).thenReturn(true);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(rpmRepoDataTarget, null);
        assertTrue(result.getStackBaseURL());
    }

    @Test
    public void testValidateForStackBaseUrlWhenDebRepoIsReachable() {
        String stackBaseUrl = "http://public-repo-1.hortonworks.com/HDP/ubuntu14/2.x/updates/2.5.5.0";
        String rpmRepoDataTarget = stackBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = stackBaseUrl + "/dists/HDP/InRelease";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setStackBaseURL(stackBaseUrl);
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget, null)).thenReturn(false);
        when(urlAccessValidationService.isAccessible(debRepoDataTarget, null)).thenReturn(true);

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture(), any());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertTrue(result.getStackBaseURL());
    }

    @Test
    public void testValidateForStackBaseUrlWhenNoRepoIsAvailable() {
        String stackBaseUrl = "http://public-repo-1.hortonworks.com/HDP/ubuntu14/2.x/updates/2.5.5.0";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setStackBaseURL(stackBaseUrl);
        when(urlAccessValidationService.isAccessible(anyString(), any())).thenReturn(false);

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture(), any());
        List<String> arguments = argumentCaptor.getAllValues();
        String rpmRepoDataTarget = stackBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = stackBaseUrl + "/dists/HDP/InRelease";
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertFalse(result.getStackBaseURL());
    }

    @Test
    public void testValidateForStackUtilsBaseUrlWhenRPMRepoIsReachable() {
        String hdpUtilsBaseUrl = "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/sles12";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setUtilsBaseURL(hdpUtilsBaseUrl);
        String rpmRepoDataTarget = hdpUtilsBaseUrl + RPM_REPO_REPODATA_PATH;
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget, null)).thenReturn(true);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(rpmRepoDataTarget, null);
        assertTrue(result.getUtilsBaseURL());
    }

    @Test
    public void testValidateForStackUtilsBaseUrlWhenDebRepoIsReachable() {
        String utilsBaseUrl = "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/ubuntu14";
        String rpmRepoDataTarget = utilsBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = utilsBaseUrl + "/dists/HDP-UTILS/InRelease";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setUtilsBaseURL(utilsBaseUrl);
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget, null)).thenReturn(false);
        when(urlAccessValidationService.isAccessible(debRepoDataTarget, null)).thenReturn(true);

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture(), any());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertTrue(result.getUtilsBaseURL());
    }

    @Test
    public void testValidateForStackUtilsBaseUrlWhenNoRepoIsAvailable() {
        String utilsBaseUrl = "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/sles12";
        String rpmRepoDataTarget = utilsBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = utilsBaseUrl + "/dists/HDP-UTILS/InRelease";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setUtilsBaseURL(utilsBaseUrl);
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget, null)).thenReturn(false);
        when(urlAccessValidationService.isAccessible(debRepoDataTarget, null)).thenReturn(false);

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture(), any());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertFalse(result.getUtilsBaseURL());
    }

    @Test
    public void testValidateForMpack() {
        String mpackUrl = "https://archive.cloudera.com/p/HDF/centos7/3.x/updates/3.5.1.0/tars/hdf_ambari_mp/hdf-ambari-mpack-3.5.1.0-17.tar.gz";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setMpackUrl(mpackUrl);
        when(urlAccessValidationService.isAccessible(eq(mpackUrl), anyMap())).thenReturn(true);
        when(paywallCredentialService.paywallCredentialAvailable()).thenReturn(true);

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(1)).isAccessible(urlCaptor.capture(), headerCaptor.capture());
        String urlValue = urlCaptor.getValue();
        Map<String, Object> headerValue = headerCaptor.getValue();
        assertTrue(result.getMpackUrl());
        assertEquals(mpackUrl, urlValue);
        assertTrue(headerValue.containsKey("Authorization"));
        assertTrue(headerValue.get("Authorization").toString().startsWith("Basic"));
    }

}
