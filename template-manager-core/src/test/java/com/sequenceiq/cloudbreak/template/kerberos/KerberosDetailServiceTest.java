package com.sequenceiq.cloudbreak.template.kerberos;

import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.type.KerberosType;

class KerberosDetailServiceTest {

    private KerberosDetailService victim = new KerberosDetailService();

    @Test
    void shouldUpdateKeyTabsInCaseOfYarnChildEnvironment() {
        KerberosConfig kerberosConfig = mock(KerberosConfig.class);

        when(kerberosConfig.getType()).thenReturn(KerberosType.FREEIPA);

        assertTrue(victim.keytabsShouldBeUpdated(CloudPlatform.YARN.name(), true, of(kerberosConfig)));
    }

    @Test
    void shouldNotUpdateKeyTabsInCaseOfYarnNonChildEnvironment() {
        KerberosConfig kerberosConfig = mock(KerberosConfig.class);

        when(kerberosConfig.getType()).thenReturn(KerberosType.FREEIPA);

        assertFalse(victim.keytabsShouldBeUpdated(CloudPlatform.YARN.name(), false, of(mock(KerberosConfig.class))));
    }
}