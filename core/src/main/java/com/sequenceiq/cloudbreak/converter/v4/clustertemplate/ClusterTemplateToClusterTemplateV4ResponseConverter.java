package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.service.stack.StackTemplateService;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;

@Component
public class ClusterTemplateToClusterTemplateV4ResponseConverter extends AbstractConversionServiceAwareConverter<ClusterTemplate, ClusterTemplateV4Response> {

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private StackTemplateService stackTemplateService;

    @Inject
    private DistroXV1RequestToStackV4RequestConverter stackV4RequestConverter;

    @Override
    public ClusterTemplateV4Response convert(ClusterTemplate source) {
        ClusterTemplateV4Response clusterTemplateV4Response = new ClusterTemplateV4Response();
        clusterTemplateV4Response.setName(source.getName());
        clusterTemplateV4Response.setDescription(source.getDescription());
        Stack stack = stackTemplateService.getByIdWithLists(source.getStackTemplate().getId()).orElse(null);
        StackV4Request stackV4Request = converterUtil.convert(stack, StackV4Request.class);
        clusterTemplateV4Response.setDistroXTemplate(getIfNotNull(stackV4Request, stackV4RequestConverter::convert));
        clusterTemplateV4Response.setCloudPlatform(source.getCloudPlatform());
        clusterTemplateV4Response.setStatus(source.getStatus());
        clusterTemplateV4Response.setId(source.getId());
        clusterTemplateV4Response.setDatalakeRequired(source.getDatalakeRequired());
        clusterTemplateV4Response.setStatus(source.getStatus());
        clusterTemplateV4Response.setType(source.getType());
        clusterTemplateV4Response.setFeatureState(source.getFeatureState());
        if (source.getStackTemplate() != null) {
            Stack stackTemplate = source.getStackTemplate();
            if (stackTemplate.getEnvironmentCrn() != null) {
                clusterTemplateV4Response.setEnvironmentCrn(stackTemplate.getEnvironmentCrn());
            }
        }
        clusterTemplateV4Response.setCreated(source.getCreated());
        return clusterTemplateV4Response;
    }

}
