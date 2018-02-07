package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationRequest;
import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationResponse;

public class RepositoryConfigValidationServiceTest {

    @Mock
    private Response response;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Client client;

    private RepositoryConfigValidationService underTest;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        underTest = Mockito.spy(RepositoryConfigValidationService.class);
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
    public void testValidateForAmbariBaseUrlWhenTheUrlIsReachable() {
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.OK.value());
        String ambariBaseUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setAmbariBaseUrl(ambariBaseUrl);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(client, times(1)).target(ambariBaseUrl + "/ambari.repo");
        assertTrue(result.getAmbariBaseUrl());
        assertNull(result.getAmbariGpgKeyUrl());
        assertNull(result.getVersionDefinitionFileUrl());
        assertNull(result.getMpackUrl());
        assertNull(result.getStackBaseURL());
        assertNull(result.getUtilsBaseURL());
    }

    @Test
    public void testValidateForAmbariBaseUrlWhenTheUrlIsNotReachable() {
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());
        String ambariBaseUrl = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/BUILDS/2.6.1.0-143";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setAmbariBaseUrl(ambariBaseUrl);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(client, times(1)).target(ambariBaseUrl + "/ambari.repo");
        assertFalse(result.getAmbariBaseUrl());
        assertNull(result.getAmbariGpgKeyUrl());
        assertNull(result.getVersionDefinitionFileUrl());
        assertNull(result.getMpackUrl());
        assertNull(result.getStackBaseURL());
        assertNull(result.getUtilsBaseURL());
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
    public void testValidateForStackBaseUrlWhenTheItIsReachable() {
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.OK.value());
        String stackBaseUrl = "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.5.0/";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setStackBaseURL(stackBaseUrl);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(client, times(1)).target(stackBaseUrl + "hdp.repo");
        assertTrue(result.getStackBaseURL());
    }

    @Test
    public void testValidateForStackBaseUrlWhenTheTheRestClientThrowException() {
        doReturn(client).when(underTest).createRestClient();
        when(client.target(anyString()).request().get()).thenReturn(response);
        when(response.getStatus()).thenThrow(new RuntimeException());
        String stackBaseUrl = "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.5.0/";
        RepoConfigValidationRequest request = new RepoConfigValidationRequest();
        request.setStackBaseURL(stackBaseUrl);

        RepoConfigValidationResponse result = underTest.validate(request);

        verify(client, times(1)).target(stackBaseUrl + "hdp.repo");
        assertFalse(result.getStackBaseURL());
    }
}