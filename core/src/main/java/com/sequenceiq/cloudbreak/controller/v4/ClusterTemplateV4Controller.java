package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

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
import com.sequenceiq.authorization.annotation.ResourceCrn;
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
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateViewService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ClusterTemplate.class)
public class ClusterTemplateV4Controller extends NotificationController implements ClusterTemplateV4Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateV4Controller.class);

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ClusterTemplateService clusterTemplateService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private EnvironmentServiceDecorator environmentServiceDecorator;

    @Inject
    private ClusterTemplateViewService clusterTemplateViewService;

    @Inject
    private SdxClientService sdxClientService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response post(Long workspaceId, @Valid ClusterTemplateV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        ClusterTemplate clusterTemplate = clusterTemplateService.createForLoggedInUser(converterUtil.convert(request, ClusterTemplate.class),
                threadLocalService.getRequestedWorkspaceId(), accountId, creator);
        return getByName(threadLocalService.getRequestedWorkspaceId(), clusterTemplate.getName());
    }

    @Override
    @DisableCheckPermissions
    public ClusterTemplateViewV4Responses list(Long workspaceId) {
        measure(() -> blueprintService.updateDefaultBlueprintCollection(threadLocalService.getRequestedWorkspaceId()),
                LOGGER, "Blueprints fetched in {}ms");
        measure(() -> clusterTemplateService.updateDefaultClusterTemplates(threadLocalService.getRequestedWorkspaceId()),
                LOGGER, "Cluster templates fetched in {}ms");
        Set<ClusterTemplateViewV4Response> result = measure(() -> clusterTemplateService.listInWorkspaceAndCleanUpInvalids(
                threadLocalService.getRequestedWorkspaceId()), LOGGER, "cluster templates cleaned in {}ms");
        return new ClusterTemplateViewV4Responses(result);
    }

    @Override
    @DisableCheckPermissions
    public ClusterTemplateViewV4Responses listByEnv(Long workspaceId, String environmentCrn) {
        measure(() -> blueprintService.updateDefaultBlueprintCollection(threadLocalService.getRequestedWorkspaceId()),
                LOGGER, "Blueprints fetched in {}ms");
        measure(() -> clusterTemplateService.updateDefaultClusterTemplates(threadLocalService.getRequestedWorkspaceId()),
                LOGGER, "Cluster templates fetched in {}ms");
        List<SdxClusterResponse> sdxClusters = sdxClientService.getByEnvironmentCrn(environmentCrn);
        Optional<String> cloudPlatformByCrn = environmentClientService.getCloudPlatformByCrn(environmentCrn);
        Optional<String> runtimeVersion = sdxClusters.stream()
                .map(SdxClusterResponse::getRuntime)
                .filter(e -> !Strings.isNullOrEmpty(e))
                .findFirst();
        Set<ClusterTemplateView> clusterTemplateViews = clusterTemplateViewService.findAllUserManagedAndDefaultByEnvironmentCrn(
                threadLocalService.getRequestedWorkspaceId(), environmentCrn, cloudPlatformByCrn.orElse(null), runtimeVersion.orElse(null));
        Set<ClusterTemplateViewV4Response> result = converterUtil.convertAllAsSet(clusterTemplateViews, ClusterTemplateViewV4Response.class);
        return new ClusterTemplateViewV4Responses(result);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response getByName(Long workspaceId, @ResourceName String name) {
        try {
            ClusterTemplate clusterTemplate = transactionService.required(() ->
                    clusterTemplateService.getByNameForWorkspaceId(name, threadLocalService.getRequestedWorkspaceId()));
            ClusterTemplateV4Response response = transactionService.required(() -> converterUtil.convert(clusterTemplate, ClusterTemplateV4Response.class));
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
        return converterUtil.convert(clusterTemplate, ClusterTemplateV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response getByCrn(Long workspaceId, @ResourceCrn String crn) {
        try {
            ClusterTemplate clusterTemplate = transactionService.required(() ->
                    clusterTemplateService.getByCrn(crn, threadLocalService.getRequestedWorkspaceId()));
            ClusterTemplateV4Response response = transactionService.required(() -> converterUtil.convert(clusterTemplate, ClusterTemplateV4Response.class));
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
        ClusterTemplate clusterTemplate = clusterTemplateService.deleteByCrn(crn, threadLocalService.getRequestedWorkspaceId());
        return converterUtil.convert(clusterTemplate, ClusterTemplateV4Response.class);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Responses deleteMultiple(Long workspaceId, @ResourceNameList Set<String> names, String environmentName, String environmentCrn) {
        Set<ClusterTemplate> clusterTemplates;
        if (Objects.nonNull(names) && !names.isEmpty()) {
            clusterTemplates = clusterTemplateService.deleteMultiple(names, threadLocalService.getRequestedWorkspaceId());
        } else {
            Optional<String> cloudPlatformByCrn = environmentClientService.getCloudPlatformByCrn(environmentCrn);
            Set<String> namesByEnv = clusterTemplateService
                    .findAllByEnvironment(threadLocalService.getRequestedWorkspaceId(), environmentCrn,
                            cloudPlatformByCrn.orElse(null), null)
                    .stream()
                    .filter(e -> !ResourceStatus.DEFAULT.equals(e.getStatus()))
                    .map(ClusterTemplateView::getName)
                    .collect(toSet());
            clusterTemplates = clusterTemplateService.deleteMultiple(namesByEnv, threadLocalService.getRequestedWorkspaceId());
        }
        return new ClusterTemplateV4Responses(converterUtil.convertAllAsSet(clusterTemplates, ClusterTemplateV4Response.class));
    }

}
