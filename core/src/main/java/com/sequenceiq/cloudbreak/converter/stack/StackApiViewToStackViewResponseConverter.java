package com.sequenceiq.cloudbreak.converter.stack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.UserViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterViewResponse;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;

@Component
public class StackApiViewToStackViewResponseConverter extends AbstractConversionServiceAwareConverter<StackApiView, StackViewResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackApiViewToStackViewResponseConverter.class);

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public StackViewResponse convert(StackApiView source) {
        StackViewResponse stackViewResponse = new StackViewResponse();
        stackViewResponse.setId(source.getId());
        stackViewResponse.setName(source.getName());
        stackViewResponse.setCredential(getConversionService().convert(source.getCredential(), CredentialResponse.class));
        stackViewResponse.setParameters(Maps.newHashMap(source.getParameters()));
        if (source.getCluster() != null) {
            stackViewResponse.setCluster(conversionService.convert(source.getCluster(), ClusterViewResponse.class));
        }
        convertComponentConfig(source, stackViewResponse);
        addNodeCount(source, stackViewResponse);
        stackViewResponse.setCloudPlatform(source.getCloudPlatform());
        stackViewResponse.setPlatformVariant(source.getPlatformVariant());
        stackViewResponse.setStatus(source.getStatus());
        stackViewResponse.setCreated(source.getCreated());
        addFlexSubscription(source, stackViewResponse);
        addUser(source, stackViewResponse);
        if (source.getEnvironment() != null) {
            stackViewResponse.setEnvironment(source.getEnvironment().getName());
        }
        return stackViewResponse;
    }

    private void convertComponentConfig(StackApiView source, StackViewResponse stackJson) {
        try {
            if (source.getCluster() != null) {
                StackRepoDetails stackRepoDetails = clusterComponentConfigProvider.getHDPRepo(source.getCluster().getId());
                if (stackRepoDetails != null && stackRepoDetails.getStack() != null) {
                    String repositoryVersion = stackRepoDetails.getStack().get(StackRepoDetails.REPOSITORY_VERSION);
                    if (!StringUtils.isEmpty(repositoryVersion)) {
                        stackJson.setHdpVersion(repositoryVersion);
                    } else {
                        stackJson.setHdpVersion(stackRepoDetails.getHdpVersion());
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to convert dynamic component.", e);
        }
    }

    private void addNodeCount(StackApiView source, StackViewResponse stackViewResponse) {
        int nodeCount = 0;
        for (InstanceGroupView instanceGroupView : source.getInstanceGroups()) {
            nodeCount += instanceGroupView.getNodeCount();
        }
        stackViewResponse.setNodeCount(nodeCount);
    }

    private void addFlexSubscription(StackApiView source, StackViewResponse stackJson) {
        if (source.getFlexSubscription() != null) {
            try {
                FlexSubscriptionResponse flexSubscription = getConversionService().convert(source.getFlexSubscription(), FlexSubscriptionResponse.class);
                stackJson.setFlexSubscription(flexSubscription);
            } catch (Exception ex) {
                LOGGER.warn("Flex subscription could not be added to stack response.", ex);
            }
        }
    }

    private void addUser(StackApiView source, StackViewResponse stackJson) {
        if (source.getUserView() != null) {
            try {
                UserViewResponse userViewResponse = getConversionService().convert(source.getUserView(), UserViewResponse.class);
                stackJson.setUser(userViewResponse);
            } catch (Exception ex) {
                LOGGER.warn("User could not be added to stack response.", ex);
            }
        }
    }
}
