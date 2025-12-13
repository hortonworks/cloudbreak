package com.sequenceiq.cloudbreak.cmtemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;

@ExtendWith(MockitoExtension.class)
class CmTemplateServiceTest {

    private static final String SERVICE_TYPE = "NIFI";

    private static final String BLUEPRINT = "blueprint-text";

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @InjectMocks
    private CmTemplateService underTest;

    @Test
    void testIsServiceTypePresentShouldReturnTrueWhenTheServiceTypeIsPresentInTheBlueprint() {
        CmTemplateProcessor templateProcessor = createTemplateProcessor(Set.of(SERVICE_TYPE, "SPARK"));
        when(cmTemplateProcessorFactory.get(BLUEPRINT)).thenReturn(templateProcessor);
        assertTrue(underTest.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT));
    }

    @Test
    void testIsServiceTypePresentShouldReturnFalseWhenTheServiceTypeIsNotPresentInTheBlueprint() {
        CmTemplateProcessor templateProcessor = createTemplateProcessor(Set.of("ZOOKEEPER", "SPARK"));
        when(cmTemplateProcessorFactory.get(BLUEPRINT)).thenReturn(templateProcessor);
        assertFalse(underTest.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT));
    }

    private CmTemplateProcessor createTemplateProcessor(Set<String> serviceNames) {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        when(templateProcessor.getTemplate()).thenReturn(createApiClusterTemplate(serviceNames));
        return templateProcessor;
    }

    private ApiClusterTemplate createApiClusterTemplate(Set<String> serviceNames) {
        ApiClusterTemplate apiClusterTemplate = new ApiClusterTemplate();
        return apiClusterTemplate.services(createServices(serviceNames));
    }

    private List<ApiClusterTemplateService> createServices(Set<String> serviceNames) {
        return serviceNames.stream().map(serviceName -> {
            ApiClusterTemplateService service = new ApiClusterTemplateService();
            return service.serviceType(serviceName);
        }).collect(Collectors.toList());
    }

}