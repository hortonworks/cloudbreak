package com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies;


import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestContextManager;

import com.sequenceiq.cloudbreak.cmtemplate.generator.CentralTemplateGeneratorContext;
import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.domain.ServiceDependencyMatrix;

@RunWith(Parameterized.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
public class ServiceDependencyMatrixServiceTest extends CentralTemplateGeneratorContext {

    private static final String CDH = "CDH";

    private static final String CDH_6_1 = "6.1";

    @Parameter
    public Set<String> inputs;

    @Parameter(1)
    public String stackType;

    @Parameter(2)
    public String version;

    @Parameter(3)
    public Set<String> dependencies;

    @Before
    public void setUp() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    @Test
    public void test() {
        ServiceDependencyMatrix serviceDependencyMatrix = serviceDependencyMatrixService()
                .collectServiceDependencyMatrix(inputs, stackType, version);
        Assert.assertEquals(dependencies, serviceDependencyMatrix.getDependencies().getServices());
        Assert.assertEquals(inputs, serviceDependencyMatrix.getServices().getServices());
    }

    @Parameters(name = "{index}: testServicesAndDependencies(get {0} with {1} {2}) = dependencies are {3}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Set.of("OOZIE"), CDH, CDH_6_1, Set.of("HIVE", "HDFS", "ZOOKEEPER", "YARN", "SPARK_ON_YARN")}
        });
    }
}