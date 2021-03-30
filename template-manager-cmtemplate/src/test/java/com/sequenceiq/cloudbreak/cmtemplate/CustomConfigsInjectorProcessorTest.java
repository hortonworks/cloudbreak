package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.domain.CustomConfigProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;
import com.sequenceiq.cloudbreak.service.CustomConfigsService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@ExtendWith(MockitoExtension.class)
class CustomConfigsInjectorProcessorTest {

    @Mock
    private CustomConfigsService customConfigsService;

    @Mock
    private CmTemplateProcessor processor;

    @Mock
    private TemplatePreparationObject templatePrepObj;

    @Mock
    private ApiClusterTemplate cmTemplate;

    @InjectMocks
    private CustomConfigsInjectorProcessor underTest;

    private final List<ApiClusterTemplateService> services = List.of(new ApiClusterTemplateService().serviceType("service1"),
            new ApiClusterTemplateService().serviceType("service2"), new ApiClusterTemplateService().serviceType("service3"));

    private CustomConfigs customConfigs = new CustomConfigs(
            "test",
            "crn:cdp:resource:us-west-1:tenant:customconfigs:c7da2918-dd14-49ed-9b43-33ff55bd6309",
            Set.of(new CustomConfigProperty("property1", "value1", "role1", "service1"),
                    new CustomConfigProperty("property2", "value2", null, "service2")),
            "7.2.8",
            System.currentTimeMillis(),
            null);

    @Test
    void testProcessIfCorrectMethodsAreCalled() {
        Mockito.when(templatePrepObj.getCustomConfigs()).thenReturn(Optional.ofNullable(customConfigs));
        Mockito.when(processor.getTemplate()).thenReturn(cmTemplate);
        Mockito.when(cmTemplate.getServices()).thenReturn(services);
        underTest.process(processor, templatePrepObj);
        Mockito.verify(customConfigsService).getCustomServiceConfigsMap(customConfigs);
        Mockito.verify(customConfigsService).getCustomRoleConfigsMap(customConfigs);
    }

    @Test
    void testProcessReturnsIfNoCustomConfigsAreProvided() {
        Mockito.when(templatePrepObj.getCustomConfigs()).thenReturn(Optional.empty());
        underTest.process(processor, templatePrepObj);
        Mockito.verifyNoInteractions(processor, customConfigsService);
    }
}