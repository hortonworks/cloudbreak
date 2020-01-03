package com.sequenceiq.cloudbreak.cmtemplate.generator.support;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.CmTemplateGeneratorConfigurationResolver;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.StackVersion;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceConfig;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.versionmatrix.CdhService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

@Service
public class DeclaredVersionService {

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private CmTemplateGeneratorConfigurationResolver cmTemplateGeneratorConfigurationResolver;

    public SupportedServices collectDeclaredVersions(String blueprintText) {
        SupportedServices supportedServices = new SupportedServices();

        Set<SupportedService> services = new HashSet<>();

        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);

        String cdhVersion = cmTemplateProcessor.getTemplate().getCdhVersion();

        StackVersion stackVersion = new StackVersion();
        stackVersion.setVersion(cdhVersion);
        stackVersion.setStackType("CDH");

        Set<CdhService> cdhServices = cmTemplateGeneratorConfigurationResolver.cdhConfigurations().get(stackVersion);

        if (cdhServices == null) {
            cdhServices = fallbackForDefault();
        }

        for (ApiClusterTemplateService service : cmTemplateProcessor.getTemplate().getServices()) {
            SupportedService supportedService = new SupportedService();
            supportedService.setName(service.getServiceType());

            for (CdhService cdhService : cdhServices) {
                if (cdhService.getName().equals(service.getServiceType())) {
                    supportedService.setVersion(cdhService.getVersion());
                }
            }

            for (ServiceConfig serviceConfig : cmTemplateGeneratorConfigurationResolver.serviceConfigs()) {
                if (serviceConfig.getName().equals(service.getServiceType())) {
                    supportedService.setDisplayName(serviceConfig.getDisplayName());
                }
            }

            if (!Strings.isNullOrEmpty(supportedService.getDisplayName())
                && !Strings.isNullOrEmpty(supportedService.getVersion())) {
                services.add(supportedService);
            }
        }

        supportedServices.setServices(services);
        return supportedServices;
    }

    public Set<CdhService> fallbackForDefault() {
        StackVersion stackVersion = new StackVersion();
        stackVersion.setVersion("default");
        stackVersion.setStackType("CDH");
        return cmTemplateGeneratorConfigurationResolver.cdhConfigurations().get(stackVersion);
    }

}
