package com.sequenceiq.cloudbreak.structuredevent.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.runners.Parameterized.Parameters;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;

@RunWith(Parameterized.class)
public class LdapConfigToLdapDetailsConverterTest {

    private static final Long WORKSPACE_ID_FROM_REQUEST = 4321L;

    private CloudbreakUser cloudbreakUser;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private LdapConfigToLdapDetailsConverter underTest;

    private LdapConfig source;

    public LdapConfigToLdapDetailsConverterTest(DirectoryType type) {
        source = TestUtil.ldapConfig();
        source.setDirectoryType(type);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        cloudbreakUser = TestUtil.cbUser();
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID_FROM_REQUEST);
    }

    @Parameters(name = "Current Directory type: {0}")
    public static Object[] data() {
        return DirectoryType.values();
    }

    @Test
    public void testAllLogicIndependentDataArePassedProperly() {
        LdapDetails result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(source.getId(), result.getId());
        assertEquals(source.getAdminGroup(), result.getAdminGroup());
        assertEquals(source.getCertificate(), result.getCertificate());
        assertEquals(source.getDescription(), result.getDescription());
        assertEquals(source.getDirectoryType().name(), result.getDirectoryType());
        assertEquals(source.getDomain(), result.getDomain());
        assertEquals(source.getGroupMemberAttribute(), result.getGroupMemberAttribute());
        assertEquals(source.getGroupObjectClass(), result.getGroupObjectClass());
        assertEquals(source.getGroupNameAttribute(), result.getGroupNameAttribute());
        assertEquals(source.getGroupSearchBase(), result.getGroupSearchBase());
        assertEquals(source.getName(), result.getName());
        assertEquals(source.getProtocol(), result.getProtocol());
        assertEquals(source.getServerHost(), result.getServerHost());
        assertEquals(source.getServerPort(), result.getServerPort());
        assertEquals(source.getUserDnPattern(), result.getUserDnPattern());
        assertEquals(source.getUserNameAttribute(), result.getUserNameAttribute());
        assertEquals(source.getUserObjectClass(), result.getUserObjectClass());
        assertEquals(source.getUserSearchBase(), result.getUserSearchBase());
        assertEquals(cloudbreakUser.getUsername(), result.getUserName());
        assertEquals(cloudbreakUser.getUserId(), result.getUserId());
        assertEquals(cloudbreakUser.getTenant(), result.getTenantName());
    }

    @Test
    public void testWhenSourceWorkspaceIdIsNotNullThenItsValueShouldBePassedBesideAllLogicIndependentData() {
        Workspace workspace = new Workspace();
        workspace.setId(23L);
        source.setWorkspace(workspace);
        LdapDetails result = underTest.convert(source);

        Assert.assertNotNull(result);
        Assert.assertEquals(source.getWorkspace().getId(), result.getWorkspaceId());
        verify(restRequestThreadLocalService, times(0)).getRequestedWorkspaceId();
    }

    @Test
    public void testWhenSourceWorkspaceIdIsNullThenItsValueShouldBePassedBesideAllLogicIndependentData() {
        source.setWorkspace(null);
        LdapDetails result = underTest.convert(source);

        Assert.assertNotNull(result);
        Assert.assertEquals(WORKSPACE_ID_FROM_REQUEST, result.getWorkspaceId());
        verify(restRequestThreadLocalService, times(1)).getRequestedWorkspaceId();
    }

}