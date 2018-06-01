package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationRequest;
import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationResponse;

public class RepositoryConfigValidationServiceTest {

    private static final String RPM_REPO_REPODATA_PATH = "/repodata/repomd.xml";

    @Mock
    private Response response;

    @Mock
    private Response secondResponse;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Client client;

    @Spy
    private RepositoryConfigValidationService underTest;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
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
        doReturn(client).when(underTest).createRestClient();

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
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.OK.value());
        String ambariBaseUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setAmbariBaseUrl(ambariBaseUrl);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(client, times(1)).target(ambariBaseUrl + "/repodata/repomd.xml");
        assertTrue(result.getAmbariBaseUrl());
        assertNull(result.getAmbariGpgKeyUrl());
        assertNull(result.getVersionDefinitionFileUrl());
        assertNull(result.getMpackUrl());
        assertNull(result.getStackBaseURL());
        assertNull(result.getUtilsBaseURL());
    }

    @Test
    public void testValidateForAmbariBaseUrlWhenTheDebRepoIsReachable() {
        doReturn(client).when(underTest).createRestClient();
        String ambariBaseUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/ubuntu14/2.x/BUILDS/2.6.1.0-143";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setAmbariBaseUrl(ambariBaseUrl);
        String rpmRepoDataTarget = ambariBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = ambariBaseUrl + "/dists/Ambari/InRelease";
        when(client.target(rpmRepoDataTarget).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());
        when(client.target(debRepoDataTarget).request().get()).thenReturn(secondResponse);
        when(secondResponse.getStatus()).thenReturn(HttpStatus.OK.value());

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client, atLeast(2)).target(argumentCaptor.capture());
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
        String rpmRepoDataTarget = ambariBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = ambariBaseUrl + "/dists/Ambari/InRelease";
        doReturn(client).when(underTest).createRestClient();
        when(client.target(rpmRepoDataTarget).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());
        when(client.target(debRepoDataTarget).request().get()).thenReturn(secondResponse);
        when(secondResponse.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client, atLeast(2)).target(argumentCaptor.capture());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertFalse(result.getAmbariBaseUrl());
    }

    @Test
    public void testValidateForAmbariGpgKeyUrlWhenTheUrlIsReachable() {
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.OK.value());
        String ambariGpgKeyUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setAmbariGpgKeyUrl(ambariGpgKeyUrl);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(client, times(1)).target(ambariGpgKeyUrl);
        assertTrue(result.getAmbariGpgKeyUrl());
    }

    @Test
    public void testValidateForAmbariGpgUrlWhenTheUrlIsNotReachable() {
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());
        String ambariGpgKeyUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setAmbariGpgKeyUrl(ambariGpgKeyUrl);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(client, times(1)).target(ambariGpgKeyUrl);
        assertFalse(result.getAmbariGpgKeyUrl());
    }

    @Test
    public void testValidateForStackBaseUrlWhenRPMRepoIsReachable() {
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.OK.value());
        String stackBaseUrl = "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.5.0";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setStackBaseURL(stackBaseUrl);
        String rpmRepoDataTarget = stackBaseUrl + RPM_REPO_REPODATA_PATH;

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(client, times(1)).target(rpmRepoDataTarget);
        assertTrue(result.getStackBaseURL());
    }

    @Test
    public void testValidateForStackBaseUrlWhenDebRepoIsReachable() {
        String stackBaseUrl = "http://public-repo-1.hortonworks.com/HDP/ubuntu14/2.x/updates/2.5.5.0";
        String rpmRepoDataTarget = stackBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = stackBaseUrl + "/dists/HDP/InRelease";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setStackBaseURL(stackBaseUrl);
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());
        when(client.target(debRepoDataTarget).request().get()).thenReturn(secondResponse);
        when(secondResponse.getStatus()).thenReturn(HttpStatus.OK.value());

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client, atLeast(2)).target(argumentCaptor.capture());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertTrue(result.getStackBaseURL());
    }

    @Test
    public void testValidateForStackBaseUrlWhenNoRepoIsAvailable() {
        String stackBaseUrl = "http://public-repo-1.hortonworks.com/HDP/ubuntu14/2.x/updates/2.5.5.0";
        String rpmRepoDataTarget = stackBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = stackBaseUrl + "/dists/HDP/InRelease";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setStackBaseURL(stackBaseUrl);
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());
        when(client.target(debRepoDataTarget).request().get()).thenReturn(secondResponse);
        when(secondResponse.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client, atLeast(2)).target(argumentCaptor.capture());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertFalse(result.getStackBaseURL());
    }

    @Test
    public void testValidateForStackUtilsBaseUrlWhenRPMRepoIsReachable() {
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.OK.value());
        String hdpUtilsBaseUrl = "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/sles12";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setUtilsBaseURL(hdpUtilsBaseUrl);
        String rpmRepoDataTarget = hdpUtilsBaseUrl + RPM_REPO_REPODATA_PATH;

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(client, times(1)).target(rpmRepoDataTarget);
        assertTrue(result.getUtilsBaseURL());
    }

    @Test
    public void testValidateForStackUtilsBaseUrlWhenDebRepoIsReachable() {
        String utilsBaseUrl = "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/ubuntu14";
        String rpmRepoDataTarget = utilsBaseUrl + RPM_REPO_REPODATA_PATH;
        String debRepoDataTarget = utilsBaseUrl + "/dists/HDP-UTILS/InRelease";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setUtilsBaseURL(utilsBaseUrl);
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());
        when(client.target(debRepoDataTarget).request().get()).thenReturn(secondResponse);
        when(secondResponse.getStatus()).thenReturn(HttpStatus.OK.value());

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client, atLeast(2)).target(argumentCaptor.capture());
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
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());
        when(client.target(debRepoDataTarget).request().get()).thenReturn(secondResponse);
        when(secondResponse.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());

        RepoConfigValidationResponse result = underTest.validate(request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client, atLeast(2)).target(argumentCaptor.capture());
        List<String> arguments = argumentCaptor.getAllValues();
        assertTrue(arguments.contains(rpmRepoDataTarget));
        assertTrue(arguments.contains(debRepoDataTarget));
        assertFalse(result.getUtilsBaseURL());
    }
}