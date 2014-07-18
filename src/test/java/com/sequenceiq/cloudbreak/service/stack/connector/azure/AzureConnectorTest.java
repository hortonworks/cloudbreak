package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;


public class AzureConnectorTest {

    private static final String DUMMY_VM_NAME = "dummyVmName";
    private static final String DUMMY_REQUEST_ID = "dummyRequestId";
    private static final String EXCEPTION_MESSAGE = "exceptionMessage";

    @InjectMocks
    private AzureConnector underTest;

    @Mock
    private JsonHelper jsonHelper;

    @Mock
    private AzureStackUtil azureStackUtil;

    @Mock
    private AzureClient azureClient;

    @Mock
    private JsonNode jsonNode;

    @Mock
    private HttpResponseDecorator httpResponseDecorator;

    @Mock
    private HttpResponseException mockedException;

    private Stack stack;

    private User user;

    private AzureCredential credential;

    private AzureTemplate azureTemplate;

    @Before
    public void setUp() {
        underTest = new AzureConnector();
        MockitoAnnotations.initMocks(this);
        user = AzureConnectorTestUtil.createUser();
        azureTemplate = AzureConnectorTestUtil.createAzureTemplate(user);
        credential = AzureConnectorTestUtil.createAzureCredential();
        stack = AzureConnectorTestUtil.createStack(user, credential, azureTemplate, getDefaultResourceSet());
    }

    public Set<Resource> getDefaultResourceSet() {
        Set<Resource> resources = new HashSet<>();
        resources.add(new Resource(ResourceType.CLOUD_SERVICE, DUMMY_VM_NAME, stack));
        resources.add(new Resource(ResourceType.CLOUD_SERVICE, DUMMY_VM_NAME, stack));
        resources.add(new Resource(ResourceType.CLOUD_SERVICE, DUMMY_VM_NAME, stack));
        resources.add(new Resource(ResourceType.VIRTUAL_MACHINE, DUMMY_VM_NAME, stack));
        resources.add(new Resource(ResourceType.VIRTUAL_MACHINE, DUMMY_VM_NAME, stack));
        resources.add(new Resource(ResourceType.VIRTUAL_MACHINE, DUMMY_VM_NAME, stack));
        return resources;
    }

