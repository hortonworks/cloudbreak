package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.rotation.secret.vault.SyncSecretVersionService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaRotationV1Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@ExtendWith(MockitoExtension.class)
public class OutdatedVaultSecretUpdaterPatchServiceTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:Test:datalake:40231209-6037-4d4b-95b9-f3c30698ae98";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private ClusterService clusterService;

    @Mock
    private SaltSecurityConfigService saltSecurityConfigService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private FreeIpaRotationV1Endpoint freeIpaRotationV1Endpoint;

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Mock
    private SyncSecretVersionService syncSecretVersionService;

    @InjectMocks
    private OutdatedVaultSecretUpdaterPatchService underTest;

    @Test
    void testUpdate() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        SecurityConfig securityConfig = new SecurityConfig();
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        RDSConfig rdsConfig = new RDSConfig();
        stack.setResourceCrn(DATALAKE_CRN);
        stack.setCluster(cluster);
        stack.setType(StackType.DATALAKE);
        stack.setSecurityConfig(securityConfig);
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        saltSecurityConfig.setId(1L);
        cluster.setId(1L);
        cluster.setDatabaseServerCrn("dbcrn");
        stack.setEnvironmentCrn("envCrn");
        when(clusterService.getCluster(any())).thenReturn(cluster);
        when(saltSecurityConfigService.getById(any())).thenReturn(Optional.of(saltSecurityConfig));
        when(rdsConfigService.findByClusterId(any())).thenReturn(Set.of(rdsConfig));
        doNothing().when(syncSecretVersionService).updateEntityIfNeeded(any(), any(), any());
        doNothing().when(freeIpaRotationV1Endpoint).syncOutdatedSecrets(any());
        doNothing().when(databaseServerV4Endpoint).syncOutdatedSecrets(any());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.doApply(stack);
            } catch (ExistingStackPatchApplyException e) {
                throw new RuntimeException(e);
            }
        });

        verify(databaseServerV4Endpoint).syncOutdatedSecrets(any());
        verify(freeIpaRotationV1Endpoint).syncOutdatedSecrets(any());
        verify(syncSecretVersionService, times(3)).updateEntityIfNeeded(any(), any(), any());
    }
}
