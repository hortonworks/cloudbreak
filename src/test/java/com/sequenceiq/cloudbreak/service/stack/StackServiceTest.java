package com.sequenceiq.cloudbreak.service.stack;

import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;
import com.sequenceiq.cloudbreak.service.account.AccountService;

public class StackServiceTest {

    @InjectMocks
    private StackService stackService;

    @Mock
    private AccountService accountService;

    @Before
    public void before() {
        stackService = new DefaultStackService();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAllStacksForAccountAdminWithoutAccountUser() {
        // GIVEN
        Account account = ServiceTestUtils.createAccount("Blueprint Ltd.", 1L);
        User admin = ServiceTestUtils.createUser(UserRole.ACCOUNT_ADMIN, account, 1L);
        // admin has a blueprint
        admin.getStacks().add(ServiceTestUtils.createStack(admin));

        // WHEN
        Set<Stack> blueprints = stackService.getAll(admin);

        // THEN
        Assert.assertNotNull(blueprints);
        Assert.assertTrue(blueprints.size() == 1);
    }

    @Test
    public void testGetAllStacksForAccountAdminWithAccountUserWithStack() {
        // GIVEN
        Account account = ServiceTestUtils.createAccount("Blueprint Ltd.", 1L);
        User admin = ServiceTestUtils.createUser(UserRole.ACCOUNT_ADMIN, account, 1L);
        User cUser = ServiceTestUtils.createUser(UserRole.ACCOUNT_USER, account, 3L);
        // admin has a stack
        admin.getStacks().add(ServiceTestUtils.createStack(admin));
        // cUser has also one stack
        cUser.getStacks().add(ServiceTestUtils.createStack(cUser));
        given(accountService.accountUsers(account.getId())).willReturn(new HashSet<User>(Arrays.asList(cUser)));

        // WHEN
        Set<Stack> blueprints = stackService.getAll(admin);

        // THEN
        Assert.assertNotNull(blueprints);
        Assert.assertTrue("The number of the returned stacks is right", blueprints.size() == 2);
    }

    @Test
    public void testGetAllForAccountUserWithVisibleAccountStacks() {
        // GIVEN
        Account account = ServiceTestUtils.createAccount("Blueprint Ltd.", 1L);
        User admin = ServiceTestUtils.createUser(UserRole.ACCOUNT_ADMIN, account, 1L);
        User cUser = ServiceTestUtils.createUser(UserRole.ACCOUNT_USER, account, 3L);
        // admin has a blueprint, with ACCOUNT_ADMIN role! (not visible for
        // account users
        admin.getStacks().add(ServiceTestUtils.createStack(admin));
        // cUser has also one blueprint
        cUser.getStacks().add(ServiceTestUtils.createStack(cUser));
        given(accountService.accountUserData(account.getId(), UserRole.ACCOUNT_USER)).willReturn(admin);

        // WHEN
        Set<Stack> blueprints = stackService.getAll(cUser);

        // THEN
        Assert.assertNotNull(blueprints);
        Assert.assertTrue("The number of the returned blueprints is right", blueprints.size() == 2);
    }

}
