package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;

import reactor.core.Reactor;

public class AzureMetadataSetupTest {

    private static final String DUMMY_PRIVATE_IP = "dummyPrivateIP";
    private static final String DUMMY_VIRTUAL_IP = "dummyVirtualIP";
    private static final String DUMMY_VIRTUAL_MACHINE = "dummyVirtualMachine";
    private static final String DUMMY_VM_NAME = "dummyVmName";

    @InjectMocks
    @Spy
    private AzureMetadataSetup underTest;

    @Mock
    private Reactor reactor;

    @Mock
    private AzureStackUtil azureStackUtil;

    @Mock
    private AzureClient azureClient;

    private Stack stack;

    private User user;

    private AzureCredential credential;

    private AzureTemplate template;

    @Before
    public void setUp() {
        underTest = new AzureMetadataSetup();
        MockitoAnnotations.initMocks(this);
        user = AzureConnectorTestUtil.createUser();
        credential = AzureConnectorTestUtil.createAzureCredential();
        template = AzureConnectorTestUtil.createAzureTemplate(user);
        stack = AzureConnectorTestUtil.createStack(user, credential, template, getDefaultResourceSet());
    }

    public Set<Resource> getDefaultResourceSet() {
        Set<Resource> resources = new HashSet<>();
        resources.add(new com.sequenceiq.cloudbreak.domain.Resource(ResourceType.CLOUD_SERVICE, DUMMY_VM_NAME, stack));
        resources.add(new com.sequenceiq.cloudbreak.domain.Resource(ResourceType.VIRTUAL_MACHINE, DUMMY_VM_NAME, stack));
        return resources;
    }

    @Test
    public void testSetupMetadata() throws IOException {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString())).willReturn(azureClient);
        doReturn(DUMMY_PRIVATE_IP).when(underTest).getPrivateIP(anyString());
        doReturn(DUMMY_VIRTUAL_IP).when(underTest).getVirtualIP(anyString());
        given(azureClient.getVirtualMachine(anyMap())).willReturn(DUMMY_VIRTUAL_MACHINE);
        given(azureStackUtil.getVmName(anyString(), anyInt())).willReturn(DUMMY_VM_NAME);
        // WHEN
        underTest.setupMetadata(stack);
        // THEN
        verify(underTest, times(3)).getVirtualIP(anyString());
    }

    @Test
    public void testSetupMetadataWhenIOExceptionOccurs() throws IOException {
        // GIVEN
        given(azureStackUtil.createAzureClient(any(Credential.class), anyString())).willReturn(azureClient);
        doThrow(new IOException()).when(underTest).getPrivateIP(anyString());
        // WHEN
        underTest.setupMetadata(stack);
        // THEN
        verify(underTest, times(0)).getVirtualIP(anyString());
    }

}
