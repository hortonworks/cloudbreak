package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;

@DataJpaTest
@Import({
        MaintenanceWindowScheduleService.class,
        MaintenanceWindowScheduleValidator.class,
        MaintenanceWindowScheduleScopeValidator.class,
        MaintenanceWindowScheduleConverter.class,
        MaintenanceWindowScheduleEligibilityService.class,
        MaintenanceOccurrenceCalculator.class
})
@Sql(scripts = "/sql/maintenance_window_schedule_version_default.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MaintenanceWindowScheduleServiceIntegrationTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:acc-1:user:1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";

    private static final long NOW = 1_735_689_600_000L;

    @Autowired
    private MaintenanceWindowScheduleRepository scheduleRepository;

    @Autowired
    private MaintenanceWindowScheduleService scheduleService;

    @MockitoBean
    private Clock clock;

    // ENVIRONMENT-scope schedules don't resolve a parent environment CRN, and the resolver only talks to remote
    // Data Hub / Data Lake / FreeIPA endpoints that aren't part of this @DataJpaTest slice, so it is mocked.
    @MockitoBean
    private MaintenanceResourceEnvironmentCrnResolver resourceEnvironmentCrnResolver;

    @BeforeEach
    void setUp() {
        scheduleRepository.deleteAll();
        org.mockito.Mockito.when(clock.getCurrentTimeMillis()).thenReturn(NOW);
    }

    @Test
    void createAndGetEnvironmentSchedule() {
        MaintenanceWindowScheduleRequest request = environmentWeeklyRequest();

        MaintenanceWindowScheduleResponse created = scheduleService.create(request, ACCOUNT_ID, USER_CRN);
        MaintenanceWindowScheduleResponse fetched = scheduleService.get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT, ENV_CRN);

        assertThat(fetched.getScopeType()).isEqualTo(MaintenanceScopeType.ENVIRONMENT.name());
        assertThat(fetched.getScopeId()).isEqualTo(ENV_CRN);
        assertThat(fetched.getName()).isEqualTo(created.getName());
        assertThat(fetched.getDurationMinutes()).isEqualTo(120);
    }

    @Test
    void getMissingScheduleThrowsNotFound() {
        assertThrows(NotFoundException.class,
                () -> scheduleService.get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT, ENV_CRN));
    }

    @Test
    void createAfterDeleteArchivesPriorAndAllowsNewActiveScheduleAtScope() {
        scheduleService.create(environmentWeeklyRequest(), ACCOUNT_ID, USER_CRN);

        scheduleService.delete(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT, ENV_CRN, USER_CRN);

        MaintenanceWindowScheduleRequest second = environmentWeeklyRequest();
        second.setName("env-weekly-v2");
        MaintenanceWindowScheduleResponse recreated = scheduleService.create(second, ACCOUNT_ID, USER_CRN);

        assertThat(recreated.getName()).isEqualTo("env-weekly-v2");
        assertThat(scheduleService.get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT, ENV_CRN).getName())
                .isEqualTo("env-weekly-v2");
        assertThat(scheduleRepository.findAll()).hasSize(2);
        assertThat(scheduleRepository.findAll().stream().filter(MaintenanceWindowSchedule::isArchived).count()).isEqualTo(1);
        assertThat(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT, ENV_CRN)).isPresent();
    }

    @Test
    void invalidEnvironmentScopeIdThrowsBadRequest() {
        MaintenanceWindowScheduleRequest request = environmentWeeklyRequest();
        request.setScopeId("not-a-crn");

        assertThrows(BadRequestException.class, () -> scheduleService.create(request, ACCOUNT_ID, USER_CRN));
    }

    private MaintenanceWindowScheduleRequest environmentWeeklyRequest() {
        MaintenanceWindowScheduleRequest request = new MaintenanceWindowScheduleRequest();
        request.setName("env-weekly");
        request.setScopeType(MaintenanceScopeType.ENVIRONMENT.name());
        request.setScopeId(ENV_CRN);
        request.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        request.setTimezone("America/Los_Angeles");
        request.setDurationMinutes(120);
        request.setStartLocalTime("02:00");
        request.setDayOfWeek(DayOfWeek.SUNDAY.name());
        return request;
    }
}
