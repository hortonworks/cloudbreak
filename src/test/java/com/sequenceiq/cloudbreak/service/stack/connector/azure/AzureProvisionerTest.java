package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.CREDENTIAL;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.EMAILASFOLDER;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.StackCreationFailureException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;
import reactor.core.Reactor;

public class AzureProvisionerTest {

    private static final String USER_DATA = "dummyUserData";
    private static final String DUMMY_EMAIL_FOLDER = "dummyEmailFolder";
    private static final String DUMMY_REQUEST_ID = "dummyRequestId";
    private static final String DUMMY_VM_NAME = "dummyVmName";
    private static final String DUMMY_DATA = "bytes";
    private static final String SHA_1_FINGERPRINT = "sha1Fingerprint";
    private static final String STACK_NAME = "stack_name";

    @InjectMocks
    private AzureProvisioner underTest;

    @Mock
    private RetryingStackUpdater retryingStackUpdater;

    @Mock
    private AzureStackUtil azureStackUtil;

    @Mock
    private AzureClient azureClient;

    @Mock
    private Object virtualNetworkConfiguration;

    @Mock
    private HttpResponseDecorator httpResponseDecorator;

    @Mock
    private HttpResponseException httpResponseException;

    @Mock
    private X509Certificate x509Certificate;

    @Mock
    private Reactor reactor;

    private Stack stack;

    private Map<String, Object> setupProperties;

    private AzureCredential credential;

    private AzureTemplate template;

    @Before
    public void setUp() {
        underTest = new AzureProvisioner();
        MockitoAnnotations.initMocks(this);
        User user = AzureConnectorTestUtil.createUser();
        template = AzureConnectorTestUtil.createAzureTemplate(user);
        credential = AzureConnectorTestUtil.createAzureCredential();
        stack = AzureConnectorTestUtil.createStack(user, credential, template, getDefaultResourceSet());
        setupProperties = createSetupProperties();
    }

    public Set<Resource> getDefaultResourceSet() {
        Set<Resource> resources = new HashSet<>();
        resources.add(new com.sequenceiq.cloudbreak.domain.Resource(ResourceType.CLOUD_SERVICE, DUMMY_VM_NAME, stack));
        resources.add(new com.sequenceiq.cloudbreak.domain.Resource(ResourceType.VIRTUAL_MACHINE, DUMMY_VM_NAME, stack));
        return resources;
    }

    @Test
    public void testBuildStack() throws Exception {
        // GIVEN
        given(retryingStackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        given(retryingStackUpdater.updateStackResources(anyLong(), any(Set.class))).willReturn(stack);
        given(retryingStackUpdater.updateStackCreateComplete(anyLong())).willReturn(stack);
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString())).willReturn(azureClient);
        given(azureClient.getAffinityGroup(anyString())).willReturn(new Object());
        given(azureClient.getStorageAccount(anyString())).willReturn(new Object());
        given(azureClient.getVirtualNetworkConfiguration()).willReturn(virtualNetworkConfiguration);
        given(virtualNetworkConfiguration.toString()).willReturn(AzureConnectorTestUtil.DUMMY_NAME);
        given(azureClient.createVirtualNetwork(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getRequestId(httpResponseDecorator)).willReturn(DUMMY_REQUEST_ID);
        given(azureClient.waitUntilComplete(DUMMY_REQUEST_ID)).willReturn(new Object());
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.createCloudService(anyMap())).willReturn(httpResponseDecorator);
        given(azureStackUtil.createX509Certificate(any(AzureCredential.class), anyString())).willReturn(x509Certificate);
        given(x509Certificate.getSha1Fingerprint()).willReturn(SHA_1_FINGERPRINT);
        given(x509Certificate.getPem()).willReturn(DUMMY_DATA.getBytes());
        given(azureClient.createServiceCertificate(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.createVirtualMachine(anyMap())).willReturn(httpResponseDecorator);
        // WHEN
        underTest.buildStack(stack, USER_DATA, setupProperties);
        // THEN
        verify(azureClient, times(3)).createVirtualMachine(anyMap());
    }

