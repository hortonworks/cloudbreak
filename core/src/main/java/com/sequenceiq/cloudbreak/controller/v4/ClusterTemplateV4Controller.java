package com.sequenceiq.cloudbreak.controller.v4;

import static java.util.stream.Collectors.toSet;

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

import com.sequenceiq.authorization.annotation.AuthorizationResource;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ClusterTemplate.class)
@AuthorizationResource
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

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response post(Long workspaceId, @Valid ClusterTemplateV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        ClusterTemplate clusterTemplate = clusterTemplateService.createForLoggedInUser(converterUtil.convert(request, ClusterTemplate.class), workspaceId,
                accountId, creator);
        return getByName(workspaceId, clusterTemplate.getName());
    }

    @Override
    @DisableCheckPermissions
    public ClusterTemplateViewV4Responses list(Long workspaceId) {
        blueprintService.updateDefaultBlueprintCollection(workspaceId);
        clusterTemplateService.updateDefaultClusterTemplates(workspaceId);
        Set<ClusterTemplateViewV4Response> result = clusterTemplateService.listInWorkspaceAndCleanUpInvalids(workspaceId);
        return new ClusterTemplateViewV4Responses(result);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response getByName(Long workspaceId, @ResourceName String name) {
        try {
            ClusterTemplate clusterTemplate = transactionService.required(() -> clusterTemplateService.getByNameForWorkspaceId(name, workspaceId));
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
        ClusterTemplate clusterTemplate = clusterTemplateService.deleteByName(name, workspaceId);
        return converterUtil.convert(clusterTemplate, ClusterTemplateV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response getByCrn(Long workspaceId, @ResourceCrn String crn) {
        try {
            ClusterTemplate clusterTemplate = transactionService.required(() -> clusterTemplateService.getByCrn(crn, workspaceId));
            ClusterTemplateV4Response response = transactionService.required(() -> converterUtil.convert(clusterTemplate, ClusterTemplateV4Response.class));
            environmentServiceDecorator.prepareEnvironment(response);
            return response;
        } catch (TransactionExecutionException cse) {
            LOGGER.warn("Unable to find cluster definition due to {}", cse.getMessage());
            throw new CloudbreakServiceException("Unable to obtain cluster definition!");
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Response deleteByCrn(Long workspaceId, @ResourceCrn String crn) {
        ClusterTemplate clusterTemplate = clusterTemplateService.deleteByCrn(crn, workspaceId);
        return converterUtil.convert(clusterTemplate, ClusterTemplateV4Response.class);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_CLUSTER_DEFINITION)
    public ClusterTemplateV4Responses deleteMultiple(Long workspaceId, @ResourceNameList Set<String> names, String environmentName, String environmentCrn) {
        Set<ClusterTemplate> clusterTemplates;
        if (Objects.nonNull(names) && !names.isEmpty()) {
            clusterTemplates = clusterTemplateService.deleteMultiple(names, workspaceId);
        } else {
            NameOrCrn environmentNameOrCrn = NameOrCrn.ofCrn(environmentCrn);
            Set<String> namesByEnv = clusterTemplateService
                    .findAllByEnvironment(environmentNameOrCrn)
                    .stream()
                    .map(ClusterTemplate::getName)
                    .collect(toSet());
            clusterTemplates = clusterTemplateService.deleteMultiple(namesByEnv, workspaceId);
        }
        return new ClusterTemplateV4Responses(converterUtil.convertAllAsSet(clusterTemplates, ClusterTemplateV4Response.class));
    }

}
