package com.sequenceiq.cloudbreak.converter.v4.stacks.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.UserViewV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;

@Component
public class StackApiViewToStackViewV4ResponseConverter extends AbstractConversionServiceAwareConverter<StackApiView, StackViewV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackApiViewToStackViewV4ResponseConverter.class);

    @Override
    public StackViewV4Response convert(StackApiView source) {
        StackViewV4Response stackViewResponse = new StackViewV4Response();
        stackViewResponse.setCrn(source.getResourceCrn());
        stackViewResponse.setName(source.getName());
        if (source.getCluster() != null) {
            stackViewResponse.setCluster(getConversionService().convert(source.getCluster(), ClusterViewV4Response.class));
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
        return stackViewResponse;
    }

    private void addUser(StackApiView source, StackViewV4Response stackJson) {
        if (source.getUserView() != null) {
            try {
                UserViewV4Response userViewResponse = getConversionService().convert(source.getUserView(), UserViewV4Response.class);
                stackJson.setUser(userViewResponse);
            } catch (Exception ex) {
                LOGGER.warn("User could not be added to stack response.", ex);
            }
        }
    }
}
