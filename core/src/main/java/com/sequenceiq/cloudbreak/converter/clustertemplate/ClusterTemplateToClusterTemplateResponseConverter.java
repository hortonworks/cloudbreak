package com.sequenceiq.cloudbreak.converter.clustertemplate;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.service.stack.StackTemplateService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;

@Component
public class ClusterTemplateToClusterTemplateResponseConverter extends AbstractConversionServiceAwareConverter<ClusterTemplate, ClusterTemplateResponse> {

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private StackTemplateService stackTemplateService;

    @Override
    public ClusterTemplateResponse convert(ClusterTemplate source) {
        ClusterTemplateResponse clusterTemplateResponse = new ClusterTemplateResponse();
        clusterTemplateResponse.setName(source.getName());
        clusterTemplateResponse.setDescription(source.getDescription());
        Stack stack = stackTemplateService.getByIdWithLists(source.getStackTemplate().getId());
        clusterTemplateResponse.setStackTemplate(converterUtil.convert(stack, StackV2Request.class));
        clusterTemplateResponse.setCloudPlatform(source.getCloudPlatform());
        clusterTemplateResponse.setStatus(source.getStatus());
        clusterTemplateResponse.setId(source.getId());
        clusterTemplateResponse.setDatalakeRequired(source.getDatalakeRequired());
        clusterTemplateResponse.setStatus(source.getStatus());
        clusterTemplateResponse.setType(source.getType());
        return clusterTemplateResponse;
    }
}
