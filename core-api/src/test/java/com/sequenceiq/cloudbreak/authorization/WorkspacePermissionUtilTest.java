package com.sequenceiq.cloudbreak.authorization;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.INVITE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.MANAGE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.READ;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.WRITE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.getName;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.ALL;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.BLUEPRINT;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.WORKSPACE;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;

@RunWith(Parameterized.class)
public class WorkspacePermissionUtilTest {

    private WorkspacePermissionUtil underTest;

    private WorkspaceResource resource;

    private Set<String> permissions;

    private boolean expectedResult;

    private Action action;

    public WorkspacePermissionUtilTest(Set<String> permissions, WorkspaceResource resource, Action action, boolean expectedResult) {
        this.expectedResult = expectedResult;
        this.permissions = permissions;
        this.resource = resource;
        this.action = action;
    }

    @Before
    public void setUp() {
        underTest = new WorkspacePermissionUtil();
    }

    @Parameterized.Parameters(name = "[{index}] Permission set: {0}, resource: {1}, action: {2}, expected output: {3}")
    public static Object[][] data() {
        return new Object[][]{
                {Set.of(getName(BLUEPRINT, READ)), BLUEPRINT, WRITE, false},
                {Set.of(getName(WORKSPACE, MANAGE)), WORKSPACE, WRITE, false},
                {Set.of(getName(WORKSPACE, INVITE)), WORKSPACE, WRITE, false},
                {Set.of(getName(WORKSPACE, INVITE)), WORKSPACE, MANAGE, false},
                {Set.of(getName(ALL, READ)), BLUEPRINT, READ, true},
                {Set.of(getName(ALL, WRITE)), BLUEPRINT, INVITE, true},
                {Set.of(getName(BLUEPRINT, READ)), BLUEPRINT, READ, true},
                {Set.of(getName(WORKSPACE, INVITE)), WORKSPACE, INVITE, true},
                {Set.of(getName(WORKSPACE, MANAGE)), WORKSPACE, INVITE, true},
                {Set.of(getName(WORKSPACE, MANAGE), getName(WORKSPACE, INVITE)), WORKSPACE, MANAGE, true},
                {Set.of(getName(WORKSPACE, MANAGE), getName(WORKSPACE, INVITE)), WORKSPACE, INVITE, true},
        };
    }

    @Test
    public void testHasPermission() {
        boolean result = underTest.hasPermission(permissions, resource, action);

        Assert.assertEquals(expectedResult, result);
    }

}