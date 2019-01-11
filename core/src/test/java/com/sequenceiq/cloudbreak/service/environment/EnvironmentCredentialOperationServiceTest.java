package com.sequenceiq.cloudbreak.service.environment;


import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentChangeCredentialV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentCredentialOperationServiceTest {

    private static final String CRED_NAME = "credName";

    private static final Long WORKSPACE_ID = 1L;

    private static final String AWS = "AWS";

    @Mock
    private CredentialService credentialService;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private EnvironmentCredentialOperationService underTest;

    @Test
    public void testGetCredentialFromRequestByName() {
        EnvironmentChangeCredentialV4Request request = new EnvironmentChangeCredentialV4Request();
        request.setCredentialName(CRED_NAME);
        request.setCredential(new CredentialV4Request());
        Credential credential = new Credential();
        when(credentialService.getByNameForWorkspaceId(CRED_NAME, WORKSPACE_ID)).thenReturn(credential);

        Credential result = underTest.getCredentialFromRequest(request, WORKSPACE_ID);

        assertNotNull(result);
    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialFromRequestByNameNotFound() {
        EnvironmentChangeCredentialV4Request request = new EnvironmentChangeCredentialV4Request();
        request.setCredentialName(CRED_NAME);
        request.setCredential(new CredentialV4Request());
        when(credentialService.getByNameForWorkspaceId(CRED_NAME, WORKSPACE_ID)).thenThrow(new NotFoundException(""));

        underTest.getCredentialFromRequest(request, WORKSPACE_ID);
    }

    @Test
    public void testGetCredentialWithCreate() {
        EnvironmentChangeCredentialV4Request request = new EnvironmentChangeCredentialV4Request();
        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName("credName2");
        credentialRequest.setCloudPlatform("AWS");
        request.setCredential(credentialRequest);

        when(conversionService.convert(any(CredentialV4Request.class), eq(Credential.class)))
                .thenAnswer((Answer<Credential>) invocation -> {
                    Credential credential = new Credential();
                    credential.setId(2L);
                    credential.setName(request.getCredential().getName());
                    credential.setCloudPlatform(request.getCredential().getCloudPlatform());
                    return credential;
                });
        when(credentialService.createForLoggedInUser(any(Credential.class), anyLong()))
                .thenAnswer((Answer<Credential>) invocation -> (Credential) invocation.getArgument(0));

        Credential result = underTest.getCredentialFromRequest(request, WORKSPACE_ID);

        assertNotNull(result);
    }

    @Test(expected = BadRequestException.class)
    public void testValidatePlatformAndGetCredentialByNameWithInvalidCloudPlatform() {
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        EnvironmentChangeCredentialV4Request request = new EnvironmentChangeCredentialV4Request();
        request.setCredentialName(CRED_NAME);
        request.setCredential(new CredentialV4Request());
        Credential credential = new Credential();
        credential.setCloudPlatform("GCP");
        when(credentialService.getByNameForWorkspaceId(CRED_NAME, WORKSPACE_ID)).thenReturn(credential);

        underTest.validatePlatformAndGetCredential(request, environment, WORKSPACE_ID);
    }

    @Test
    public void testValidatePlatformAndGetCredentialByName() {
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        EnvironmentChangeCredentialV4Request request = new EnvironmentChangeCredentialV4Request();
        request.setCredentialName(CRED_NAME);
        request.setCredential(new CredentialV4Request());
        Credential credential = new Credential();
        credential.setCloudPlatform("AWS");
        when(credentialService.getByNameForWorkspaceId(CRED_NAME, WORKSPACE_ID)).thenReturn(credential);

        Credential result = underTest.validatePlatformAndGetCredential(request, environment, WORKSPACE_ID);

        assertNotNull(result);
    }

    @Test
    public void testValidatePlatformAndGetCredentialWithCreate() {
        Environment environment = new Environment();
        environment.setCloudPlatform(AWS);
        EnvironmentChangeCredentialV4Request request = new EnvironmentChangeCredentialV4Request();
        request.setCredential(new CredentialV4Request());
        request.getCredential().setCloudPlatform(AWS);

        when(conversionService.convert(any(CredentialV4Request.class), eq(Credential.class)))
                .thenAnswer((Answer<Credential>) invocation -> {
                    Credential credential = new Credential();
                    credential.setCloudPlatform(AWS);
                    credential.setId(2L);
                    return credential;
                });
        when(credentialService.createForLoggedInUser(any(Credential.class), anyLong()))
                .thenAnswer((Answer<Credential>) invocation -> (Credential) invocation.getArgument(0));

        Credential result = underTest.validatePlatformAndGetCredential(request, environment, WORKSPACE_ID);

        assertNotNull(result);
    }
}