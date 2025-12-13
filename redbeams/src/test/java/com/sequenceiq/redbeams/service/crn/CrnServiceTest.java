package com.sequenceiq.redbeams.service.crn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.service.UuidGeneratorService;

@ExtendWith(MockitoExtension.class)
class CrnServiceTest {

    private static final String TEST_ACCOUNT_ID = "accountId";

    private static final String TEST_USER_ID = "bob";

    private static final Crn CRN = CrnTestUtil.getUserCrnBuilder()
            .setAccountId(TEST_ACCOUNT_ID)
            .setResource(TEST_USER_ID)
            .build();

    @InjectMocks
    private CrnService crnService;

    @Mock
    private UuidGeneratorService uuidGeneratorService;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @BeforeEach
    public void setUp() {
        lenient().when(uuidGeneratorService.randomUuid()).thenReturn("uuid");
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
    }

    @Test
    void testGetCurrentAccountId() {
        assertEquals(TEST_ACCOUNT_ID, ThreadBasedUserCrnProvider.doAs(CRN.toString(), () -> crnService.getCurrentAccountId()));
    }

    @Test
    void testGetCurrentUserId() {
        assertEquals(TEST_USER_ID, ThreadBasedUserCrnProvider.doAs(CRN.toString(), () -> crnService.getCurrentUserId()));
    }

    @Test
    void testCreateCrnDatabaseConfig() {
        DatabaseConfig resource = new DatabaseConfig();
        Crn crn = ThreadBasedUserCrnProvider.doAs(CRN.toString(), () -> crnService.createCrn(resource));

        assertEquals(Crn.Service.REDBEAMS, crn.getService());
        assertEquals(CRN.getAccountId(), crn.getAccountId());
        assertEquals(Crn.ResourceType.DATABASE, crn.getResourceType());
        assertEquals("uuid", crn.getResource());
    }

    @Test
    void testCreateCrnDatabaseServerConfig() {
        DatabaseServerConfig resource = new DatabaseServerConfig();
        Crn crn = ThreadBasedUserCrnProvider.doAs(CRN.toString(), () -> crnService.createCrn(resource));

        assertEquals(Crn.Service.REDBEAMS, crn.getService());
        assertEquals(CRN.getAccountId(), crn.getAccountId());
        assertEquals(Crn.ResourceType.DATABASE_SERVER, crn.getResourceType());
        assertEquals("uuid", crn.getResource());
    }

}