    @Test
    public void testBuildStackWhenNoAffinityGroupAndStorageAccountNotFoundAndNoTemplatePassword()
            throws FileNotFoundException, CertificateException, NoSuchAlgorithmException {
        // GIVEN
        given(retryingStackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        given(retryingStackUpdater.updateStackResources(anyLong(), any(Set.class))).willReturn(stack);
        given(retryingStackUpdater.updateStackCreateComplete(anyLong())).willReturn(stack);
        given(retryingStackUpdater.updateStackCreateComplete(anyLong())).willReturn(stack);
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString())).willReturn(azureClient);
        given(azureClient.getAffinityGroup(anyString())).willReturn(httpResponseException);
        given(azureClient.getStorageAccount(anyString())).willReturn(httpResponseException);
        given(httpResponseException.getStatusCode()).willReturn(404);
        given(azureClient.createStorageAccount(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.createAffinityGroup(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getVirtualNetworkConfiguration()).willReturn(STACK_NAME);
        given(azureClient.createVirtualNetwork(anyMap())).willReturn(httpResponseDecorator);
        given(virtualNetworkConfiguration.toString()).willReturn(AzureConnectorTestUtil.DUMMY_NAME);
        given(azureClient.createVirtualNetwork(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getRequestId(httpResponseDecorator)).willReturn(DUMMY_REQUEST_ID);
        given(azureClient.waitUntilComplete(DUMMY_REQUEST_ID)).willReturn(new Object());
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.createCloudService(anyMap())).willReturn(httpResponseDecorator);
        given(azureStackUtil.createX509Certificate(any(AzureCredential.class), anyString())).willReturn(x509Certificate);
        given(x509Certificate.getPem()).willReturn(DUMMY_DATA.getBytes());
        given(x509Certificate.getSha1Fingerprint()).willReturn(SHA_1_FINGERPRINT);
        given(azureClient.createServiceCertificate(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.createVirtualMachine(anyMap())).willReturn(httpResponseDecorator);
        // WHEN
        underTest.buildStack(stack, USER_DATA, setupProperties);
        // THEN
        verify(azureClient, times(3)).createVirtualMachine(anyMap());
    }

    @Test
    public void testBuildStackWhenThrowsInternalServerErrorOnCreateStorageAccount() throws Exception {
        // GIVEN
        given(retryingStackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        given(retryingStackUpdater.updateStackResources(anyLong(), any(Set.class))).willReturn(stack);
        given(retryingStackUpdater.updateStackCreateComplete(anyLong())).willReturn(stack);
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString())).willReturn(azureClient);
        given(azureClient.getAffinityGroup(anyString())).willReturn(httpResponseException);
        given(azureClient.getStorageAccount(anyString())).willReturn(httpResponseException);
        given(httpResponseException.getStatusCode()).willReturn(401);
        given(azureClient.createStorageAccount(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.createAffinityGroup(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getVirtualNetworkConfiguration()).willReturn(STACK_NAME);
        given(azureClient.createVirtualNetwork(anyMap())).willReturn(httpResponseDecorator);
        given(virtualNetworkConfiguration.toString()).willReturn(AzureConnectorTestUtil.DUMMY_NAME);
        given(azureClient.createVirtualNetwork(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getRequestId(httpResponseDecorator)).willReturn(DUMMY_REQUEST_ID);
        given(azureClient.waitUntilComplete(DUMMY_REQUEST_ID)).willReturn(new Object());
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.createCloudService(anyMap())).willReturn(httpResponseDecorator);
        given(azureStackUtil.createX509Certificate(any(AzureCredential.class), anyString())).willReturn(x509Certificate);
        given(x509Certificate.getPem()).willReturn(DUMMY_DATA.getBytes());
        given(x509Certificate.getSha1Fingerprint()).willReturn(SHA_1_FINGERPRINT);
        given(azureClient.createServiceCertificate(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.createVirtualMachine(anyMap())).willReturn(httpResponseDecorator);
        // WHEN
        underTest.buildStack(stack, USER_DATA, setupProperties);
        // THEN
        verify(azureClient, times(3)).createVirtualMachine(anyMap());
    }

    @Test(expected = StackCreationFailureException.class)
    public void testBuildStackWhenCertificateFileNotFound() throws FileNotFoundException, CertificateException {
        // GIVEN
        given(retryingStackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString())).willReturn(azureClient);
        given(azureClient.getAffinityGroup(anyString())).willReturn(httpResponseException);
        given(azureClient.getStorageAccount(anyString())).willReturn(httpResponseException);
        given(httpResponseException.getStatusCode()).willReturn(401);
        given(azureClient.createStorageAccount(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.createAffinityGroup(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getVirtualNetworkConfiguration()).willReturn(STACK_NAME);
        given(azureClient.createVirtualNetwork(anyMap())).willReturn(httpResponseDecorator);
        given(virtualNetworkConfiguration.toString()).willReturn(AzureConnectorTestUtil.DUMMY_NAME);
        given(azureClient.createVirtualNetwork(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getRequestId(httpResponseDecorator)).willReturn(DUMMY_REQUEST_ID);
        given(azureClient.waitUntilComplete(DUMMY_REQUEST_ID)).willReturn(new Object());
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.createCloudService(anyMap())).willReturn(httpResponseDecorator);
        given(azureStackUtil.createX509Certificate(any(AzureCredential.class), anyString()))
                .willThrow(new FileNotFoundException("file not found"));
        // WHEN
        underTest.buildStack(stack, USER_DATA, setupProperties);
        // THEN
        verify(azureClient, times(0)).createVirtualMachine(anyMap());
    }

    @Test(expected = IllegalStateException.class)
    public void testBuildStackWhenExceptionThrowsDuringCloudServiceCreation() throws FileNotFoundException, CertificateException, NoSuchAlgorithmException {
        // GIVEN
        given(retryingStackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString())).willReturn(azureClient);
        given(azureClient.getAffinityGroup(anyString())).willReturn(httpResponseException);
        given(azureClient.getStorageAccount(anyString())).willReturn(httpResponseException);
        given(httpResponseException.getStatusCode()).willReturn(404);
        given(azureClient.createStorageAccount(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.createAffinityGroup(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getVirtualNetworkConfiguration()).willReturn(STACK_NAME);
        given(azureClient.createVirtualNetwork(anyMap())).willReturn(httpResponseDecorator);
        given(virtualNetworkConfiguration.toString()).willReturn(AzureConnectorTestUtil.DUMMY_NAME);
        given(azureClient.createVirtualNetwork(anyMap())).willReturn(httpResponseDecorator);
        given(azureClient.getRequestId(httpResponseDecorator)).willReturn(DUMMY_REQUEST_ID);
        given(azureClient.waitUntilComplete(DUMMY_REQUEST_ID)).willReturn(new Object());
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        given(azureClient.createCloudService(anyMap())).willThrow(new IllegalStateException("exception"));
        // WHEN
        underTest.buildStack(stack, USER_DATA, setupProperties);
        // THEN
        verify(azureStackUtil, times(0)).createX509Certificate(any(AzureCredential.class), anyString());
    }

    private Map<String, Object> createSetupProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(CREDENTIAL, credential);
        props.put(EMAILASFOLDER, DUMMY_EMAIL_FOLDER);
        return props;
    }
}
