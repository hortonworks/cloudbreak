package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.HostGroupServicesV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@Component
public class StackToHostGroupServicesV4ResponseConverter {

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public HostGroupServicesV4Response convert(StackDto stack, String hostGroup) {
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(stack.getBlueprint().getBlueprintText());
        Set<String> servicesInHostGroup = processor.getServiceComponentsByHostGroup()
                .get(hostGroup).stream().map(ServiceComponent::getService).collect(Collectors.toSet());
        HostGroupServicesV4Response hostGroupServicesV4Response = new HostGroupServicesV4Response();
        hostGroupServicesV4Response.setServicesOnHostGroup(servicesInHostGroup);
        return hostGroupServicesV4Response;
    }
}
