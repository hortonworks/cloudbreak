package com.sequenceiq.cloudbreak.ambari;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import com.sequenceiq.ambari.client.services.ClusterService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterTemplateGeneratorTest {

    @InjectMocks
    private final AmbariClusterTemplateGenerator underTest = new AmbariClusterTemplateGenerator();

    @Mock
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Mock
    private ClusterService ambariClient;

    @Before
    public void init() {
        AmbariSecurityConfigProvider ambariSecurityConfigProvider = new AmbariSecurityConfigProvider();
        Whitebox.setInternalState(underTest, "ambariSecurityConfigProvider", ambariSecurityConfigProvider);
    }

    @Test
    public void testDefaultPasswordIsUserProvided() {
        // GIVEN
        Cluster cluster = TestUtil.cluster();
        cluster.setPassword("UserProvidedPassword");

        // WHEN
        underTest.generateClusterTemplate(cluster, new HashMap<>(), ambariClient);

        // THEN
        Mockito.verify(ambariClient, Mockito.times(1)).
                createClusterJson(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.eq("UserProvidedPassword"),
                        ArgumentMatchers.eq(ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES.name()), ArgumentMatchers.isNull(),
                        ArgumentMatchers.isNull(), ArgumentMatchers.isNull(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.isNull());
    }

    @Test
    public void testDefaultPasswordIsUserProvidedSecure() {
        // GIVEN
        Cluster cluster = TestUtil.cluster();
        cluster.setPassword("UserProvidedPassword");
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setPassword("KerberosPassword");
        kerberosConfig.setPrincipal("principal");
        cluster.setKerberosConfig(kerberosConfig);

        // WHEN
        underTest.generateClusterTemplate(cluster, new HashMap<>(), ambariClient);

        // THEN
        Mockito.verify(ambariClient, Mockito.times(1)).
                createClusterJson(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.eq("UserProvidedPassword"),
                        ArgumentMatchers.eq(ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES.name()),
                        ArgumentMatchers.eq("principal"), ArgumentMatchers.eq("KerberosPassword"), ArgumentMatchers.eq("PERSISTED"),
                        ArgumentMatchers.anyBoolean(), ArgumentMatchers.isNull());
    }

}
