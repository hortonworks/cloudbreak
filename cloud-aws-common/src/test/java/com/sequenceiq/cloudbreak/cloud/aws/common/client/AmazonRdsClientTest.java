package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.Certificate;
import software.amazon.awssdk.services.rds.model.CreateDbParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.DbParameterGroupNotFoundException;
import software.amazon.awssdk.services.rds.model.DeleteDbParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.DescribeCertificatesRequest;
import software.amazon.awssdk.services.rds.model.DescribeCertificatesResponse;
import software.amazon.awssdk.services.rds.model.DescribeDbParameterGroupsRequest;
import software.amazon.awssdk.services.rds.model.InvalidDbParameterGroupStateException;
import software.amazon.awssdk.services.rds.model.ModifyDbParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.Parameter;

@ExtendWith(MockitoExtension.class)
class AmazonRdsClientTest {

    private static final String MARKER = "marker";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstanceIdentifier";

    private static final String DB_PARAMETER_GROUP_NAME = "dbParameterGroupName";

    private static final String DB_PARAMETER_GROUP_FAMILY = "dbParameterGroupFamily";

    private static final String DATABASE_SERVER_ID = "databaseServerId";

    @Mock
    private RdsClient client;

    @Spy
    private AwsPageCollector awsPageCollector;

    @InjectMocks
    private AmazonRdsClient underTest;

    @Test
    void describeCertificatesTestSimple() {
        DescribeCertificatesRequest describeCertificatesRequest = DescribeCertificatesRequest.builder().build();

        Certificate cert1 = Certificate.builder().build();
        Certificate cert2 = Certificate.builder().build();
        DescribeCertificatesResponse describeCertificatesResult = DescribeCertificatesResponse.builder()
                .marker(null)
                .certificates(cert1, cert2)
                .build();

        when(client.describeCertificates(describeCertificatesRequest)).thenReturn(describeCertificatesResult);

        List<Certificate> certificates = underTest.describeCertificates(describeCertificatesRequest);

        assertThat(certificates).isNotNull();
        assertThat(certificates).isEqualTo(List.of(cert1, cert2));
    }

    @Test
    void describeCertificatesTestWithPaging() {
        DescribeCertificatesRequest describeCertificatesRequest = DescribeCertificatesRequest.builder().marker(MARKER).build();

        Certificate cert1 = Certificate.builder().build();
        DescribeCertificatesResponse describeCertificatesResult1 = DescribeCertificatesResponse.builder()
                .marker(MARKER)
                .certificates(cert1)
                .build();

        Certificate cert2 = Certificate.builder().build();
        DescribeCertificatesResponse describeCertificatesResult2 = DescribeCertificatesResponse.builder()
                .marker(null)
                .certificates(cert2)
                .build();

        when(client.describeCertificates(describeCertificatesRequest)).thenReturn(describeCertificatesResult1, describeCertificatesResult2);

        List<Certificate> certificates = underTest.describeCertificates(describeCertificatesRequest);

        assertThat(certificates).isNotNull();
        assertThat(certificates).isEqualTo(List.of(cert1, cert2));
    }

    @Test
    void testCreateParameterGroup() {
        underTest.createParameterGroup(DB_PARAMETER_GROUP_FAMILY, DB_PARAMETER_GROUP_NAME, DATABASE_SERVER_ID);

        ArgumentCaptor<CreateDbParameterGroupRequest> requestArgumentCaptor = ArgumentCaptor.forClass(CreateDbParameterGroupRequest.class);
        verify(client).createDBParameterGroup(requestArgumentCaptor.capture());
        CreateDbParameterGroupRequest request = requestArgumentCaptor.getValue();
        assertEquals(DB_PARAMETER_GROUP_FAMILY, request.dbParameterGroupFamily());
        assertEquals(DB_PARAMETER_GROUP_NAME, request.dbParameterGroupName());
    }

    @Test
    void testChangeParameterGroupEntries() {
        Parameter parameter = Parameter.builder().build();

        underTest.changeParameterInGroup(DB_INSTANCE_IDENTIFIER, List.of(parameter));

        ArgumentCaptor<ModifyDbParameterGroupRequest> modifyRequestCaptor = ArgumentCaptor.forClass(ModifyDbParameterGroupRequest.class);
        verify(client).modifyDBParameterGroup(modifyRequestCaptor.capture());
        ModifyDbParameterGroupRequest request = modifyRequestCaptor.getValue();
        assertEquals(parameter, request.parameters().get(0));
        assertEquals(DB_INSTANCE_IDENTIFIER, request.dbParameterGroupName());
    }

    @Test
    void testDeleteParameterGroup() {
        underTest.deleteParameterGroup("paramGroup");
        ArgumentCaptor<DeleteDbParameterGroupRequest> requestCaptor = ArgumentCaptor.forClass(DeleteDbParameterGroupRequest.class);
        verify(client).deleteDBParameterGroup(requestCaptor.capture());
        assertEquals("paramGroup", requestCaptor.getValue().dbParameterGroupName());
    }

    @Test
    void testDeleteParameterGroupIfNotExistShouldNotTriggerDelete() {
        doThrow(DbParameterGroupNotFoundException.builder().build()).when(client).describeDBParameterGroups(any(DescribeDbParameterGroupsRequest.class));
        underTest.deleteParameterGroup("paramGroup");
        verify(client, never()).deleteDBParameterGroup(any(DeleteDbParameterGroupRequest.class));
    }

    @Test
    void testDeleteParameterGroupWhenNotFound() {
        when(client.deleteDBParameterGroup(any(DeleteDbParameterGroupRequest.class)))
                .thenThrow(DbParameterGroupNotFoundException.builder().message("errorMsg").build());
        underTest.deleteParameterGroup("paramGroup");
    }

    @Test
    void testDeleteParameterGroupWhenInvalidDBParameterGroupState() {
        InvalidDbParameterGroupStateException invalidGroupStateException = InvalidDbParameterGroupStateException.builder().message("errorMsg").build();
        when(client.deleteDBParameterGroup(any(DeleteDbParameterGroupRequest.class))).thenThrow(invalidGroupStateException);
        CloudConnectorException actualException = assertThrows(CloudConnectorException.class, () -> underTest.deleteParameterGroup("paramGroup"));
        assertEquals(invalidGroupStateException, actualException.getCause());
    }
}
