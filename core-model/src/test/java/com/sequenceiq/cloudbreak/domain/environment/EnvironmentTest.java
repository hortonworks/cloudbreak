package com.sequenceiq.cloudbreak.domain.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

public class EnvironmentTest {

    private static final String WORKSPACE_NAME = "workspaceName";

    @Test
    public void testUnsetRelationsToEntitiesToBeDeleted() {
        Workspace workspace = new Workspace();
        workspace.setName(WORKSPACE_NAME);
        Credential credential = new Credential();
        Environment underTest = new Environment();
        underTest.setWorkspace(workspace);
        underTest.setCredential(credential);
        underTest.setKerberosConfigs(Set.of(new KerberosConfig()));
        underTest.setDatalakeResources(Set.of(new DatalakeResources()));
        underTest.setKubernetesConfigs(Set.of(new KubernetesConfig()));
        underTest.setProxyConfigs(Set.of(new ProxyConfig()));

        underTest.unsetRelationsToEntitiesToBeDeleted();

        assertNull(underTest.getKerberosConfigs());
        assertNull(underTest.getKubernetesConfigs());
        assertNull(underTest.getLdapConfigs());
        assertNull(underTest.getProxyConfigs());
        assertNull(underTest.getRdsConfigs());
        assertTrue(underTest.isArchived());
        assertEquals(credential, underTest.getCredential());
        assertEquals(workspace, underTest.getWorkspace());
    }
}
