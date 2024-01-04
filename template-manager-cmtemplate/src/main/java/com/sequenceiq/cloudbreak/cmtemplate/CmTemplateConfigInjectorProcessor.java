package com.sequenceiq.cloudbreak.cmtemplate;

import static java.util.Optional.ofNullable;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class CmTemplateConfigInjectorProcessor {

    @Inject
    private List<CmTemplateConfigInjector> injectors;

    public void process(CmTemplateProcessor processor, TemplatePreparationObject source) {
        List<ApiClusterTemplateService> services = ofNullable(processor.getTemplate().getServices()).orElse(List.of());
        for (CmTemplateConfigInjector injector : injectors) {
            for (ApiClusterTemplateService service : services) {
                injector.addServiceConfigs(service, processor, source);

                Iterable<ApiClusterTemplateRoleConfigGroup> roleConfigGroups = ofNullable(service.getRoleConfigGroups()).orElse(List.of());
                for (ApiClusterTemplateRoleConfigGroup roleConfigGroup : roleConfigGroups) {
                    injector.addRoleConfigs(roleConfigGroup, service, processor, source);
                }
            }
        }
    }

}
