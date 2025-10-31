package com.sequenceiq.cloudbreak.service.encryptionprofile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileServiceTest {
    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private EncryptionProfileService underTest;

    @Test
    void testWhenClusterEncryptionProfileIsNotNull() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileName("environmentEp");
        StackDto stackDto = mock(StackDto.class);
        ClusterView cluster = mock(ClusterView.class);

        when(stackDto.getCluster()).thenReturn(cluster);
        when(cluster.getEncryptionProfileName()).thenReturn("clusterEp");

        EncryptionProfileResponse response = underTest.getEncryptionProfileByNameOrDefault(environment, stackDto);

        verify(environmentService, times(1)).getEncryptionProfileByNameOrDefaultIfEmpty("clusterEp");
    }

    @Test
    void testWhenClusterEncryptionProfileIsNullEnvironmentEncryptionProfileShouldBeUsed() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileName("environmentEp");
        StackDto stackDto = mock(StackDto.class);
        ClusterView cluster = mock(ClusterView.class);

        when(stackDto.getCluster()).thenReturn(cluster);
        when(cluster.getEncryptionProfileName()).thenReturn(null);

        EncryptionProfileResponse response = underTest.getEncryptionProfileByNameOrDefault(environment, stackDto);

        verify(environmentService, times(1)).getEncryptionProfileByNameOrDefaultIfEmpty("environmentEp");
    }
}
