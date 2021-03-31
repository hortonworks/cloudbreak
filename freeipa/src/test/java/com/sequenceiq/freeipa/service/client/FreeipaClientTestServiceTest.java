package com.sequenceiq.freeipa.service.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Group;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@ExtendWith(MockitoExtension.class)
public class FreeipaClientTestServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:userId";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:1234:environment:env";

    private static final String ADMIN_GROUP = "admins";

    private static final String ADMIN_USER = "admin";

    private static final String OTHER_USER_OR_GROUP = "other";

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private FreeIpaClient freeIpaClient;

    @InjectMocks
    private FreeipaClientTestService underTest;

    @BeforeEach
    public void setup() throws FreeIpaClientException {
        when(freeIpaClientFactory.getFreeIpaClientByAccountAndEnvironment(any(), any())).thenReturn(freeIpaClient);
    }

    @Test
    public void testCheckUsers() throws FreeIpaClientException {
        when(freeIpaClient.userFindAll()).thenReturn(Sets.newHashSet(createUser(ADMIN_USER)));
        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.checkUsers(ENV_CRN, Sets.newHashSet(ADMIN_USER))));
        assertFalse(ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.checkUsers(ENV_CRN, Sets.newHashSet(OTHER_USER_OR_GROUP))));
    }

    @Test
    public void testCheckGroups() throws FreeIpaClientException {
        when(freeIpaClient.groupFindAll()).thenReturn(Sets.newHashSet(createGroup(ADMIN_GROUP, ADMIN_USER)));
        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.checkGroups(ENV_CRN, Sets.newHashSet(ADMIN_GROUP))));
        assertFalse(ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.checkGroups(ENV_CRN, Sets.newHashSet(OTHER_USER_OR_GROUP))));
    }

    @Test
    public void testCheckUsersInGroup() throws FreeIpaClientException {
        when(freeIpaClient.groupFindAll()).thenReturn(Sets.newHashSet(createGroup(ADMIN_GROUP, ADMIN_USER)));
        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.checkUsersInGroup(ENV_CRN, Sets.newHashSet(ADMIN_USER), ADMIN_GROUP)));
        assertFalse(ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.checkUsersInGroup(ENV_CRN, Sets.newHashSet(OTHER_USER_OR_GROUP), ADMIN_GROUP)));
        assertFalse(ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.checkUsersInGroup(ENV_CRN, Sets.newHashSet(ADMIN_USER, OTHER_USER_OR_GROUP), ADMIN_GROUP)));
        assertFalse(ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.checkUsersInGroup(ENV_CRN, Sets.newHashSet(ADMIN_USER), OTHER_USER_OR_GROUP)));
    }

    private User createUser(String user) {
        User ipaUser = new User();
        ipaUser.setUid(user);
        return ipaUser;
    }

    private Group createGroup(String group, String user) {
        Group ipaGroup = new Group();
        ipaGroup.setCn(group);
        ipaGroup.setMemberUser(Lists.newArrayList(user));
        return ipaGroup;
    }
}
