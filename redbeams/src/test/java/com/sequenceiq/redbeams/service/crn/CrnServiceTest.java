package com.sequenceiq.redbeams.service.crn;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

public class CrnServiceTest {

    private static final String RESOURCE_ID = "resourceId";

    private final CrnService crnService = new CrnService();

    @Test
    public void testCreateDatabaseCrnFrom() {
        Crn crn = crnService.createDatabaseCrnFrom(RESOURCE_ID);

        assertEquals(RESOURCE_ID, crn.getResource());
        assertEquals(Crn.ResourceType.DATABASE, crn.getResourceType());
        assertEquals(Crn.Service.REDBEAMS, crn.getService());
        assertEquals("ACCOUNT_ID", crn.getAccountId());
    }

    public static Crn getValidCrn() {
        return Crn.builder()
                .setService(Crn.Service.REDBEAMS)
                .setAccountId("ACCOUNT_ID")
                .setResourceType(Crn.ResourceType.DATABASE)
                .setResource("resourceId")
                .build();
    }
}
