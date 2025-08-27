package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static java.util.stream.Collectors.toSet;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.ClusterTemplateToClusterTemplateV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.ClusterTemplateV4RequestToClusterTemplateConverter;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.ClusterTemplateViewToClusterTemplateViewV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateViewService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentBaseResponse;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ClusterTemplate.class)
public class ClusterTemplateV4Controller extends NotificationController implements ClusterTemplateV4Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateV4Controller.class);

    @Inject
    private ClusterTemplateService clusterTemplateService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private EnvironmentServiceDecorator environmentServiceDecorator;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ClusterTemplateViewService clusterTemplateViewService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Inject
    private ClusterTemplateV4RequestToClusterTemplateConverter clusterTemplateV4RequestToClusterTemplateConverter;

    @Inject
    private ClusterTemplateToClusterTemplateV4ResponseConverter clusterTemplateToClusterTemplateV4ResponseConverter;

    @Inject
    private ClusterTemplateViewToClusterTemplateViewV4ResponseConverter clusterTemplateViewToClusterTemplateViewV4ResponseConverter;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response post(Long workspaceId, ClusterTemplateV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        ClusterTemplate clusterTemplate = clusterTemplateService.createForLoggedInUser(
                clusterTemplateV4RequestToClusterTemplateConverter.convert(request),
                threadLocalService.getRequestedWorkspaceId(), accountId, creator);
        return getByName(threadLocalService.getRequestedWorkspaceId(), clusterTemplate.getName());
    }

    @Override
    @DisableCheckPermissions
    public ClusterTemplateViewV4Responses list(Long workspaceId) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        measure(() -> blueprintService.updateDefaultBlueprintCollection(threadLocalService.getRequestedWorkspaceId()),
                LOGGER, "Blueprints fetched in {}ms");
        measure(() -> clusterTemplateService.updateDefaultClusterTemplates(threadLocalService.getRequestedWorkspaceId()),
                LOGGER, "Cluster definitions fetched in {}ms");
        Set<ClusterTemplateViewV4Response> result = measure(() -> clusterTemplateService.listInWorkspaceAndCleanUpInvalids(
                threadLocalService.getRequestedWorkspaceId(), accountId), LOGGER, "cluster definitions cleaned in {}ms");
        return new ClusterTemplateViewV4Responses(result);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public ClusterTemplateViewV4Responses listByEnv(Long workspaceId, @ResourceCrn String environmentCrn) {
        boolean internalTenant = isInternalTenant();
        measure(() -> blueprintService.updateDefaultBlueprintCollection(threadLocalService.getRequestedWorkspaceId()),
                LOGGER, "Blueprints fetched in {}ms");
        measure(() -> clusterTemplateService.updateDefaultClusterTemplates(threadLocalService.getRequestedWorkspaceId()),
                LOGGER, "Cluster definitions fetched in {}ms");
        Optional<SdxBasicView> sdxBasicView = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(environmentCrn);
        Optional<DetailedEnvironmentResponse> environment = getEnvironment(environmentCrn);
        Optional<String> cloudPlatformByCrn = environment.map(EnvironmentBaseResponse::getCloudPlatform);
        Boolean hybridEnvironment = environment.map(env -> EnvironmentType.HYBRID.name().equalsIgnoreCase(env.getEnvironmentType())).orElse(null);
        Optional<String> runtimeVersion = sdxBasicView.stream()
                .map(SdxBasicView::runtime)
                .filter(e -> !Strings.isNullOrEmpty(e))
                .findFirst();
        Set<ClusterTemplateView> clusterTemplateViews = clusterTemplateViewService.findAllUserManagedAndDefaultByEnvironmentCrn(
                threadLocalService.getRequestedWorkspaceId(),
                environmentCrn,
                cloudPlatformByCrn.orElse(null),
                runtimeVersion.orElse(null),
                internalTenant,
                hybridEnvironment);
        Set<ClusterTemplateViewV4Response> result = clusterTemplateViews.stream()
                .map(s -> clusterTemplateViewToClusterTemplateViewV4ResponseConverter.convert(s))
                .collect(Collectors.toSet());
        return new ClusterTemplateViewV4Responses(result);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response getByName(Long workspaceId, @ResourceName String name) {
        try {
            ClusterTemplate clusterTemplate = transactionService.required(() ->
                    clusterTemplateService.getByNameForWorkspaceId(name, threadLocalService.getRequestedWorkspaceId()));
            ClusterTemplateV4Response response = transactionService.required(() ->
                    clusterTemplateToClusterTemplateV4ResponseConverter.convert(clusterTemplate));
            Optional.ofNullable(response.getEnvironmentCrn()).ifPresent(crn -> environmentServiceDecorator.prepareEnvironment(response));
            return response;
        } catch (TransactionExecutionException cse) {
            LOGGER.warn("Unable to find cluster definition due to " + cse.getMessage(), cse.getCause());
            throw new CloudbreakServiceException("Unable to obtain cluster definition!", cse.getCause());
        }
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response deleteByName(Long workspaceId, @ResourceName String name) {
        ClusterTemplate clusterTemplate = clusterTemplateService.deleteByName(name, threadLocalService.getRequestedWorkspaceId());
        return clusterTemplateToClusterTemplateV4ResponseConverter.convert(clusterTemplate);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response getByCrn(Long workspaceId, @ResourceCrn String crn) {
        boolean internalTenant = isInternalTenant();
        try {
            ClusterTemplate clusterTemplate = transactionService.required(() ->
                    clusterTemplateService.getByCrn(crn, threadLocalService.getRequestedWorkspaceId(), internalTenant));
            ClusterTemplateV4Response response = transactionService.required(() ->
                    clusterTemplateToClusterTemplateV4ResponseConverter.convert(clusterTemplate));
            if (!StringUtils.isEmpty(response.getEnvironmentCrn())) {
                environmentServiceDecorator.prepareEnvironment(response);
            } else {
                LOGGER.warn("Skipping response decoration with environment name. Environment CRN was empty.");
            }
            return response;
        } catch (TransactionExecutionException cse) {
            LOGGER.warn("Unable to find cluster definition due to {}", cse.getMessage());
            throw new CloudbreakServiceException("Unable to obtain cluster definition!");
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response deleteByCrn(Long workspaceId, @ResourceCrn String crn) {
        boolean internalTenant = isInternalTenant();
        ClusterTemplate clusterTemplate = clusterTemplateService.deleteByCrn(crn, threadLocalService.getRequestedWorkspaceId(), internalTenant);
        return clusterTemplateToClusterTemplateV4ResponseConverter.convert(clusterTemplate);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Responses deleteMultiple(Long workspaceId, @ResourceNameList Set<String> names, String environmentName, String environmentCrn) {
        Set<ClusterTemplate> clusterTemplates;
        if (Objects.nonNull(names) && !names.isEmpty()) {
            clusterTemplates = clusterTemplateService.deleteMultiple(names, threadLocalService.getRequestedWorkspaceId());
        } else {
            Optional<String> cloudPlatformByCrn = getEnvironment(environmentCrn).map(EnvironmentBaseResponse::getCloudPlatform);

            Set<String> namesByEnv = clusterTemplateService
                    .findAllByEnvironment(threadLocalService.getRequestedWorkspaceId(), environmentCrn,
                            cloudPlatformByCrn.orElse(null), null, true, null)
                    .stream()
                    .filter(e -> !ResourceStatus.DEFAULT.equals(e.getStatus()))
                    .map(ClusterTemplateView::getName)
                    .collect(toSet());
            clusterTemplates = clusterTemplateService.deleteMultiple(namesByEnv, threadLocalService.getRequestedWorkspaceId());
        }
        return new ClusterTemplateV4Responses(
                clusterTemplates.stream()
                .map(c -> clusterTemplateToClusterTemplateV4ResponseConverter.convert(c))
                .collect(toSet()));
    }

    private boolean isInternalTenant() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return entitlementService.internalTenant(accountId);
    }

    private Optional<DetailedEnvironmentResponse> getEnvironment(String crn) {
        try {
            return Optional.ofNullable(environmentClientService.getByCrn(crn));
        } catch (CloudbreakServiceException e) {
            return Optional.empty();
        }
    }

}
