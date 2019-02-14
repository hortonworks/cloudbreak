package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import com.sequenceiq.ambari.client.services.ClusterService;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.clusterdefinition.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterTemplateGeneratorTest {

    @InjectMocks
    private final AmbariClusterTemplateGenerator underTest = new AmbariClusterTemplateGenerator();

    @Mock
    private KerberosDetailService kerberosDetailService;

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
    public void testDefaultPasswordIsUserProvided() throws CloudbreakException {
        // GIVEN
        Cluster cluster = TestUtil.cluster();
        cluster.setPassword("UserProvidedPassword");

        // WHEN
        underTest.generateClusterTemplate(cluster, new HashMap<>(), ambariClient);

        // THEN
        verify(ambariClient, times(1)).
                createClusterJson(anyString(), any(), eq("UserProvidedPassword"), eq(ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES.name()),
                        isNull(), isNull(), isNull(), anyBoolean(), isNull());
    }

    @Test
    public void testDefaultPasswordIsUserProvidedSecure() throws CloudbreakException {
        // GIVEN
        Cluster cluster = TestUtil.cluster();
        cluster.setPassword("UserProvidedPassword");
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setPassword("KerberosPassword");
        cluster.setKerberosConfig(kerberosConfig);
        given(kerberosDetailService.resolvePrincipalForKerberos(any())).willReturn("principal");

        // WHEN
        underTest.generateClusterTemplate(cluster, new HashMap<>(), ambariClient);

        // THEN
        verify(ambariClient, times(1)).
                createClusterJson(anyString(), any(), eq("UserProvidedPassword"), eq(ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES.name()),
                        eq("principal"), eq("KerberosPassword"), eq("PERSISTED"), anyBoolean(), isNull());
    }

}
