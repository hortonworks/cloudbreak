package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.domain.ClusterTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ClusterTemplateToClusterTemplateResponseConverter extends AbstractConversionServiceAwareConverter<ClusterTemplate, ClusterTemplateResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateToClusterTemplateResponseConverter.class);

    @Override
    public ClusterTemplateResponse convert(ClusterTemplate json) {
        ClusterTemplateResponse clusterTemplateResponse = new ClusterTemplateResponse();
        clusterTemplateResponse.setName(json.getName());
        clusterTemplateResponse.setTemplate(json.getTemplate().getValue());
        clusterTemplateResponse.setType(json.getType());
        clusterTemplateResponse.setId(json.getId());
        return clusterTemplateResponse;
    }
}
