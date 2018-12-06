package com.sequenceiq.cloudbreak.converter.stack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterViewResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;

@Component
public class StackApiViewToStackViewResponseConverter extends AbstractConversionServiceAwareConverter<StackApiView, StackViewResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackApiViewToStackViewResponseConverter.class);

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public StackViewResponse convert(StackApiView source) {
        StackViewResponse stackViewResponse = new StackViewResponse();
        stackViewResponse.setId(source.getId());
        stackViewResponse.setName(source.getName());
        stackViewResponse.setCredential(getConversionService().convert(source.getCredential(), CredentialViewResponse.class));
        if (source.getCluster() != null) {
            stackViewResponse.setCluster(conversionService.convert(source.getCluster(), ClusterViewResponse.class));
        }
        addNodeCount(source, stackViewResponse);
        stackViewResponse.setCloudPlatform(source.getCloudPlatform());
        stackViewResponse.setPlatformVariant(source.getPlatformVariant());
        stackViewResponse.setStatus(source.getStatus());
        stackViewResponse.setCreated(source.getCreated());
        return stackViewResponse;
    }

    private void addNodeCount(StackApiView source, StackViewResponse stackViewResponse) {
        int nodeCount = 0;
        for (InstanceGroupView instanceGroupView : source.getInstanceGroups()) {
            nodeCount += instanceGroupView.getNodeCount();
        }
        stackViewResponse.setNodeCount(nodeCount);
    }

}
