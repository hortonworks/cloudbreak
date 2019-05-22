package com.sequenceiq.redbeams.service.crn;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

public class CrnServiceTest {

    private static final String RESOURCE_ID = "resourceId";

    private final CrnService crnService = new CrnService();

    @Test
    public void testCreateCrnDatabaseConfig() {
        DatabaseConfig resource = new DatabaseConfig();
        Crn crn = crnService.createCrn(resource);

        assertEquals(Crn.Service.REDBEAMS, crn.getService());
        assertEquals(Crn.ResourceType.DATABASE, crn.getResourceType());
    }

    @Test
    public void testCreateCrnDatabaseServerConfig() {
        DatabaseServerConfig resource = new DatabaseServerConfig();
        Crn crn = crnService.createCrn(resource);

        assertEquals(Crn.Service.REDBEAMS, crn.getService());
        assertEquals(Crn.ResourceType.DATABASE_SERVER, crn.getResourceType());
    }

}
