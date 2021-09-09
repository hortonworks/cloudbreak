package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToStackConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;

@Component
public class ClusterTemplateV4RequestToClusterTemplateConverter {

    private final UserService userService;

    private final WorkspaceService workspaceService;

    private final CredentialClientService credentialClientService;

    private final DistroXV1RequestToStackV4RequestConverter stackV4RequestConverter;

    private final CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    private final StackV4RequestToStackConverter stackV4RequestToStackConverter;

    public ClusterTemplateV4RequestToClusterTemplateConverter(
            StackV4RequestToStackConverter stackV4RequestToStackConverter,
            WorkspaceService workspaceService,
            UserService userService,
            CloudbreakRestRequestThreadLocalService restRequestThreadLocalService,
            CredentialClientService credentialClientService,
            DistroXV1RequestToStackV4RequestConverter stackV4RequestConverter) {
        this.stackV4RequestToStackConverter = stackV4RequestToStackConverter;
        this.workspaceService = workspaceService;
        this.userService = userService;
        this.restRequestThreadLocalService = restRequestThreadLocalService;
        this.credentialClientService = credentialClientService;
        this.stackV4RequestConverter = stackV4RequestConverter;
    }

    public ClusterTemplate convert(ClusterTemplateV4Request source) {
        if (source.getDistroXTemplate() == null) {
            throw new BadRequestException("The Datahub template cannot be null.");
        }
        if (StringUtils.isEmpty(source.getDistroXTemplate().getEnvironmentName())) {
            throw new BadRequestException("The environmentName cannot be null.");
        }
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterTemplate.setWorkspace(workspace);
        StackV4Request stackV4Request = stackV4RequestConverter.convert(source.getDistroXTemplate());
        stackV4Request.setType(StackType.TEMPLATE);
        Stack stack = stackV4RequestToStackConverter.convert(stackV4Request);
        prepareEmptyInstanceMetadata(stack);
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setCloudPlatform(getCloudPlatform(source, stack));
        clusterTemplate.setName(source.getName());
        clusterTemplate.setDescription(source.getDescription());
        clusterTemplate.setDatalakeRequired(DatalakeRequired.OPTIONAL);
        clusterTemplate.setFeatureState(FeatureState.RELEASED);
        clusterTemplate.setStatus(ResourceStatus.USER_MANAGED);
        checkStackWhetherItsCapableOfProvidingCldrRuntimeVersion(stack);
        clusterTemplate.setClouderaRuntimeVersion(stack.getCluster().getBlueprint().getStackVersion());
        if (source.getType() == null) {
            clusterTemplate.setType(ClusterTemplateV4Type.OTHER);
        } else {
            clusterTemplate.setType(source.getType());
        }
        return clusterTemplate;
    }

    private void checkStackWhetherItsCapableOfProvidingCldrRuntimeVersion(Stack stack) {
        String msgBaseFormat = "Unable to determine Cloudera Runtime version since no %s has been found for stack: " + stack.getResourceCrn();
        if (stack.getCluster() == null) {
            throw new IllegalStateException(String.format(msgBaseFormat, "cluster"));
        } else if (stack.getCluster().getBlueprint() == null) {
            throw new IllegalStateException(String.format(msgBaseFormat, "blueprint"));
        }
    }

    public void prepareEmptyInstanceMetadata(Stack stack) {
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            instanceGroup.setInstanceMetaData(new HashSet<>());
        }
    }

    private String getCloudPlatform(ClusterTemplateV4Request source, Stack stack) {
        return source.getCloudPlatform() != null
                ? source.getCloudPlatform()
                : credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn()).cloudPlatform();
    }

}
