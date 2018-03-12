package com.sequenceiq.cloudbreak.service.credential;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileCredentialHandler;

@RunWith(MockitoJUnitRunner.class)
public class CredentialServiceTest {

    private static final String PLATFORM = "OPENSTACK";

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Mock
    private UserProfileCredentialHandler userProfileCredentialHandler;

    @Mock
    private NotificationSender notificationSender;

    @InjectMocks
    private CredentialService credentialService = new CredentialService();

    private Credential credentialToModify;

    private IdentityUser user;

    private Topology originalTopology;

    private String originalDescription;

    private Json originalAttributes;

    @Before
    public void init() throws Exception {
        doNothing().when(authorizationService).hasWritePermission(any());
        doNothing().when(notificationSender).send(any());
        when(credentialAdapter.init(any(Credential.class))).then(invocation -> invocation.getArgumentAt(0, Credential.class));

        credentialToModify = new Credential();
        credentialToModify.setId(1L);
        credentialToModify.setCloudPlatform(PLATFORM);
        originalTopology = new Topology();
        credentialToModify.setTopology(originalTopology);
        originalDescription = "orig-desc";
        credentialToModify.setDescription(originalDescription);
        originalAttributes = new Json("test");
        credentialToModify.setAttributes(originalAttributes);
        when(credentialRepository.findByNameInUser(anyString(), anyString())).thenReturn(credentialToModify);
        when(credentialRepository.findOneByName(anyString(), anyString())).thenReturn(credentialToModify);
        when(credentialRepository.save(any(Credential.class))).then(invocation -> invocation.getArgumentAt(0, Credential.class));
        user = new IdentityUser("asef", "asdf", "asdf", null, "ASdf", "asdf", new Date());
    }

    @Test
    public void testModifyMapAllField() throws Exception {
        Credential credential = new Credential();
        credential.setCloudPlatform(PLATFORM);
        credential.setTopology(new Topology());
        credential.setAttributes(new Json("other"));
        credential.setDescription("mod-desc");
        Credential modify = credentialService.modify(user, credential);
        assertEquals(credential.getTopology(), modify.getTopology());
        assertEquals(credential.getAttributes(), modify.getAttributes());
        assertEquals(credential.getDescription(), modify.getDescription());
        assertNotEquals(originalTopology, modify.getTopology());
        assertNotEquals(originalAttributes, modify.getAttributes());
        assertNotEquals(originalAttributes, modify.getDescription());
        assertEquals(credential.cloudPlatform(), modify.cloudPlatform());
    }

    @Test
    public void testModifyMapNone() {
        Credential credential = new Credential();
        credential.setCloudPlatform(PLATFORM);
        Credential modify = credentialService.modify(user, credential);
        assertEquals(credentialToModify.getTopology(), modify.getTopology());
        assertEquals(credentialToModify.getAttributes(), modify.getAttributes());
        assertEquals(credentialToModify.getDescription(), modify.getDescription());
        assertEquals(originalTopology, modify.getTopology());
        assertEquals(originalAttributes, modify.getAttributes());
        assertEquals(originalDescription, modify.getDescription());
        assertEquals(credential.cloudPlatform(), modify.cloudPlatform());
    }

    @Test(expected = BadRequestException.class)
    public void testModifyDifferentPlatform() {
        Credential credential = new Credential();
        credential.setCloudPlatform("BAD");
        credentialService.modify(user, credential);
    }

}