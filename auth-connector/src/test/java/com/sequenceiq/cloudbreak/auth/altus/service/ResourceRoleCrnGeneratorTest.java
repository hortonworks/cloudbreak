package com.sequenceiq.cloudbreak.auth.altus.service;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;

@ExtendWith(MockitoExtension.class)
public class ResourceRoleCrnGeneratorTest {

    private static final String ACCOUNT_ID = "altus";

    private static final String TEST_ROLE_1 = "TestRole1";

    private static final String TEST_ROLE_2 = "TestRole2";

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @InjectMocks
    private RoleCrnGenerator underTest;

    @BeforeEach
    public void init() {
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
    }

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