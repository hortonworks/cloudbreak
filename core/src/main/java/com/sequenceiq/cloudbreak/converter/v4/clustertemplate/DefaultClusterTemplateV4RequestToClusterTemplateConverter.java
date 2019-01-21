package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import java.util.Base64;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class DefaultClusterTemplateV4RequestToClusterTemplateConverter
        extends AbstractConversionServiceAwareConverter<DefaultClusterTemplateV4Request, ClusterTemplate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClusterTemplateV4RequestToClusterTemplateConverter.class);

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ClusterTemplate convert(DefaultClusterTemplateV4Request source) {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterTemplate.setWorkspace(workspace);
        Stack stack = converterUtil.convert(source.getStackTemplate(), Stack.class);
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setCloudPlatform(getCloudPlatform(source, stack));
        clusterTemplate.setName(source.getName());
        clusterTemplate.setDescription(source.getDescription());
        clusterTemplate.setStatus(ResourceStatus.DEFAULT);
        if (source.getType() == null) {
            clusterTemplate.setType(ClusterTemplateV4Type.OTHER);
        } else {
            clusterTemplate.setType(source.getType());
        }
        clusterTemplate.setDatalakeRequired(source.getDatalakeRequired());
        clusterTemplate.setTemplateContent(Base64.getEncoder().encodeToString(JsonUtil.writeValueAsStringSilent(source).getBytes()));
        return clusterTemplate;
    }

    private String getCloudPlatform(DefaultClusterTemplateV4Request source, Stack stack) {
        return source.getCloudPlatform() != null ? source.getCloudPlatform() : stack.getEnvironment().getCloudPlatform();

    }
}
