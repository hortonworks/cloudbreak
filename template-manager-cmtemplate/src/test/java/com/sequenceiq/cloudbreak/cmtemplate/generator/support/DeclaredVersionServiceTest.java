package com.sequenceiq.cloudbreak.cmtemplate.generator.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;

@ExtendWith(MockitoExtension.class)
class DeclaredVersionServiceTest {

    @InjectMocks
    private DeclaredVersionService underTest;

    @Mock
    private ExposedServiceCollector exposedServiceCollector;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Test
    void collectDeclaredVersions() {
        String blueprintText = "blueprintText";
        ApiClusterTemplate apiClusterTemplate = new ApiClusterTemplate();
        ApiClusterTemplateService service = new ApiClusterTemplateService();
        service.setServiceType("HUE");
        ApiClusterTemplateRoleConfigGroup roleConfigGroup = new ApiClusterTemplateRoleConfigGroup();
        roleConfigGroup.setRoleType("HUE_SERVER");
        service.setRoleConfigGroups(List.of(roleConfigGroup));
        apiClusterTemplate.setServices(List.of(service));

        ExposedService exposedService = ExposedService.builder()
                .withName("HUE_SERVER")
                .withDisplayName("Hue")
                .withServiceName("HUE_SERVER")
                .withKnoxService("HUE_SERVER")
                .withKnoxUrl("/hue")
                .withIconKey("hue")
                .withSsoSupported(true)
                .withPort(80)
                .withTlsPort(80)
                .withApiOnly(false)
                .withCmProxied(false)
                .withApiIncluded(false)
                .withVisibleForDatalake(true)
                .withVisibleForDatahub(true)
                .withEntitlement("")
                .withMinVersion("")
                .withMaxVersion("")
                .withWithoutProxyPath(false)
                .withMinHttpsVersion("")
                .withRoleTypes(Set.of())
                .build();

        when(cmTemplateProcessorFactory.get(blueprintText)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getTemplate()).thenReturn(apiClusterTemplate);
        when(exposedServiceCollector.getExposedServices()).thenReturn(List.of(exposedService));

        SupportedServices supportedServices = underTest.collectDeclaredVersions(blueprintText);

        assertEquals(1, supportedServices.getServices().size());
        SupportedService supportedService = supportedServices.getServices().iterator().next();
        assertEquals("HUE", supportedService.getName());
        assertEquals("Hue", supportedService.getDisplayName());
        assertEquals("HUE_SERVER", supportedService.getComponentNameInParcel());
    }

    @Test
    void collectDeclaredVersionsWithRoleTypes() {
        String blueprintText = "blueprintText";
        ApiClusterTemplate apiClusterTemplate = new ApiClusterTemplate();
        ApiClusterTemplateService service = new ApiClusterTemplateService();
        service.setServiceType("IMPALA");
        ApiClusterTemplateRoleConfigGroup roleConfigGroup = new ApiClusterTemplateRoleConfigGroup();
        roleConfigGroup.setRoleType("IMPALAD");
        service.setRoleConfigGroups(List.of(roleConfigGroup));
        apiClusterTemplate.setServices(List.of(service));

        ExposedService exposedService = ExposedService.builder()
                .withName("IMPALA")
                .withDisplayName("Impala")
                .withServiceName("IMPALA")
                .withKnoxService("IMPALA")
                .withKnoxUrl("/impala")
                .withIconKey("impala")
                .withSsoSupported(true)
                .withPort(80)
                .withTlsPort(80)
                .withApiOnly(false)
                .withCmProxied(false)
                .withApiIncluded(false)
                .withVisibleForDatalake(true)
                .withVisibleForDatahub(true)
                .withEntitlement("")
                .withMinVersion("")
                .withMaxVersion("")
                .withWithoutProxyPath(false)
                .withMinHttpsVersion("")
                .withRoleTypes(Set.of("IMPALAD"))
                .build();

        when(cmTemplateProcessorFactory.get(blueprintText)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getTemplate()).thenReturn(apiClusterTemplate);
        when(exposedServiceCollector.getExposedServices()).thenReturn(List.of(exposedService));

        SupportedServices supportedServices = underTest.collectDeclaredVersions(blueprintText);

        assertEquals(1, supportedServices.getServices().size());
        SupportedService supportedService = supportedServices.getServices().iterator().next();
        assertEquals("IMPALA", supportedService.getName());
        assertEquals("Impala", supportedService.getDisplayName());
        assertEquals("IMPALA", supportedService.getComponentNameInParcel());
    }
}
