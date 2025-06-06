package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintToBlueprintV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintToBlueprintV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintV4RequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintViewToBlueprintV4ViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Blueprint.class)
public class BlueprintV4Controller extends NotificationController implements BlueprintV4Endpoint {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private BlueprintViewToBlueprintV4ViewResponseConverter blueprintViewToBlueprintV4ViewResponseConverter;

    @Inject
    private BlueprintToBlueprintV4ResponseConverter blueprintToBlueprintV4ResponseConverter;

    @Inject
    private BlueprintToBlueprintV4RequestConverter blueprintToBlueprintV4RequestConverter;

    @Inject
    private BlueprintV4RequestToBlueprintConverter blueprintV4RequestToBlueprintConverter;

    @Override
    @DisableCheckPermissions
    public BlueprintV4ViewResponses list(Long workspaceId, Boolean withSdx) {
        Set<BlueprintView> allAvailableViewInWorkspace = blueprintService.getAllAvailableViewInWorkspaceAndFilterBySdxReady(
                restRequestThreadLocalService.getRequestedWorkspaceId(), withSdx);
        return new BlueprintV4ViewResponses(
                allAvailableViewInWorkspace.stream()
                        .map(b -> blueprintViewToBlueprintV4ViewResponseConverter.convert(b))
                        .collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public BlueprintV4Response getByName(Long workspaceId, @ResourceName String name) {
        Blueprint blueprint = blueprintService.getByWorkspace(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        return blueprintToBlueprintV4ResponseConverter.convert(blueprint);
    }

    @Override
    @InternalOnly
    public BlueprintV4Response getByNameInternal(Long workspaceId, @AccountId String accountId, String name) {
        return getByName(restRequestThreadLocalService.getRequestedWorkspaceId(), name);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public BlueprintV4Response getByCrn(Long workspaceId, @ResourceCrn String crn) {
        Blueprint blueprint = blueprintService.getByWorkspace(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
        return blueprintToBlueprintV4ResponseConverter.convert(blueprint);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CLUSTER_TEMPLATE)
    public BlueprintV4Response post(Long workspaceId, BlueprintV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        Blueprint toSave = blueprintV4RequestToBlueprintConverter.convert(request);
        Blueprint blueprint = blueprintService.createForLoggedInUser(toSave, restRequestThreadLocalService.getRequestedWorkspaceId(), accountId, creator);
        notify(ResourceEvent.BLUEPRINT_CREATED);
        return blueprintToBlueprintV4ResponseConverter.convert(blueprint);
    }

    @Override
    @InternalOnly
    public BlueprintV4Response postInternal(@AccountId String accountId, Long workspaceId, BlueprintV4Request request) {
        Blueprint toSave = blueprintV4RequestToBlueprintConverter.convert(request);
        Blueprint blueprint = blueprintService.createWithInternalUser(toSave, restRequestThreadLocalService.getRequestedWorkspaceId(), accountId);
        notify(ResourceEvent.BLUEPRINT_CREATED);
        return blueprintToBlueprintV4ResponseConverter.convert(blueprint);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_CLUSTER_TEMPLATE)
    public BlueprintV4Response deleteByName(Long workspaceId, @ResourceName String name) {
        Blueprint deleted = blueprintService.deleteByWorkspace(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return blueprintToBlueprintV4ResponseConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_CLUSTER_TEMPLATE)
    public BlueprintV4Response deleteByCrn(Long workspaceId, @ResourceCrn String crn) {
        Blueprint deleted = blueprintService.deleteByWorkspace(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return blueprintToBlueprintV4ResponseConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_CLUSTER_TEMPLATE)
    public BlueprintV4Responses deleteMultiple(Long workspaceId, @ResourceNameList Set<String> names) {
        Set<Blueprint> deleted = blueprintService.deleteMultipleByNameFromWorkspace(names, restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return new BlueprintV4Responses(deleted.stream()
                .map(b -> blueprintToBlueprintV4ResponseConverter.convert(b))
                .collect(Collectors.toSet())
        );
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public BlueprintV4Request getRequest(Long workspaceId, @ResourceName String name) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(name, restRequestThreadLocalService.getRequestedWorkspaceId());
        return blueprintToBlueprintV4RequestConverter.convert(blueprint);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public ParametersQueryV4Response getParameters(Long workspaceId, @ResourceName String name) {
        ParametersQueryV4Response parametersQueryV4Response = new ParametersQueryV4Response();
        parametersQueryV4Response.setCustom(blueprintService.queryCustomParametersMap(name, restRequestThreadLocalService.getRequestedWorkspaceId()));
        return parametersQueryV4Response;
    }
}
