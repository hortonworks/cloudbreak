package com.sequenceiq.redbeams.service.crn;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.service.UuidGeneratorService;

public class CrnServiceTest {

    private static final String TEST_ACCOUNT_ID = "accountId";

    private static final String TEST_USER_ID = "bob";

    private static final Crn CRN = Crn.builder()
        .setService(Crn.Service.IAM)
        .setAccountId(TEST_ACCOUNT_ID)
        .setResourceType(Crn.ResourceType.USER)
        .setResource(TEST_USER_ID)
        .build();

    @InjectMocks
    private CrnService crnService;

    @Mock
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Mock
    private UuidGeneratorService uuidGeneratorService;

    @Before
    public void setUp() {
        initMocks(this);

        // the provider should really store this as a CRN
        when(threadBasedUserCrnProvider.getUserCrn()).thenReturn(CRN.toString());

        when(uuidGeneratorService.randomUuid()).thenReturn("uuid");
    }

    @Test
    public void testGetCurrentAccountId() {
        assertEquals(TEST_ACCOUNT_ID, crnService.getCurrentAccountId());
    }

    @Test
    public void testGetCurrentUserId() {
        assertEquals(TEST_USER_ID, crnService.getCurrentUserId());
    }

    @Test
    public void testCreateCrnDatabaseConfig() {
        DatabaseConfig resource = new DatabaseConfig();
        Crn crn = crnService.createCrn(resource);

        assertEquals(Crn.Service.REDBEAMS, crn.getService());
        assertEquals(CRN.getAccountId(), crn.getAccountId());
        assertEquals(Crn.ResourceType.DATABASE, crn.getResourceType());
        assertEquals("uuid", crn.getResource());
    }

    @Test
    public void testCreateCrnDatabaseServerConfig() {
        DatabaseServerConfig resource = new DatabaseServerConfig();
        Crn crn = crnService.createCrn(resource);

        assertEquals(Crn.Service.REDBEAMS, crn.getService());
        assertEquals(CRN.getAccountId(), crn.getAccountId());
        assertEquals(Crn.ResourceType.DATABASE_SERVER, crn.getResourceType());
        assertEquals("uuid", crn.getResource());
    }

}
