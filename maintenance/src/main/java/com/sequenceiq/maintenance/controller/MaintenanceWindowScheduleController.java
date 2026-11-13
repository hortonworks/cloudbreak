package com.sequenceiq.maintenance.controller;

import static com.sequenceiq.maintenance.authorization.MaintenanceWindowScheduleAccessMode.READ;
import static com.sequenceiq.maintenance.authorization.MaintenanceWindowScheduleAccessMode.WRITE;
import static com.sequenceiq.maintenance.domain.MaintenanceEnumValues.toScopeType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.endpoint.MaintenanceWindowScheduleEndpoint;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowEffectiveScheduleParams;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleListParams;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowSchedulePreviewRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowSkipRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowEffectiveScheduleResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleListResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowSchedulePreviewResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowSkipResponse;
import com.sequenceiq.maintenance.authorization.MaintenanceScopeKey;
import com.sequenceiq.maintenance.authorization.MaintenanceWindowScheduleAuthorizationService;
import com.sequenceiq.maintenance.authorization.MaintenanceWindowScheduleEntitlementService;
import com.sequenceiq.maintenance.service.MaintenanceWindowSchedulePreviewService;
import com.sequenceiq.maintenance.service.MaintenanceWindowScheduleService;
import com.sequenceiq.maintenance.service.MaintenanceWindowSkipService;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleLookupContext;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleResolution;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleResourceContext;

@Controller
public class MaintenanceWindowScheduleController implements MaintenanceWindowScheduleEndpoint {

    private final MaintenanceWindowScheduleService scheduleService;

    private final MaintenanceWindowSkipService skipService;

    private final MaintenanceWindowSchedulePreviewService previewService;

    private final MaintenanceWindowScheduleAuthorizationService scheduleAuthorization;

    private final MaintenanceWindowScheduleEntitlementService scheduleEntitlementService;

    @Context
    private UriInfo uriInfo;

    public MaintenanceWindowScheduleController(
            MaintenanceWindowScheduleService scheduleService,
            MaintenanceWindowSkipService skipService,
            MaintenanceWindowSchedulePreviewService previewService,
            MaintenanceWindowScheduleAuthorizationService scheduleAuthorization,
            MaintenanceWindowScheduleEntitlementService scheduleEntitlementService) {
        this.scheduleService = scheduleService;
        this.skipService = skipService;
        this.previewService = previewService;
        this.scheduleAuthorization = scheduleAuthorization;
        this.scheduleEntitlementService = scheduleEntitlementService;
    }

    @Override
    @CustomPermissionCheck
    public MaintenanceWindowScheduleListResponse list(MaintenanceWindowScheduleListParams params) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        scheduleEntitlementService.verifyEntitled(accountId);
        MaintenanceScopeType scopeType = toScopeType(params.getScopeType());
        if (scopeType != null) {
            scheduleService.validateScope(scopeType, params.getScopeId(), accountId);
            scheduleAuthorization.authorize(scopeType, params.getScopeId(), READ);
            return scheduleService.list(accountId, scopeType, params.getScopeId());
        }
        MaintenanceWindowScheduleListResponse all = scheduleService.list(accountId, null, null);
        Set<MaintenanceScopeKey> readable = scheduleAuthorization.filterReadable(all.getSchedules().stream()
                .map(schedule -> new MaintenanceScopeKey(
                        MaintenanceScopeType.valueOf(schedule.getScopeType()), schedule.getScopeId()))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        all.setSchedules(all.getSchedules().stream()
                .filter(schedule -> readable.contains(new MaintenanceScopeKey(
                        MaintenanceScopeType.valueOf(schedule.getScopeType()), schedule.getScopeId())))
                .toList());
        return all;
    }

