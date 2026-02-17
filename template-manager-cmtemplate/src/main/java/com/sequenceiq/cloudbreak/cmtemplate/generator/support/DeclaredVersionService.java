package com.sequenceiq.cloudbreak.cmtemplate.generator.support;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;

@Service
public class DeclaredVersionService {

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    public SupportedServices collectDeclaredVersions(String blueprintText) {
        SupportedServices supportedServices = new SupportedServices();
        supportedServices.setServices(new HashSet<>());
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
        Collection<ExposedService> exposedServices = exposedServiceCollector
                .filterSupportedKnoxServices(Optional.ofNullable(cmTemplateProcessor.getTemplate().getCdhVersion()));

        for (ApiClusterTemplateService service : cmTemplateProcessor.getTemplate().getServices()) {
            Set<String> serviceNames = Optional.ofNullable(service.getRoleConfigGroups())
                    .stream()
                    .flatMap(Collection::stream)
                    .map(ApiClusterTemplateRoleConfigGroup::getRoleType)
                    .collect(Collectors.toSet());

            Optional<ExposedService> exposedService = exposedServices.stream()
                    .filter(e -> serviceNames.contains(e.getServiceName()))
                    .findFirst();

            if (exposedService.isPresent()) {
                SupportedService supportedService = new SupportedService();
                supportedService.setName(service.getServiceType());
                supportedService.setDisplayName(exposedService.get().getDisplayName());
                supportedService.setComponentNameInParcel(exposedService.get().getServiceName());
                supportedService.setIconKey(exposedService.get().getIconKey());
                supportedService.setVersion("N/A");
                supportedServices.getServices().add(supportedService);
            }
        }
        return supportedServices;
    }

}
