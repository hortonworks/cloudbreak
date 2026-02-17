package com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.generator.CentralTemplateGeneratorContext;
import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.domain.ServiceDependencyMatrix;

@ExtendWith(SpringExtension.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
class ServiceDependencyMatrixServiceTest extends CentralTemplateGeneratorContext {

    private static final String CDH = "CDH";

    private static final String CDH_6_1 = "6.1";

    @MockBean
    private ExposedServiceCollector exposedServiceCollector;

    @BeforeEach
    public void setUp() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    @MethodSource("data")
    @ParameterizedTest
    void test(Set<String> inputs, String stackType, String version, Set<String> dependencies) {
        ServiceDependencyMatrix serviceDependencyMatrix = serviceDependencyMatrixService()
                .collectServiceDependencyMatrix(inputs, stackType, version);
        assertEquals(dependencies, serviceDependencyMatrix.getDependencies().getServices());
        assertEquals(inputs, serviceDependencyMatrix.getServices().getServices());
    }

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(Set.of("OOZIE"), CDH, CDH_6_1, Set.of("HIVE", "HDFS", "ZOOKEEPER", "YARN", "SPARK_ON_YARN"))
        );
    }
}