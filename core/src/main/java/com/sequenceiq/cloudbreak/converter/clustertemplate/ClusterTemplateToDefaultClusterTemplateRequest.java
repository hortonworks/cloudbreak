package com.sequenceiq.cloudbreak.converter.clustertemplate;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.template.DefaultClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;

@Component
public class ClusterTemplateToDefaultClusterTemplateRequest extends AbstractConversionServiceAwareConverter<ClusterTemplate, DefaultClusterTemplateRequest> {

    @Override
    public DefaultClusterTemplateRequest convert(ClusterTemplate source) {
        DefaultClusterTemplateRequest ret = new DefaultClusterTemplateRequest();
        ret.setCloudPlatform(source.getCloudPlatform());
        ret.setDatalakeRequired(source.getDatalakeRequired());
        ret.setDescription(source.getDescription());
        ret.setName(source.getName());
        ret.setType(source.getType());
        ret.setStackTemplate(getConversionService().convert(source.getStackTemplate(), StackV2Request.class));
        return ret;
    }
}
