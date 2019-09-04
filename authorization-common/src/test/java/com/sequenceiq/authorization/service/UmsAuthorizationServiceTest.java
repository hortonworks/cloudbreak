package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.ResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.RightUtils;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@RunWith(MockitoJUnitRunner.class)
public class UmsAuthorizationServiceTest {

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:" + USER_ID;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private UmsAuthorizationService underTest;

    @Test
    public void testCheckReadRight() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/write. This requires one of these roles: PowerUser. "
                + "You can request access through IAM service from an administrator.");

        underTest.checkRightOfUserForResource(USER_CRN, ResourceType.DATALAKE, ResourceAction.WRITE);
    }

    @Test
    public void testCheckWriteRight() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/read. This requires one of these roles: PowerUser. "
                + "You can request access through IAM service from an administrator.");

        underTest.checkRightOfUserForResource(USER_CRN, ResourceType.DATALAKE, ResourceAction.READ);
    }

    @Test
    public void testHasRightOfUserForResourceWithValidResourceAndAction() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(true);

        assertTrue(underTest.hasRightOfUserForResource(USER_CRN, "datalake", "write"));

        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        assertFalse(underTest.hasRightOfUserForResource(USER_CRN, "datalake", "write"));
    }

    @Test
    public void testHasRightOfUserForResourceWithInvalidResource() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Resource or action cannot be found by request!");

        underTest.hasRightOfUserForResource(USER_CRN, "invalid", "write");

        verifyZeroInteractions(umsClient);
    }

    @Test
    public void testHasRightOfUserForResourceWithInvalidAction() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Resource or action cannot be found by request!");

        underTest.hasRightOfUserForResource(USER_CRN, "datalake", "invalid");

        verifyZeroInteractions(umsClient);
    }

    private List<UserManagementProto.Role> getRoles() {
        ArrayList<UserManagementProto.Role> roles = Lists.newArrayList();
        List<String> writeRights = Arrays.stream(ResourceType.values())
                .map(resource -> RightUtils.getRight(resource, ResourceAction.WRITE))
                .collect(Collectors.toList());
        List<String> readRights = Arrays.stream(ResourceType.values())
                .map(resource -> RightUtils.getRight(resource, ResourceAction.READ))
                .collect(Collectors.toList());
        roles.add(createRole("EnvironmentAdmin", Lists.newArrayList(Iterables.concat(writeRights, readRights))));
        roles.add(createRole("EnvironmentUser", readRights));
        return roles;
    }

    private UserManagementProto.Role createRole(String name, List<String> rights) {
        UserManagementProto.PolicyStatement policyStatement = UserManagementProto.PolicyStatement.newBuilder()
                .addAllRight(rights)
                .build();
        UserManagementProto.PolicyDefinition policyDefinition = UserManagementProto.PolicyDefinition.newBuilder()
                .addStatement(policyStatement)
                .build();

        UserManagementProto.Policy policy = UserManagementProto.Policy.newBuilder()
                .setPolicyDefinition(policyDefinition)
                .build();
        return UserManagementProto.Role.newBuilder()
                .setCrn(Crn.builder()
                        .setAccountId(Crn.fromString(USER_CRN).getAccountId())
                        .setResource(name)
                        .setResourceType(Crn.ResourceType.ROLE)
                        .setService(Crn.Service.IAM)
                        .build()
                        .toString())
                .addPolicy(policy)
                .build();
    }

}
