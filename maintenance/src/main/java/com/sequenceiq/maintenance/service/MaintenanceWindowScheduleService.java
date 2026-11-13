package com.sequenceiq.maintenance.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeTypes;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowEffectiveScheduleParams;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowEffectiveScheduleResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleListResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.exception.ConflictException;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleLookupContext;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleResolution;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleResourceContext;
import com.sequenceiq.maintenance.service.model.MaintenanceWindowResourceIdentity;

@Service
public class MaintenanceWindowScheduleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowScheduleService.class);

    private final MaintenanceWindowScheduleRepository scheduleRepository;

    private final MaintenanceWindowScheduleValidator scheduleValidator;

    private final MaintenanceWindowScheduleScopeValidator scopeValidator;

    private final MaintenanceWindowScheduleConverter scheduleConverter;

    private final MaintenanceWindowScheduleEligibilityService scheduleEligibilityService;

    private final MaintenanceResourceEnvironmentCrnResolver resourceEnvironmentCrnResolver;

    private final Clock clock;

    @Inject
    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    public MaintenanceWindowScheduleService(
            MaintenanceWindowScheduleRepository scheduleRepository,
            MaintenanceWindowScheduleValidator scheduleValidator,
            MaintenanceWindowScheduleScopeValidator scopeValidator,
            MaintenanceWindowScheduleConverter scheduleConverter,
            MaintenanceWindowScheduleEligibilityService scheduleEligibilityService,
            MaintenanceResourceEnvironmentCrnResolver resourceEnvironmentCrnResolver,
            Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleValidator = scheduleValidator;
        this.scopeValidator = scopeValidator;
        this.scheduleConverter = scheduleConverter;
        this.scheduleEligibilityService = scheduleEligibilityService;
        this.resourceEnvironmentCrnResolver = resourceEnvironmentCrnResolver;
        this.clock = clock;
    }

    /**
     * Validates {@code scopeType}/{@code scopeId} for the caller's account. Must run before UMS authorization.
     */
    public void validateScope(MaintenanceScopeType scopeType, String scopeId, String accountId) {
        scopeValidator.validate(scopeType, scopeId, accountId);
    }

    /**
     * Validates {@code resourceCrn} and optional {@code environmentCrn} shape/account only. Must run before UMS
     * authorization on the resource CRN and before any owning-service lookup.
     */
    public EffectiveScheduleResourceContext validateEffectiveScheduleResource(
            String accountId, MaintenanceWindowEffectiveScheduleParams params) {
        MaintenanceScopeType resourceScopeType = MaintenanceScopeTypes.fromResourceCrn(params.getResourceCrn());
        String resourceCrn = params.getResourceCrn();
        validateScope(resourceScopeType, resourceCrn, accountId);
        if (StringUtils.isNotBlank(params.getEnvironmentCrn())) {
            validateEnvironmentCrn(params.getEnvironmentCrn(), accountId);
        }
        return new EffectiveScheduleResourceContext(accountId, resourceScopeType, resourceCrn);
    }

    /**
     * Derives the parent environment from the owning service after resource authorization. When the resource is unknown
     * to the owner, environment inheritance is skipped so resource and tenant schedules can still apply.
     */
    public EffectiveScheduleLookupContext completeEffectiveScheduleLookup(
            EffectiveScheduleResourceContext resourceContext, MaintenanceWindowEffectiveScheduleParams params) {
        String derivedEnvironmentCrn = resourceEnvironmentCrnResolver
                .resolveOptional(resourceContext.accountId(), resourceContext.resourceCrn(), resourceContext.resourceScopeType())
                .map(env -> {
                    validateEnvironmentCrn(env, resourceContext.accountId());
                    return env;
                })
                .orElse(null);
        if (StringUtils.isNotBlank(params.getEnvironmentCrn())) {
            if (derivedEnvironmentCrn == null) {
                throw new BadRequestException(
                        "Unable to resolve parent environment for resourceCrn; omit environmentCrn when the resource"
                                + " is not registered in the owning service.");
            }
            if (!params.getEnvironmentCrn().equals(derivedEnvironmentCrn)) {
                throw new BadRequestException(
                        "environmentCrn does not match the parent environment of resourceCrn.");
            }
        }
        return new EffectiveScheduleLookupContext(
                resourceContext.accountId(),
                resourceContext.resourceScopeType(),
                resourceContext.resourceCrn(),
                derivedEnvironmentCrn);
    }

    @Transactional(TxType.REQUIRED)
    public MaintenanceWindowScheduleResponse create(MaintenanceWindowScheduleRequest request, String accountId, String userCrn) {
        MaintenanceWindowSchedule schedule = scheduleConverter.toEntity(request);
        validateScope(schedule.getScopeType(), schedule.getScopeId(), accountId);
        schedule.setAccountId(accountId);
        if (scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                schedule.getAccountId(), schedule.getScopeType(), schedule.getScopeId()).isPresent()) {
            throw scheduleAlreadyExistsException(schedule);
        }

        scheduleValidator.validate(schedule);
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
        validateScope(scopeType, scopeId, accountId);
        MaintenanceWindowSchedule schedule = findRequired(accountId, scopeType, scopeId);
        scheduleConverter.applyUpdateRequest(schedule, request);
        scheduleValidator.validate(schedule);
        schedule.setUpdatedAt(clock.getCurrentTimeMillis());
        schedule.setUpdatedBy(userCrn);
        return scheduleConverter.toResponse(scheduleRepository.save(schedule));
    }

    public MaintenanceWindowScheduleResponse get(String accountId, MaintenanceScopeType scopeType, String scopeId) {
        validateScope(scopeType, scopeId, accountId);
        return scheduleConverter.toResponse(findRequired(accountId, scopeType, scopeId));
    }

    public EffectiveScheduleResolution resolveEffectiveSchedule(EffectiveScheduleLookupContext lookup) {
        MaintenanceWindowResourceIdentity identity = new MaintenanceWindowResourceIdentity(
                lookup.accountId(), lookup.environmentCrn(), lookup.resourceCrn(), lookup.resourceScopeType());
        return scheduleEligibilityService.resolveEffectiveScheduleWithSource(identity)
                .orElseThrow(notFound(String.format(
                        "No maintenance schedule applies to resourceCrn=%s", lookup.resourceCrn())));
    }

    public MaintenanceWindowEffectiveScheduleResponse toEffectiveResponse(EffectiveScheduleResolution resolution) {
        LOGGER.debug(
                "Effective maintenance schedule resolved from sourceScopeType={} sourceScopeId={} scheduleId={}",
                resolution.sourceScopeType(),
                resolution.sourceScopeId(),
                resolution.schedule().getId());
        return scheduleConverter.toEffectiveResponse(resolution);
    }

    public MaintenanceWindowScheduleListResponse list(String accountId, MaintenanceScopeType scopeType, String scopeId) {
        if (scopeType != null) {
            validateScope(scopeType, scopeId, accountId);
        }
        List<MaintenanceWindowSchedule> schedules = scopeType != null
                ? scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(accountId, scopeType, scopeId)
                        .stream().toList()
                : scheduleRepository.findByAccountIdAndArchivedFalse(accountId);
        MaintenanceWindowScheduleListResponse response = new MaintenanceWindowScheduleListResponse();
        response.setSchedules(schedules.stream().map(scheduleConverter::toResponse).toList());
        return response;
    }

    public void delete(String accountId, MaintenanceScopeType scopeType, String scopeId, String userCrn) {
        validateScope(scopeType, scopeId, accountId);
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

    private void validateCrnBelongsToAccount(String crnValue, String accountId, String fieldName) {
        if (!Crn.safeFromString(crnValue).getAccountId().equals(accountId)) {
            throw new BadRequestException(fieldName + " must belong to the same account as the caller.");
        }
    }

    private void validateEnvironmentCrn(String environmentCrn, String accountId) {
        if (!Crn.isCrn(environmentCrn)) {
            throw new BadRequestException("environmentCrn must be a valid CRN.");
        }
        Crn crn = Crn.fromString(environmentCrn);
        if (crn.getResourceType() != Crn.ResourceType.ENVIRONMENT) {
            throw new BadRequestException("environmentCrn must reference an environment resource.");
        }
        validateCrnBelongsToAccount(environmentCrn, accountId, "environmentCrn");
    }
}
