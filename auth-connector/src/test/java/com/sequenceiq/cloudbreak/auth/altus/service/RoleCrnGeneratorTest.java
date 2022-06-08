package com.sequenceiq.cloudbreak.auth.altus.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.ws.rs.InternalServerErrorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

@ExtendWith(MockitoExtension.class)
public class RoleCrnGeneratorTest {

    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    private static final String ACCOUNT_ID = "altus";

    private static final String TEST_ROLE_1 = "TestRole1";

    private static final String TEST_ROLE_2 = "TestRole2";

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @InjectMocks
    private RoleCrnGenerator underTest;

    @BeforeEach
    public void setup() {
        when(grpcUmsClient.getRoles(any())).thenReturn(Set.of(
                "crn:altus:iam:us-west-1:altus:role:TestRole1",
                "crn:altus:iam:us-west-1:altus:role:TestRole2"));
        when(grpcUmsClient.getResourceRoles(any())).thenReturn(Set.of(
                "crn:altus:iam:us-west-1:altus:resourceRole:TestRole1",
                "crn:altus:iam:us-west-1:altus:resourceRole:TestRole2"));
    }

    @Test
    public void testGetExistingRoles() {
        Crn testRole1 = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.getRoleCrn(TEST_ROLE_1, "altus"));
        Crn testRole2 = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.getRoleCrn(TEST_ROLE_2, "altus"));
        existingRoleAssertions(testRole1, testRole2, Crn.ResourceType.ROLE);

        testRole1 = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.getResourceRoleCrn(TEST_ROLE_1, "altus"));
        testRole2 = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.getResourceRoleCrn(TEST_ROLE_2, "altus"));
        existingRoleAssertions(testRole1, testRole2, Crn.ResourceType.RESOURCE_ROLE);
    }

    @Test
    public void testGetNonExistingRoles() {
        assertThrows(InternalServerErrorException.class, () -> ThreadBasedUserCrnProvider.doAs(ACTOR, () ->
                underTest.getRoleCrn("whatever", "altus")));
        assertThrows(InternalServerErrorException.class, () -> ThreadBasedUserCrnProvider.doAs(ACTOR, () ->
                underTest.getResourceRoleCrn("whatever", "altus")));
    }

    private void existingRoleAssertions(Crn testRole1, Crn testRole2, Crn.ResourceType roleType) {
        assertEquals(ACCOUNT_ID, testRole1.getAccountId());
        assertEquals(TEST_ROLE_1, testRole1.getResource());
        assertEquals(roleType, testRole1.getResourceType());

        assertEquals(ACCOUNT_ID, testRole2.getAccountId());
        assertEquals(TEST_ROLE_2, testRole2.getResource());
        assertEquals(roleType, testRole2.getResourceType());
    }
}