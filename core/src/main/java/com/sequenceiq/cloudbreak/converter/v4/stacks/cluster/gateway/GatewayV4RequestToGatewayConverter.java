package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class GatewayV4RequestToGatewayConverter {

    @Inject
    private GatewayConvertUtil gatewayConvertUtil;

    @Inject
    private GatewayV4RequestValidator gatewayJsonValidator;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    public Gateway convert(GatewayV4Request source) {
        ValidationResult validationResult = gatewayJsonValidator.validate(source);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        if (CollectionUtils.isEmpty(source.getTopologies())) {
            return null;
        }
        Gateway gateway = new Gateway();
        gatewayConvertUtil.setBasicProperties(source, gateway);
        gatewayConvertUtil.setTopologies(source, gateway);
        gatewayConvertUtil.setGatewayPathAndSsoProvider(source, gateway);

        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        gateway.setWorkspace(workspace);
        return gateway;
    }
}
