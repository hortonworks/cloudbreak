package com.sequenceiq.cloudbreak.cmtemplate;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.ServiceDependencyMatrixService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.domain.Dependencies;
import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.domain.ServiceDependencyMatrix;
import com.sequenceiq.cloudbreak.cmtemplate.generator.template.GeneratedCmTemplateService;

@ExtendWith(MockitoExtension.class)
class ClusterTemplateGeneratorServiceTest {

    @Captor
    private ArgumentCaptor<Set<String>> captor;

    @Mock
    private ServiceDependencyMatrixService serviceDependencyMatrixService;

    @Mock
    private GeneratedCmTemplateService generatedCMTemplateService;

    @InjectMocks
    private CmTemplateGeneratorService underTest;

    @Test
    void testGenerateTemplateByServicesShouldCallGenerationWithTheSetOfSpecifiedAndDependentServices() {
        Set<String> services = Set.of("HDFS", "HIVE");
        ServiceDependencyMatrix dependentServices = new ServiceDependencyMatrix();
        Dependencies dependencies = new Dependencies();
        dependencies.setServices(Set.of("ZOOKEEPER"));
        dependentServices.setDependencies(dependencies);

        when(serviceDependencyMatrixService.collectServiceDependencyMatrix(services, "CDH", "6.1.1"))
                .thenReturn(dependentServices);

        underTest.generateTemplateByServices(services, "CDH-6.1.1");

        verify(generatedCMTemplateService).prepareClouderaManagerTemplate(captor.capture(), anyString(), anyString(), anyString());
        Set<String> expectedServicesArgument = Set.of("HDFS", "HIVE", "ZOOKEEPER");
        assertEquals(expectedServicesArgument, captor.getValue());
    }
}