package com.sequenceiq.cloudbreak.converter.v4.cluster_template;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;

@Component
public class ClusterTemplateToDefaultClusterTemplateV4Request extends AbstractConversionServiceAwareConverter<ClusterTemplate, DefaultClusterTemplateV4Request> {

    @Override
    public DefaultClusterTemplateV4Request convert(ClusterTemplate source) {
        DefaultClusterTemplateV4Request ret = new DefaultClusterTemplateV4Request();
        ret.setCloudPlatform(source.getCloudPlatform());
        ret.setDatalakeRequired(source.getDatalakeRequired());
        ret.setDescription(source.getDescription());
        ret.setName(source.getName());
        ret.setType(source.getType());
        ret.setStackTemplate(getConversionService().convert(source.getStackTemplate(), StackV2Request.class));
        return ret;
    }
}
