package com.sequenceiq.cloudbreak.converter.v4.stacks.view;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.UserViewV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.database.ExternalDatabaseToDatabaseResponseConverter;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;

@Component
public class StackApiViewToStackViewV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackApiViewToStackViewV4ResponseConverter.class);

    @Inject
    private ClusterApiViewToClusterViewV4ResponseConverter clusterApiViewToClusterViewV4ResponseConverter;

    @Inject
    private UserViewToUserViewV4ResponseConverter userViewToUserViewV4ResponseConverter;

    @Inject
    private ExternalDatabaseToDatabaseResponseConverter externalDatabaseToDatabaseResponseConverter;

    public StackViewV4Response convert(StackApiView source) {
        StackViewV4Response stackViewResponse = new StackViewV4Response();
        stackViewResponse.setCrn(source.getResourceCrn());
        stackViewResponse.setName(source.getName());
        if (source.getCluster() != null) {
            stackViewResponse.setCluster(clusterApiViewToClusterViewV4ResponseConverter
                    .convert(source.getCluster(), source.getInstanceGroups()));
        }
        stackViewResponse.setTunnel(source.getTunnel());
        stackViewResponse.setNodeCount(source.getNodeCount());
        stackViewResponse.setStatus(source.getStatus());
        stackViewResponse.setCreated(source.getCreated());
        stackViewResponse.setTerminated(source.getTerminated());
        addUser(source, stackViewResponse);
        stackViewResponse.setCloudPlatform(source.getCloudPlatform());
        stackViewResponse.setEnvironmentCrn(source.getEnvironmentCrn());
        stackViewResponse.setStackVersion(source.getStackVersion());
        stackViewResponse.setVariant(Strings.isNullOrEmpty(source.getPlatformVariant()) ? source.getCloudPlatform() : source.getPlatformVariant());
        stackViewResponse.setExternalDatabase(
                externalDatabaseToDatabaseResponseConverter.convert(source.getExternalDatabaseCreationType(), source.getExternalDatabaseEngineVersion()));
        stackViewResponse.setProviderSyncStates(source.getProviderSyncStates());
        return stackViewResponse;
    }

    private void addUser(StackApiView source, StackViewV4Response stackJson) {
        if (source.getUserView() != null) {
            try {
                UserViewV4Response userViewResponse = userViewToUserViewV4ResponseConverter
                        .convert(source.getUserView());
                stackJson.setUser(userViewResponse);
            } catch (Exception ex) {
                LOGGER.warn("User could not be added to stack response.", ex);
            }
        }
    }
}
