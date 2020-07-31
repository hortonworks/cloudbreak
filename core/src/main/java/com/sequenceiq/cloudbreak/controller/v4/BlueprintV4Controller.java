package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.cloudera.cdp.datahub.model.CreateClusterTemplateRequest;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Blueprint.class)
@AuthorizationResource
public class BlueprintV4Controller extends NotificationController implements BlueprintV4Endpoint {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    @DisableCheckPermissions
    public BlueprintV4ViewResponses list(Long workspaceId, Boolean withSdx) {
        Set<BlueprintView> allAvailableViewInWorkspace = blueprintService.getAllAvailableViewInWorkspaceAndFilterBySdxReady(workspaceId, withSdx);
        return new BlueprintV4ViewResponses(converterUtil.convertAllAsSet(allAvailableViewInWorkspace, BlueprintV4ViewResponse.class));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public BlueprintV4Response getByName(Long workspaceId, @ResourceName @NotNull String name) {
        Blueprint blueprint = blueprintService.getByWorkspace(NameOrCrn.ofName(name), workspaceId);
        return converterUtil.convert(blueprint, BlueprintV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public BlueprintV4Response getByCrn(Long workspaceId, @NotNull @ResourceCrn String crn) {
        Blueprint blueprint = blueprintService.getByWorkspace(NameOrCrn.ofCrn(crn), workspaceId);
        return converterUtil.convert(blueprint, BlueprintV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CLUSTER_TEMPLATE)
    public BlueprintV4Response post(Long workspaceId, BlueprintV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        Blueprint toSave = converterUtil.convert(request, Blueprint.class);
        Blueprint blueprint = blueprintService.createForLoggedInUser(toSave, workspaceId, accountId, creator);
        notify(ResourceEvent.BLUEPRINT_CREATED);
        return converterUtil.convert(blueprint, BlueprintV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_CLUSTER_TEMPLATE)
    public BlueprintV4Response deleteByName(Long workspaceId, @NotNull @ResourceName String name) {
        Blueprint deleted = blueprintService.deleteByWorkspace(NameOrCrn.ofName(name), workspaceId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return converterUtil.convert(deleted, BlueprintV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_CLUSTER_TEMPLATE)
    public BlueprintV4Response deleteByCrn(Long workspaceId, @NotNull @ResourceCrn String crn) {
        Blueprint deleted = blueprintService.deleteByWorkspace(NameOrCrn.ofCrn(crn), workspaceId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return converterUtil.convert(deleted, BlueprintV4Response.class);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_CLUSTER_TEMPLATE)
    public BlueprintV4Responses deleteMultiple(Long workspaceId, @ResourceNameList Set<String> names) {
        Set<Blueprint> deleted = blueprintService.deleteMultipleByNameFromWorkspace(names, workspaceId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return new BlueprintV4Responses(converterUtil.convertAllAsSet(deleted, BlueprintV4Response.class));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public BlueprintV4Request getRequest(Long workspaceId, @ResourceName String name) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(blueprint, BlueprintV4Request.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public ParametersQueryV4Response getParameters(Long workspaceId, @ResourceName String name) {
        ParametersQueryV4Response parametersQueryV4Response = new ParametersQueryV4Response();
        parametersQueryV4Response.setCustom(blueprintService.queryCustomParametersMap(name, workspaceId));
        return parametersQueryV4Response;
    }

    @Override
    @DisableCheckPermissions
    public CreateClusterTemplateRequest getCreateClusterTemplateRequestForCli(Long workspaceId, BlueprintV4Request blueprintV4Request) {
        return converterUtil.convert(blueprintV4Request, CreateClusterTemplateRequest.class);
    }
}
