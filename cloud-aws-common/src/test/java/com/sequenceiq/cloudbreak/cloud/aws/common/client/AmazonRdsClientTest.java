package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.Certificate;
import com.amazonaws.services.rds.model.CreateDBParameterGroupRequest;
import com.amazonaws.services.rds.model.DescribeCertificatesRequest;
import com.amazonaws.services.rds.model.DescribeCertificatesResult;
import com.amazonaws.services.rds.model.ModifyDBParameterGroupRequest;
import com.amazonaws.services.rds.model.Parameter;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;

@ExtendWith(MockitoExtension.class)
class AmazonRdsClientTest {

    private static final String MARKER = "marker";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstanceIdentifier";

    private static final String DB_PARAMETER_GROUP_NAME = "dbParameterGroupName";

    private static final String DB_PARAMETER_GROUP_FAMILY = "dbParameterGroupFamily";

    private static final String DATABASE_SERVER_ID = "databaseServerId";

    @Mock
    private AmazonRDS client;

    @Spy
    private AwsPageCollector awsPageCollector;

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

    @Test
    void testCreateParameterGroup() {
        underTest.createParameterGroup(DB_PARAMETER_GROUP_FAMILY, DB_PARAMETER_GROUP_NAME, DATABASE_SERVER_ID);

        ArgumentCaptor<CreateDBParameterGroupRequest> requestArgumentCaptor = ArgumentCaptor.forClass(CreateDBParameterGroupRequest.class);
        verify(client).createDBParameterGroup(requestArgumentCaptor.capture());
        CreateDBParameterGroupRequest request = requestArgumentCaptor.getValue();
        assertEquals(DB_PARAMETER_GROUP_FAMILY, request.getDBParameterGroupFamily());
        assertEquals(DB_PARAMETER_GROUP_NAME, request.getDBParameterGroupName());
    }

    @Test
    void testChangeParameterGroupEntries() {
        Parameter parameter = new Parameter();

        underTest.changeParameterInGroup(DB_INSTANCE_IDENTIFIER, List.of(parameter));

        ArgumentCaptor<ModifyDBParameterGroupRequest> modifyRequestCaptor = ArgumentCaptor.forClass(ModifyDBParameterGroupRequest.class);
        verify(client).modifyDBParameterGroup(modifyRequestCaptor.capture());
        ModifyDBParameterGroupRequest request = modifyRequestCaptor.getValue();
        assertEquals(parameter, request.getParameters().get(0));
        assertEquals(DB_INSTANCE_IDENTIFIER, request.getDBParameterGroupName());
    }

}
