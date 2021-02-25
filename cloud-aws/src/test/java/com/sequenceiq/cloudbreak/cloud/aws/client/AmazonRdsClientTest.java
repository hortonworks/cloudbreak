package com.sequenceiq.cloudbreak.cloud.aws.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.Certificate;
import com.amazonaws.services.rds.model.DescribeCertificatesRequest;
import com.amazonaws.services.rds.model.DescribeCertificatesResult;

@ExtendWith(MockitoExtension.class)
class AmazonRdsClientTest {

    private static final String MARKER = "marker";

    @Mock
    private AmazonRDS client;

    @InjectMocks
    private AmazonRdsClient underTest;

    @Test
    void describeCertificatesTestSimple() {
        DescribeCertificatesRequest describeCertificatesRequest = mock(DescribeCertificatesRequest.class);

        DescribeCertificatesResult describeCertificatesResult = mock(DescribeCertificatesResult.class);
        Certificate cert1 = mock(Certificate.class);
        Certificate cert2 = mock(Certificate.class);
        when(describeCertificatesResult.getMarker()).thenReturn(null);
        when(describeCertificatesResult.getCertificates()).thenReturn(List.of(cert1, cert2));

        when(client.describeCertificates(describeCertificatesRequest)).thenReturn(describeCertificatesResult);

        List<Certificate> certificates = underTest.describeCertificates(describeCertificatesRequest);

        assertThat(certificates).isNotNull();
        assertThat(certificates).isEqualTo(List.of(cert1, cert2));
    }

    @Test
    void describeCertificatesTestWithPaging() {
        DescribeCertificatesRequest describeCertificatesRequest = mock(DescribeCertificatesRequest.class);

        DescribeCertificatesResult describeCertificatesResult1 = mock(DescribeCertificatesResult.class);
        Certificate cert1 = mock(Certificate.class);
        when(describeCertificatesResult1.getMarker()).thenReturn(MARKER);
        when(describeCertificatesResult1.getCertificates()).thenReturn(List.of(cert1));

        DescribeCertificatesResult describeCertificatesResult2 = mock(DescribeCertificatesResult.class);
        Certificate cert2 = mock(Certificate.class);
        when(describeCertificatesResult2.getMarker()).thenReturn(null);
        when(describeCertificatesResult2.getCertificates()).thenReturn(List.of(cert2));

        when(client.describeCertificates(describeCertificatesRequest)).thenReturn(describeCertificatesResult1, describeCertificatesResult2);

        List<Certificate> certificates = underTest.describeCertificates(describeCertificatesRequest);

        assertThat(certificates).isNotNull();
        assertThat(certificates).isEqualTo(List.of(cert1, cert2));
    }

}