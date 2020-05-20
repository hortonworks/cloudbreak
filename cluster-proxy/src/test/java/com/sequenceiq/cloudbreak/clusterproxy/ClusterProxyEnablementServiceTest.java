package com.sequenceiq.cloudbreak.clusterproxy;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(Parameterized.class)
public class ClusterProxyEnablementServiceTest {

    @Parameterized.Parameter
    public String cloudPlatform;

    @Parameterized.Parameter(1)
    public boolean clusterProxyIntegrationEnabled;

    @Parameterized.Parameter(2)
    public boolean clusterProxyApplicable;

    @Mock
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @InjectMocks
    private ClusterProxyEnablementService clusterProxyEnablementService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ReflectionTestUtils.setField(clusterProxyEnablementService, "clusterProxyDisabledPlatforms", Set.of("MOCK"));
    }

    @Test
    public void isClusterProxyApplicable() {
        when(clusterProxyConfiguration.isClusterProxyIntegrationEnabled()).thenReturn(clusterProxyIntegrationEnabled);

        Assert.assertEquals(clusterProxyApplicable, clusterProxyEnablementService.isClusterProxyApplicable(cloudPlatform));
    }

    @Parameterized.Parameters(name = "{index}: clusterProxyEnablementService.clusterProxyApplicable(get cloudPlatform '{0}' "
            + "with clusterProxyIntegrationEnabled '{1}') = output is clusterProxyApplicable '{2}'")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "MOCK", true, false },
                { "MOCK", false, false },
                { "AWS", true, true },
                { "AWS", false, false },
                { "AZURE", true, true },
                { "AZURE", false, false }
        });
    }

}