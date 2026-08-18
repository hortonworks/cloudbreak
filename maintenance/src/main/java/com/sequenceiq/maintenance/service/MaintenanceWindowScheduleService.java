package com.sequenceiq.maintenance.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleListResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.exception.ConflictException;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;

@Service
public class MaintenanceWindowScheduleService {

    private final MaintenanceWindowScheduleRepository scheduleRepository;

    private final MaintenanceWindowScheduleValidator scheduleValidator;

    private final MaintenanceWindowScheduleConverter scheduleConverter;

    private final Clock clock;

    @Inject
    public MaintenanceWindowScheduleService(
            MaintenanceWindowScheduleRepository scheduleRepository,
            MaintenanceWindowScheduleValidator scheduleValidator,
            MaintenanceWindowScheduleConverter scheduleConverter,
            Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleValidator = scheduleValidator;
        this.scheduleConverter = scheduleConverter;
        this.clock = clock;
    }

    @Transactional(TxType.REQUIRED)
    public MaintenanceWindowScheduleResponse create(MaintenanceWindowScheduleRequest request, String accountId, String userCrn) {
        MaintenanceWindowSchedule schedule = scheduleConverter.toEntity(request);
        schedule.setAccountId(accountId);
        if (scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                schedule.getAccountId(), schedule.getScopeType(), schedule.getScopeId()).isPresent()) {
            throw scheduleAlreadyExistsException(schedule);
        }

        scheduleValidator.validate(schedule, null);
        applyDefaultScheduleName(schedule);
        long now = clock.getCurrentTimeMillis();
        schedule.setArchived(false);
        schedule.setCreatedAt(now);
        schedule.setUpdatedAt(now);
        schedule.setCreatedBy(userCrn);
        schedule.setUpdatedBy(userCrn);
        try {
            return scheduleConverter.toResponse(scheduleRepository.save(schedule));
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            throw scheduleAlreadyExistsException(schedule, e);
        }
    }

    @Transactional(TxType.REQUIRED)
    public MaintenanceWindowScheduleResponse update(
            String accountId, MaintenanceScopeType scopeType, String scopeId,
            UpdateMaintenanceWindowScheduleRequest request, String userCrn) {
        MaintenanceWindowSchedule schedule = findRequired(accountId, scopeType, scopeId);
        scheduleConverter.applyUpdateRequest(schedule, request);
        scheduleValidator.validate(schedule, schedule.getId());
        schedule.setUpdatedAt(clock.getCurrentTimeMillis());
        schedule.setUpdatedBy(userCrn);
        return scheduleConverter.toResponse(scheduleRepository.save(schedule));
    }

    public MaintenanceWindowScheduleResponse get(String accountId, MaintenanceScopeType scopeType, String scopeId) {
        return scheduleConverter.toResponse(findRequired(accountId, scopeType, scopeId));
    }

    public MaintenanceWindowScheduleListResponse list(String accountId, MaintenanceScopeType scopeType, String scopeId) {
        List<MaintenanceWindowSchedule> schedules = scopeType != null
                ? scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(accountId, scopeType, scopeId)
                        .stream().toList()
                : scheduleRepository.findByAccountIdAndArchivedFalse(accountId);
        MaintenanceWindowScheduleListResponse response = new MaintenanceWindowScheduleListResponse();
        response.setSchedules(schedules.stream().map(scheduleConverter::toResponse).toList());
        return response;
    }

    public void delete(String accountId, MaintenanceScopeType scopeType, String scopeId, String userCrn) {
        MaintenanceWindowSchedule schedule = findRequired(accountId, scopeType, scopeId);
        schedule.setArchived(true);
        schedule.setUpdatedAt(clock.getCurrentTimeMillis());
        schedule.setUpdatedBy(userCrn);
        scheduleRepository.save(schedule);
    }

    public MaintenanceWindowSchedule findRequired(String accountId, MaintenanceScopeType scopeType, String scopeId) {
        return scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(accountId, scopeType, scopeId)
                .orElseThrow(notFound(String.format(
                        "Maintenance schedule not found for accountId=%s scopeType=%s scopeId=%s",
                        accountId, scopeType, scopeId)));
    }

    private void applyDefaultScheduleName(MaintenanceWindowSchedule schedule) {
        if (StringUtils.isBlank(schedule.getName()) && schedule.getScopeType() != null && StringUtils.isNotBlank(schedule.getScopeId())) {
            schedule.setName(schedule.getScopeType().name().toLowerCase(Locale.ROOT) + "_" + schedule.getScopeId());
        }
    }

    private ConflictException scheduleAlreadyExistsException(MaintenanceWindowSchedule schedule) {
        return scheduleAlreadyExistsException(schedule, null);
    }

    private ConflictException scheduleAlreadyExistsException(MaintenanceWindowSchedule schedule, Throwable cause) {
        String message = String.format(
                "Maintenance schedule already exists for scope %s/%s.", schedule.getScopeType(), schedule.getScopeId());
        return cause == null ? new ConflictException(message) : new ConflictException(message, cause);
    }
}
