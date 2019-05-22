package com.sequenceiq.redbeams.service.dbconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.repository.DatabaseConfigRepository;
import com.sequenceiq.redbeams.service.crn.CrnService;

public class DatabaseConfigServiceTest {

    private static final long CURRENT_TIME_MILLIS = 1000L;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
        when(crnService.createCrn(configToRegister)).thenReturn(TestData.getTestCrn("database", "name"));

        underTest.register(configToRegister);

        verify(databaseConfigRepository).save(configToRegister);
        assertEquals(CURRENT_TIME_MILLIS, configToRegister.getCreationDate().longValue());
        assertEquals(ResourceStatus.USER_MANAGED, configToRegister.getStatus());
        assertNotNull(configToRegister.getCrn());
    }

    @Test
    public void testRegisterEntityWithNameExists() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("database config already exists with name");
        DatabaseConfig configToRegister = new DatabaseConfig();
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);
        when(crnService.createCrn(configToRegister)).thenReturn(TestData.getTestCrn("database", "name"));
        when(databaseConfigRepository.save(configToRegister)).thenThrow(getDataIntegrityException());

        underTest.register(configToRegister);
    }

    private DataIntegrityViolationException getDataIntegrityException() {
        return new DataIntegrityViolationException("", new ConstraintViolationException("", new SQLException(), ""));
    }

    @Test
    public void testRegisterHasNoAccess() {
        thrown.expect(AccessDeniedException.class);
        DatabaseConfig configToRegister = new DatabaseConfig();
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);
        when(crnService.createCrn(configToRegister)).thenReturn(TestData.getTestCrn("database", "name"));
        when(databaseConfigRepository.save(configToRegister)).thenThrow(new AccessDeniedException("User has no right to access resource"));

        underTest.register(configToRegister);
    }

}
