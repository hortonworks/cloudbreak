package com.sequenceiq.redbeams.service.dbconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.repository.DatabaseConfigRepository;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.crn.CrnServiceTest;

public class DatabaseConfigServiceTest {

    private static final long CURRENT_TIME_MILLIS = 1000L;

    private static final String CRN = "crn";

    @Mock
    private DatabaseConfigRepository databaseConfigRepository;

    @Mock
    private Clock clock;

    @Mock
    private CrnService crnService;

    @InjectMocks
    private DatabaseConfigService underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRegister() {
        DatabaseConfig configToRegister = new DatabaseConfig();
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);
        when(crnService.createDatabaseCrn()).thenReturn(CrnServiceTest.getValidCrn());

        underTest.register(configToRegister);

        verify(databaseConfigRepository).save(configToRegister);
        assertEquals(CURRENT_TIME_MILLIS, configToRegister.getCreationDate().longValue());
        assertEquals(ResourceStatus.USER_MANAGED, configToRegister.getStatus());
        assertNotNull(configToRegister.getCrn());
    }
}