    @Test
    public void testDescribeStackWithResources() {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString()))
                .willReturn(azureClient);
        given(azureClient.getAffinityGroup(anyString())).willReturn(new Object());
        given(azureClient.getStorageAccount(anyString())).willReturn(new Object());
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.getCloudService(DUMMY_VM_NAME)).willReturn(new Object());
        given(azureClient.getVirtualMachine(anyMap())).willReturn(new Object());
        given(jsonHelper.createJsonFromString(anyString())).willReturn(jsonNode);
        // WHEN
        StackDescription result = underTest.describeStackWithResources(user, stack, credential);
        // THEN
        verify(jsonHelper, times(8)).createJsonFromString(anyString());
        assertNotNull(result);
    }

    @Test
    public void testDescribeStackWithResourcesWhenThrowsAffinityGroupRelatedException() {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString()))
                .willReturn(azureClient);
        given(azureClient.getAffinityGroup(anyString())).willThrow(new IllegalStateException());
        given(azureClient.getStorageAccount(anyString())).willReturn(new Object());
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.getCloudService(DUMMY_VM_NAME)).willReturn(new Object());
        given(azureClient.getVirtualMachine(anyMap())).willReturn(new Object());
        given(jsonHelper.createJsonFromString(anyString())).willReturn(jsonNode);
        // WHEN
        StackDescription result = underTest.describeStackWithResources(user, stack, credential);
        // THEN
        verify(jsonHelper, times(8)).createJsonFromString(anyString());
        assertNotNull(result);
    }

    @Test
    public void testDescribeStackWithResourcesWhenThrowsStorageAccountRelatedException() {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString()))
                .willReturn(azureClient);
        given(azureClient.getAffinityGroup(anyString())).willReturn(new Object());
        given(azureClient.getStorageAccount(anyString())).willThrow(new IllegalStateException());
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.getCloudService(DUMMY_VM_NAME)).willReturn(new Object());
        given(azureClient.getVirtualMachine(anyMap())).willReturn(new Object());
        given(jsonHelper.createJsonFromString(anyString())).willReturn(jsonNode);
        // WHEN
        StackDescription result = underTest.describeStackWithResources(user, stack, credential);
        // THEN
        verify(jsonHelper, times(8)).createJsonFromString(anyString());
        assertNotNull(result);
    }

    @Test
    public void testDescribeStackWithResourcesWhenThrowsCloudServiceRelatedException() {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString()))
                .willReturn(azureClient);
        given(azureClient.getAffinityGroup(anyString())).willReturn(new Object());
        given(azureClient.getStorageAccount(anyString())).willReturn(new Object());
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.getCloudService(DUMMY_VM_NAME)).willThrow(new IllegalStateException());
        given(azureClient.getVirtualMachine(anyMap())).willReturn(new Object());
        given(jsonHelper.createJsonFromString(anyString())).willReturn(jsonNode);
        // WHEN
        StackDescription result = underTest.describeStackWithResources(user, stack, credential);
        // THEN
        verify(jsonHelper, times(8)).createJsonFromString(anyString());
        assertNotNull(result);
    }

    @Test
    public void testDescribeStackWithResourcesWhenThrowsVirtualMachineRelatedException() {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString()))
                .willReturn(azureClient);
        given(azureClient.getAffinityGroup(anyString())).willReturn(new Object());
        given(azureClient.getStorageAccount(anyString())).willReturn(new Object());
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.getCloudService(DUMMY_VM_NAME)).willReturn(new Object());
        given(azureClient.getVirtualMachine(anyMap())).willThrow(new IllegalStateException());
        given(jsonHelper.createJsonFromString(anyString())).willReturn(jsonNode);
        // WHEN
        StackDescription result = underTest.describeStackWithResources(user, stack, credential);
        // THEN
        verify(jsonHelper, times(8)).createJsonFromString(anyString());
        assertNotNull(result);
    }

    @Test
    public void testDeleteStack() throws HttpResponseException {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString()))
                .willReturn(azureClient);
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.deleteVirtualMachine(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.deleteCloudService(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getRequestId(httpResponseDecorator)).willReturn(DUMMY_REQUEST_ID);
        given(azureClient.waitUntilComplete(DUMMY_REQUEST_ID)).willReturn(new Object());
        // WHEN
        underTest.deleteStack(user, stack, credential);
        // THEN
        verify(azureClient, times(6)).waitUntilComplete(anyString());
    }

    @Test(expected = InternalServerException.class)
    public void testDeleteStackWhenThrowsVirtualMachineRelatedInternalException() throws HttpResponseException {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString()))
                .willReturn(azureClient);
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.deleteVirtualMachine(anyMap())).willThrow(new InternalServerException(EXCEPTION_MESSAGE));
        // WHEN
        underTest.deleteStack(user, stack, credential);
    }

    @Test(expected = InternalServerException.class)
    public void testDeleteStackWhenThrowsVirtualMachineRelatedHttpException() throws HttpResponseException {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString()))
                .willReturn(azureClient);
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.deleteVirtualMachine(anyMap())).willThrow(mockedException);
        given(azureClient.deleteCloudService(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getRequestId(httpResponseDecorator)).willReturn(DUMMY_REQUEST_ID);
        given(azureClient.waitUntilComplete(DUMMY_REQUEST_ID)).willReturn(new Object());
        // WHEN
        underTest.deleteStack(user, stack, credential);
    }

    @Test
    public void testDeleteStackWhenThrowsVirtualMachineRelatedHttpNotFoundException() throws HttpResponseException {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString()))
                .willReturn(azureClient);
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.deleteVirtualMachine(anyMap())).willThrow(mockedException);
        given(mockedException.getStatusCode()).willReturn(404);
        given(azureClient.deleteCloudService(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getRequestId(httpResponseDecorator)).willReturn(DUMMY_REQUEST_ID);
        given(azureClient.waitUntilComplete(DUMMY_REQUEST_ID)).willReturn(new Object());
        // WHEN
        underTest.deleteStack(user, stack, credential);
        // THEN
        verify(azureClient, times(3)).waitUntilComplete(anyString());
    }

    @Test
    public void testDeleteStackWhenThrowsCloudServiceRelatedException() throws HttpResponseException {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString()))
                .willReturn(azureClient);
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.deleteVirtualMachine(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.deleteCloudService(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getRequestId(httpResponseDecorator)).willReturn(DUMMY_REQUEST_ID);
        given(azureClient.waitUntilComplete(DUMMY_REQUEST_ID)).willReturn(new Object());
        // WHEN
        underTest.deleteStack(user, stack, credential);
        // THEN
        verify(azureClient, times(6)).waitUntilComplete(anyString());
    }

    @Test(expected = InternalServerException.class)
    public void testDeleteStackWhenThrowsCloudServiceRelatedInternalException() throws HttpResponseException {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString()))
                .willReturn(azureClient);
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.deleteVirtualMachine(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.deleteCloudService(anyMap())).willThrow(new InternalServerException(EXCEPTION_MESSAGE));
        given(azureClient.getRequestId(httpResponseDecorator)).willReturn(DUMMY_REQUEST_ID);
        given(azureClient.waitUntilComplete(DUMMY_REQUEST_ID)).willReturn(new Object());
        // WHEN
        underTest.deleteStack(user, stack, credential);
    }
}