    @Override
    @CustomPermissionCheck
    public MaintenanceWindowEffectiveScheduleResponse getEffective(MaintenanceWindowEffectiveScheduleParams params) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        scheduleEntitlementService.verifyEntitled(accountId);
        EffectiveScheduleResourceContext resourceContext = scheduleService.validateEffectiveScheduleResource(accountId, params);
        scheduleAuthorization.authorize(resourceContext.resourceScopeType(), resourceContext.resourceCrn(), READ);
        EffectiveScheduleLookupContext lookup = scheduleService.completeEffectiveScheduleLookup(resourceContext, params);
        EffectiveScheduleResolution resolution = scheduleService.resolveEffectiveSchedule(lookup);
        if (resolution.sourceScopeType() == MaintenanceScopeType.ENVIRONMENT) {
            scheduleAuthorization.authorize(MaintenanceScopeType.ENVIRONMENT, resolution.sourceScopeId(), READ);
        }
        return scheduleService.toEffectiveResponse(resolution);
    }

    @Override
    @CustomPermissionCheck
    public MaintenanceWindowSchedulePreviewResponse preview(MaintenanceWindowSchedulePreviewRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        scheduleEntitlementService.verifyEntitled(accountId);
        if (StringUtils.isNotBlank(request.getScopeType()) && StringUtils.isNotBlank(request.getScopeId())) {
            MaintenanceScopeType scope = toScopeType(request.getScopeType());
            scheduleService.validateScope(scope, request.getScopeId(), accountId);
            scheduleAuthorization.authorize(scope, request.getScopeId(), READ);
        } else {
            scheduleAuthorization.authorize(MaintenanceScopeType.TENANT, accountId, READ);
        }
        return previewService.preview(request, accountId);
    }

    @Override
    @CustomPermissionCheck
    public MaintenanceWindowScheduleResponse get(String scopeType, String scopeId) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        scheduleEntitlementService.verifyEntitled(accountId);
        MaintenanceScopeType scope = toScopeType(scopeType);
        scheduleService.validateScope(scope, scopeId, accountId);
        scheduleAuthorization.authorize(scope, scopeId, READ);
        return scheduleService.get(accountId, scope, scopeId);
    }

    @Override
    @CustomPermissionCheck
    public Response create(MaintenanceWindowScheduleRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        scheduleEntitlementService.verifyEntitled(accountId);
        MaintenanceScopeType scope = toScopeType(request.getScopeType());
        scheduleService.validateScope(scope, request.getScopeId(), accountId);
        scheduleAuthorization.authorize(scope, request.getScopeId(), WRITE);
        MaintenanceWindowScheduleResponse response = scheduleService.create(
                request,
                accountId,
                ThreadBasedUserCrnProvider.getUserCrn());
        return Response.status(Response.Status.CREATED)
                .location(uriInfo.getAbsolutePathBuilder()
                        .path("scope")
                        .path(response.getScopeType())
                        .path("scopeId")
                        .path(response.getScopeId())
                        .build())
                .entity(response)
                .build();
    }

    @Override
    @CustomPermissionCheck
    public MaintenanceWindowScheduleResponse update(
            String scopeType, String scopeId, UpdateMaintenanceWindowScheduleRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        scheduleEntitlementService.verifyEntitled(accountId);
        MaintenanceScopeType scope = toScopeType(scopeType);
        scheduleService.validateScope(scope, scopeId, accountId);
        scheduleAuthorization.authorize(scope, scopeId, WRITE);
        return scheduleService.update(
                accountId,
                scope,
                scopeId,
                request,
                ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CustomPermissionCheck
    public void delete(String scopeType, String scopeId) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        scheduleEntitlementService.verifyEntitled(accountId);
        MaintenanceScopeType scope = toScopeType(scopeType);
        scheduleService.validateScope(scope, scopeId, accountId);
        scheduleAuthorization.authorize(scope, scopeId, WRITE);
        scheduleService.delete(
                accountId,
                scope,
                scopeId,
                ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CustomPermissionCheck
    public MaintenanceWindowSkipResponse skipNextWindow(
            String scopeType, String scopeId, MaintenanceWindowSkipRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        scheduleEntitlementService.verifyEntitled(accountId);
        MaintenanceScopeType scope = toScopeType(scopeType);
        scheduleService.validateScope(scope, scopeId, accountId);
        scheduleAuthorization.authorize(scope, scopeId, WRITE);
        return skipService.skipNextWindow(
                accountId,
                scope,
                scopeId,
                request,
                ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CustomPermissionCheck
    public MaintenanceWindowSkipResponse cancelSkipNextWindow(String scopeType, String scopeId) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        scheduleEntitlementService.verifyEntitled(accountId);
        MaintenanceScopeType scope = toScopeType(scopeType);
        scheduleService.validateScope(scope, scopeId, accountId);
        scheduleAuthorization.authorize(scope, scopeId, WRITE);
        return skipService.cancelSkipNextWindow(
                accountId,
                scope,
                scopeId);
    }
}
