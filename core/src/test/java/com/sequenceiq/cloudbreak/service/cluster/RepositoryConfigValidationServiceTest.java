package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RepoConfigValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.RepoConfigValidationV4Response;
import com.sequenceiq.cloudbreak.common.service.url.UrlAccessValidationService;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryConfigValidationServiceTest {

    private static final String RPM_REPO_REPODATA_PATH = "/repodata/repomd.xml";

    @Mock
    private UrlAccessValidationService urlAccessValidationService;

    @InjectMocks
    private RepositoryConfigValidationService underTest;

    @Before
    public void setUp() {
    }

    @Test
    public void testValidateForRequestsIsNull() {
        RepoConfigValidationV4Response result = underTest.validate(null);

        assertNull(result.getAmbariBaseUrl());
        assertNull(result.getAmbariGpgKeyUrl());
        assertNull(result.getVersionDefinitionFileUrl());
        assertNull(result.getMpackUrl());
        assertNull(result.getStackBaseURL());
        assertNull(result.getUtilsBaseURL());
    }

    @Test
    public void testValidateForRequestsWithNullValuedFields() {
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        RepoConfigValidationV4Response result = underTest.validate(request);

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
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setAmbariBaseUrl(ambariBaseUrl);
        when(urlAccessValidationService.isAccessible(anyString())).thenReturn(true);

        RepoConfigValidationV4Response result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(anyString());
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
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setAmbariBaseUrl(ambariBaseUrl);
        String rpmRepoDataTarget = ambariBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = ambariBaseUrl + "/dists/Ambari/InRelease";
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget)).thenReturn(false);
        when(urlAccessValidationService.isAccessible(debRepoDataTarget)).thenReturn(true);

        RepoConfigValidationV4Response result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertTrue(result.getAmbariBaseUrl());
    }

    @Test
    public void testValidateForAmbariBaseUrlWhenNoRPMOrDebRepoIsReachable() {
        String ambariBaseUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143";
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setAmbariBaseUrl(ambariBaseUrl);
        when(urlAccessValidationService.isAccessible(anyString())).thenReturn(false);

        RepoConfigValidationV4Response result = underTest.validate(request);

        String rpmRepoDataTarget = ambariBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = ambariBaseUrl + "/dists/Ambari/InRelease";
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertFalse(result.getAmbariBaseUrl());
    }

    @Test
    public void testValidateForAmbariGpgKeyUrlWhenTheUrlIsReachable() {
        String ambariGpgKeyUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins";
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setAmbariGpgKeyUrl(ambariGpgKeyUrl);
        when(urlAccessValidationService.isAccessible(ambariGpgKeyUrl)).thenReturn(true);

        RepoConfigValidationV4Response result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(ambariGpgKeyUrl);
        assertTrue(result.getAmbariGpgKeyUrl());
    }

    @Test
    public void testValidateForAmbariGpgUrlWhenTheUrlIsNotReachable() {
        String ambariGpgKeyUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins";
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setAmbariGpgKeyUrl(ambariGpgKeyUrl);
        when(urlAccessValidationService.isAccessible(ambariGpgKeyUrl)).thenReturn(false);

        RepoConfigValidationV4Response result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(ambariGpgKeyUrl);
        assertFalse(result.getAmbariGpgKeyUrl());
    }

    @Test
    public void testValidateForStackBaseUrlWhenRPMRepoIsReachable() {
        String stackBaseUrl = "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.5.0";
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setStackBaseURL(stackBaseUrl);
        String rpmRepoDataTarget = stackBaseUrl + RPM_REPO_REPODATA_PATH;
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget)).thenReturn(true);

        RepoConfigValidationV4Response result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(rpmRepoDataTarget);
        assertTrue(result.getStackBaseURL());
    }

    @Test
    public void testValidateForStackBaseUrlWhenDebRepoIsReachable() {
        String stackBaseUrl = "http://public-repo-1.hortonworks.com/HDP/ubuntu14/2.x/updates/2.5.5.0";
        String rpmRepoDataTarget = stackBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = stackBaseUrl + "/dists/HDP/InRelease";
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setStackBaseURL(stackBaseUrl);
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget)).thenReturn(false);
        when(urlAccessValidationService.isAccessible(debRepoDataTarget)).thenReturn(true);

        RepoConfigValidationV4Response result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertTrue(result.getStackBaseURL());
    }

    @Test
    public void testValidateForStackBaseUrlWhenNoRepoIsAvailable() {
        String stackBaseUrl = "http://public-repo-1.hortonworks.com/HDP/ubuntu14/2.x/updates/2.5.5.0";
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setStackBaseURL(stackBaseUrl);
        when(urlAccessValidationService.isAccessible(anyString())).thenReturn(false);

        RepoConfigValidationV4Response result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture());
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
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setUtilsBaseURL(hdpUtilsBaseUrl);
        String rpmRepoDataTarget = hdpUtilsBaseUrl + RPM_REPO_REPODATA_PATH;
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget)).thenReturn(true);

        RepoConfigValidationV4Response result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(rpmRepoDataTarget);
        assertTrue(result.getUtilsBaseURL());
    }

    @Test
    public void testValidateForStackUtilsBaseUrlWhenDebRepoIsReachable() {
        String utilsBaseUrl = "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/ubuntu14";
        String rpmRepoDataTarget = utilsBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = utilsBaseUrl + "/dists/HDP-UTILS/InRelease";
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setUtilsBaseURL(utilsBaseUrl);
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget)).thenReturn(false);
        when(urlAccessValidationService.isAccessible(debRepoDataTarget)).thenReturn(true);

        RepoConfigValidationV4Response result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture());
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
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setUtilsBaseURL(utilsBaseUrl);
        when(urlAccessValidationService.isAccessible(rpmRepoDataTarget)).thenReturn(false);
        when(urlAccessValidationService.isAccessible(debRepoDataTarget)).thenReturn(false);

        RepoConfigValidationV4Response result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(urlAccessValidationService, times(2)).isAccessible(argumentCaptor.capture());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertFalse(result.getUtilsBaseURL());
    }
}