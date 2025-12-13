package com.sequenceiq.cloudbreak.cmtemplate;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.CustomConfigurationPropertyView;
import com.sequenceiq.cloudbreak.template.views.CustomConfigurationsView;

@ExtendWith(MockitoExtension.class)
class CustomConfigurationsInjectorProcessorTest {

    private static final String TEST_NAME = "test";

    private static final String TEST_CRN = "crn:cdp:resource:us-west-1:tenant:customconfigs:c7da2918-dd14-49ed-9b43-33ff55bd6309";

    private static final Set<CustomConfigurationPropertyView> TEST_CONFIGURATIONS = Set.of(
            new CustomConfigurationPropertyView("property1", "value1", "role1", "service1"),
            new CustomConfigurationPropertyView("property2", "value2", null, "service2"));

    private static final String TEST_RUNTIME  = "7.2.10";

    @Mock
    private CmTemplateProcessor processor;

    @Mock
    private TemplatePreparationObject templatePrepObj;

    @Mock
    private ApiClusterTemplate cmTemplate;

    @InjectMocks
    private CustomConfigurationsInjectorProcessor underTest;

    private final List<ApiClusterTemplateService> services = List.of(new ApiClusterTemplateService().serviceType("service1"),
            new ApiClusterTemplateService().serviceType("service2"), new ApiClusterTemplateService().serviceType("service3"));

    private CustomConfigurationsView customConfigurations = new CustomConfigurationsView(
            TEST_NAME,
            TEST_CRN,
            TEST_RUNTIME,
            TEST_CONFIGURATIONS);

    @Test
    void testProcessIfCorrectMethodsAreCalled() {
        when(templatePrepObj.getCustomConfigurationsView()).thenReturn(Optional.ofNullable(customConfigurations));
        when(processor.getTemplate()).thenReturn(cmTemplate);
        when(cmTemplate.getServices()).thenReturn(services);
        underTest.process(processor, templatePrepObj);
        verify(processor).getCustomServiceConfigsMap(customConfigurations.getConfigurations());
        verify(processor).getCustomRoleConfigsMap(customConfigurations.getConfigurations());
    }

    @Test
    void testProcessReturnsIfNoCustomConfigsAreProvided() {
        when(templatePrepObj.getCustomConfigurationsView()).thenReturn(Optional.empty());
        underTest.process(processor, templatePrepObj);
        verify(processor, times(0)).getCustomServiceConfigsMap(customConfigurations.getConfigurations());
        verify(processor, times(0)).getCustomRoleConfigsMap(customConfigurations.getConfigurations());
    }
}