package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.service.stack.StackTemplateService;

@Component
public class ClusterTemplateToClusterTemplateV4ResponseConverter extends AbstractConversionServiceAwareConverter<ClusterTemplate, ClusterTemplateV4Response> {

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private StackTemplateService stackTemplateService;

    @Override
    public ClusterTemplateV4Response convert(ClusterTemplate source) {
        ClusterTemplateV4Response clusterTemplateV4Response = new ClusterTemplateV4Response();
        clusterTemplateV4Response.setName(source.getName());
        clusterTemplateV4Response.setDescription(source.getDescription());
        Stack stack = stackTemplateService.getByIdWithLists(source.getStackTemplate().getId()).orElse(null);
        clusterTemplateV4Response.setStackTemplate(converterUtil.convert(stack, StackV4Request.class));
        clusterTemplateV4Response.setCloudPlatform(source.getCloudPlatform());
        clusterTemplateV4Response.setStatus(source.getStatus());
        clusterTemplateV4Response.setId(source.getId());
        clusterTemplateV4Response.setDatalakeRequired(source.getDatalakeRequired());
        clusterTemplateV4Response.setStatus(source.getStatus());
        clusterTemplateV4Response.setType(source.getType());
        return clusterTemplateV4Response;
    }

}
