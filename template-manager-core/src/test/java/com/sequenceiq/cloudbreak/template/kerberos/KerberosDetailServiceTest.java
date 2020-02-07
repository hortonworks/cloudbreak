package com.sequenceiq.cloudbreak.template.kerberos;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.type.KerberosType;
import org.junit.Test;

import static java.util.Optional.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KerberosDetailServiceTest {

    private KerberosDetailService victim = new KerberosDetailService();

    @Test
    public void shouldUpdateKeyTabsInCaseOfYarnChildEnvironment() {
        KerberosConfig kerberosConfig = mock(KerberosConfig.class);

        when(kerberosConfig.getType()).thenReturn(KerberosType.FREEIPA);

        assertTrue(victim.keytabsShouldBeUpdated(CloudPlatform.YARN.name(), true, of(kerberosConfig)));
    }

    @Test
    public void shouldNotUpdateKeyTabsInCaseOfYarnNonChildEnvironment() {
        KerberosConfig kerberosConfig = mock(KerberosConfig.class);

        when(kerberosConfig.getType()).thenReturn(KerberosType.FREEIPA);

        assertFalse(victim.keytabsShouldBeUpdated(CloudPlatform.YARN.name(), false, of(mock(KerberosConfig.class))));
    }
}