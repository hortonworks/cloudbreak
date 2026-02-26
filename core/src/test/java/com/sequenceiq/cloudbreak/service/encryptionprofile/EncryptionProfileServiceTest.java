package com.sequenceiq.cloudbreak.service.encryptionprofile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileServiceTest {
    @Mock
    private EncryptionProfileEndpoint encryptionProfileEndpoint;

    @InjectMocks
    private EncryptionProfileService underTest;

    @Test
    void testGetEncryptionProfileCrnWhenClusterEncryptionProfileIsNotNull() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileCrn("envEncryptionProfileCrn");
        ClusterView cluster = mock(ClusterView.class);

        when(cluster.getEncryptionProfileCrn()).thenReturn("clusterEpCrn");

        String response = underTest.getEncryptionProfileCrn(environment, cluster);

        assertEquals("clusterEpCrn", response);
    }

    @Test
    void testGetEncryptionProfileCrnWhenClusterEncryptionProfileIsNullEnvironmentEncryptionProfileShouldBeUsed() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileCrn("environmentEp");
        ClusterView cluster = mock(ClusterView.class);

        when(cluster.getEncryptionProfileCrn()).thenReturn(null);

        String response = underTest.getEncryptionProfileCrn(environment, cluster);

        assertEquals("environmentEp", response);
    }

    @Test
    void testGetEncryptionProfileByCrnWhenEncryptionProfileIsNullDefaultEncryptionProfileShouldBeUsed() {
        ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () -> underTest.getEncryptionProfileByCrnOrDefault(null));

        verify(encryptionProfileEndpoint, never()).getByCrn(anyString());
        verify(encryptionProfileEndpoint, only()).getDefaultEncryptionProfile();
    }

    @Test
    void testGetEncryptionProfileByCrnWhenEncryptionProfileIsNotNull() {
        ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () -> underTest.getEncryptionProfileByCrnOrDefault("clusterEpCrn"));

        verify(encryptionProfileEndpoint, only()).getByCrn(eq("clusterEpCrn"));
        verify(encryptionProfileEndpoint, never()).getDefaultEncryptionProfile();
    }
}
