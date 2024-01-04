package com.sequenceiq.cloudbreak.auth.altus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Set;

import jakarta.ws.rs.InternalServerErrorException;

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

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @InjectMocks
    private RoleCrnGenerator underTest;

    @BeforeEach
    public void setup() {
        lenient().when(grpcUmsClient.getRoles(any())).thenReturn(Set.of(
                "crn:altus:iam:us-west-1:altus:role:DbusUploader",
                "crn:altus:iam:us-west-1:altus:role:ComputeMetricsPublisher"));
        when(grpcUmsClient.getResourceRoles(any())).thenReturn(Set.of(
                "crn:altus:iam:us-west-1:altus:resourceRole:Owner",
                "crn:altus:iam:us-west-1:altus:resourceRole:EnvironmentAdmin"));
    }

    @Test
    public void testGetExistingRoles() {
        Crn testRole1 = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.getRoleCrn(UmsRole.DBUS_UPLOADER, ACCOUNT_ID));
        Crn testRole2 = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.getRoleCrn(UmsRole.COMPUTE_METRICS_PUBLISHER, ACCOUNT_ID));
        existingRoleAssertions(testRole1, testRole2, Crn.ResourceType.ROLE);

        testRole1 = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.getResourceRoleCrn(UmsResourceRole.OWNER, ACCOUNT_ID));
        testRole2 = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.getResourceRoleCrn(UmsResourceRole.ENVIRONMENT_ADMIN, ACCOUNT_ID));
        existingRoleAssertions(testRole1, testRole2, Crn.ResourceType.RESOURCE_ROLE);
    }

    @Test
    public void testGetNonExistingRoles() {
        assertThrows(InternalServerErrorException.class, () -> ThreadBasedUserCrnProvider.doAs(ACTOR, () ->
                underTest.getResourceRoleCrn(UmsResourceRole.ENVIRONMENT_USER, ACCOUNT_ID)));
    }

    private void existingRoleAssertions(Crn testRole1, Crn testRole2, Crn.ResourceType roleType) {
        assertEquals(ACCOUNT_ID, testRole1.getAccountId());
        assertEquals(roleType, testRole1.getResourceType());

        assertEquals(ACCOUNT_ID, testRole2.getAccountId());
        assertEquals(roleType, testRole2.getResourceType());
    }
}
