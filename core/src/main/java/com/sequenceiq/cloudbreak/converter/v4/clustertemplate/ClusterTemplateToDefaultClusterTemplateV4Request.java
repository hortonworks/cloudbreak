package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;

@Component
public class ClusterTemplateToDefaultClusterTemplateV4Request extends AbstractConversionServiceAwareConverter<ClusterTemplate, DefaultClusterTemplateV4Request> {

    @Inject
    private DistroXV1RequestToStackV4RequestConverter stackV4RequestConverter;

    @Override
    public DefaultClusterTemplateV4Request convert(ClusterTemplate source) {
        DefaultClusterTemplateV4Request ret = new DefaultClusterTemplateV4Request();
        ret.setCloudPlatform(source.getCloudPlatform());
        ret.setDatalakeRequired(source.getDatalakeRequired());
        ret.setDescription(source.getDescription());
        ret.setName(source.getName());
        ret.setType(source.getType());
        StackV4Request stackV4Request = getConversionService().convert(source.getStackTemplate(), StackV4Request.class);
        ret.setDistroXTemplate(stackV4RequestConverter.convert(stackV4Request));
        return ret;
    }
}
