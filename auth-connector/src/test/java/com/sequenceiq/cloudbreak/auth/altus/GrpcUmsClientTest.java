package com.sequenceiq.cloudbreak.auth.altus;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GrpcUmsClientTest {

    private static final String ACCOUNT_ID = "altus";

    private static final String TEST_ROLE_1 = "TestRole1";

    private static final String TEST_ROLE_2 = "TestRole2";

    @InjectMocks
    private GrpcUmsClient underTest;

    @Test
    public void testGetRoleCrn() {
        Crn testRole1 = underTest.getRoleCrn(TEST_ROLE_1);
        Crn testRole2 = underTest.getRoleCrn(TEST_ROLE_2);

        assertEquals(ACCOUNT_ID, testRole1.getAccountId());
        assertEquals(TEST_ROLE_1, testRole1.getResource());
        assertEquals(Crn.ResourceType.ROLE, testRole1.getResourceType());

        assertEquals(ACCOUNT_ID, testRole2.getAccountId());
        assertEquals(TEST_ROLE_2, testRole2.getResource());
        assertEquals(Crn.ResourceType.ROLE, testRole2.getResourceType());
    }

    @Test
    public void testGetResourceRoleCrn() {
        Crn testRole1 = underTest.getResourceRoleCrn(TEST_ROLE_1);
        Crn testRole2 = underTest.getResourceRoleCrn(TEST_ROLE_2);

        assertEquals(ACCOUNT_ID, testRole1.getAccountId());
        assertEquals(TEST_ROLE_1, testRole1.getResource());
        assertEquals(Crn.ResourceType.RESOURCE_ROLE, testRole1.getResourceType());

        assertEquals(ACCOUNT_ID, testRole2.getAccountId());
        assertEquals(TEST_ROLE_2, testRole2.getResource());
        assertEquals(Crn.ResourceType.RESOURCE_ROLE, testRole2.getResourceType());
    }
}
