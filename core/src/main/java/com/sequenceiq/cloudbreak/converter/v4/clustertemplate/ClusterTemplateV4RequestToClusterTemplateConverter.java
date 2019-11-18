package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;

@Component
public class ClusterTemplateV4RequestToClusterTemplateConverter extends AbstractConversionServiceAwareConverter<ClusterTemplateV4Request, ClusterTemplate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateV4RequestToClusterTemplateConverter.class);

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private DistroXV1RequestToStackV4RequestConverter stackV4RequestConverter;

    @Override
    public ClusterTemplate convert(ClusterTemplateV4Request source) {
        if (StringUtils.isEmpty(source.getDistroXTemplate().getEnvironmentName())) {
            throw new BadRequestException("The environmentName cannot be null.");
        }

        ClusterTemplate clusterTemplate = new ClusterTemplate();
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterTemplate.setWorkspace(workspace);
        StackV4Request stackV4Request = stackV4RequestConverter.convertAsTemplate(source.getDistroXTemplate(), null);
        stackV4Request.setType(StackType.TEMPLATE);
        Stack stack = converterUtil.convert(stackV4Request, Stack.class);
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setCloudPlatform(getCloudPlatform(source, stack));
        clusterTemplate.setName(source.getName());
        clusterTemplate.setDescription(source.getDescription());
        clusterTemplate.setDatalakeRequired(DatalakeRequired.OPTIONAL);
        clusterTemplate.setFeatureState(FeatureState.RELEASED);
        clusterTemplate.setStatus(ResourceStatus.USER_MANAGED);
        if (source.getType() == null) {
            clusterTemplate.setType(ClusterTemplateV4Type.OTHER);
        } else {
            clusterTemplate.setType(source.getType());
        }
        return clusterTemplate;
    }

    private String getCloudPlatform(ClusterTemplateV4Request source, Stack stack) {
        return source.getCloudPlatform() != null
                ? source.getCloudPlatform()
                : credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn()).cloudPlatform();
    }
}
