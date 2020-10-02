package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.CertExpirationState.HOST_CERT_EXPIRING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.CertExpirationState.VALID;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CertExpirationState;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;

@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

    @Mock
    private ClusterRepository repository;

    @InjectMocks
    private ClusterService underTest;

    static Object[][] updateClusterCertExpirationStateScenarios() {
        return new Object[][]{
                {"Change from valid to expiring", VALID, Boolean.TRUE, Boolean.TRUE, HOST_CERT_EXPIRING},
                {"Change from expiring to valid", HOST_CERT_EXPIRING, Boolean.FALSE, Boolean.TRUE, VALID},
                {"No change when valid", VALID, Boolean.FALSE, Boolean.FALSE, null},
                {"No change when expiring", HOST_CERT_EXPIRING, Boolean.TRUE, Boolean.FALSE, null}
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("updateClusterCertExpirationStateScenarios")
    public void testUpdateClusterCertExpirationState(String name, CertExpirationState current, Boolean hostCertificateExpiring, Boolean stateChanged,
            CertExpirationState newState) {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setCertExpirationState(current);

        underTest.updateClusterCertExpirationState(cluster, hostCertificateExpiring);

        if (stateChanged) {
            verify(repository, times(1)).updateCertExpirationState(cluster.getId(), newState);
        } else {
            verifyNoInteractions(repository);
        }
    }
}