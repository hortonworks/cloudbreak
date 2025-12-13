package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RepoConfigValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.RepoConfigValidationV4Response;
import com.sequenceiq.cloudbreak.auth.service.url.UrlAccessValidationService;

@ExtendWith(MockitoExtension.class)
class RepositoryConfigValidationServiceTest {

    private static final String RPM_REPO_REPODATA_PATH = "/repodata/repomd.xml";

    @Mock
    private UrlAccessValidationService urlAccessValidationService;

    @InjectMocks
    private RepositoryConfigValidationService underTest;

    @Test
    void testValidateForRequestsIsNull() {
        RepoConfigValidationV4Response result = underTest.validate(null);

        assertFalse(result.getAmbariBaseUrl());
        assertFalse(result.getAmbariGpgKeyUrl());
        assertFalse(result.getVersionDefinitionFileUrl());
        assertFalse(result.getMpackUrl());
        assertFalse(result.getStackBaseURL());
        assertFalse(result.getUtilsBaseURL());
    }

    @Test
    void testValidateForRequestsWithNullValuedFields() {
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        RepoConfigValidationV4Response result = underTest.validate(request);

        assertFalse(result.getAmbariBaseUrl());
        assertFalse(result.getAmbariGpgKeyUrl());
        assertFalse(result.getVersionDefinitionFileUrl());
        assertFalse(result.getMpackUrl());
        assertFalse(result.getStackBaseURL());
        assertFalse(result.getUtilsBaseURL());
    }

    @Test
    void testValidateForAmbariBaseUrlWhenTheRPMRepoIsReachable() {
        String ambariBaseUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143";
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setAmbariBaseUrl(ambariBaseUrl);
        when(urlAccessValidationService.isAccessible(anyString())).thenReturn(true);

        RepoConfigValidationV4Response result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(anyString());
        assertTrue(result.getAmbariBaseUrl());
        assertFalse(result.getAmbariGpgKeyUrl());
        assertFalse(result.getVersionDefinitionFileUrl());
        assertFalse(result.getMpackUrl());
        assertFalse(result.getStackBaseURL());
        assertFalse(result.getUtilsBaseURL());
    }

    @Test
    void testValidateForAmbariBaseUrlWhenTheDebRepoIsReachable() {
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
    void testValidateForAmbariBaseUrlWhenNoRPMOrDebRepoIsReachable() {
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
    void testValidateForAmbariGpgKeyUrlWhenTheUrlIsReachable() {
        String ambariGpgKeyUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins";
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setAmbariGpgKeyUrl(ambariGpgKeyUrl);
        when(urlAccessValidationService.isAccessible(ambariGpgKeyUrl)).thenReturn(true);

        RepoConfigValidationV4Response result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(ambariGpgKeyUrl);
        assertTrue(result.getAmbariGpgKeyUrl());
    }

    @Test
    void testValidateForAmbariGpgUrlWhenTheUrlIsNotReachable() {
        String ambariGpgKeyUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins";
        RepoConfigValidationV4Request request = new RepoConfigValidationV4Request();
        request.setAmbariGpgKeyUrl(ambariGpgKeyUrl);
        when(urlAccessValidationService.isAccessible(ambariGpgKeyUrl)).thenReturn(false);

        RepoConfigValidationV4Response result = underTest.validate(request);

        verify(urlAccessValidationService, times(1)).isAccessible(ambariGpgKeyUrl);
        assertFalse(result.getAmbariGpgKeyUrl());
    }

    @Test
    void testValidateForStackBaseUrlWhenRPMRepoIsReachable() {
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
    void testValidateForStackBaseUrlWhenDebRepoIsReachable() {
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
    void testValidateForStackBaseUrlWhenNoRepoIsAvailable() {
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
    void testValidateForStackUtilsBaseUrlWhenRPMRepoIsReachable() {
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
    void testValidateForStackUtilsBaseUrlWhenDebRepoIsReachable() {
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
    void testValidateForStackUtilsBaseUrlWhenNoRepoIsAvailable() {
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