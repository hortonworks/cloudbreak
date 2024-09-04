package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerSslConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.check.DatabaseCertificateRotationOutdatedDatahubsCollector;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateEntryResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.support.SupportV4Endpoint;

@ExtendWith(MockitoExtension.class)
class DatabaseCertificateRotationOutdatedDatahubsCollectorTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackService stackService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private SupportV4Endpoint supportV4Endpoint;

    @Mock
    private DatabaseService databaseService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private StackView datalake;

    private DatabaseCertificateRotationOutdatedDatahubsCollector underTest;

    @BeforeEach
    void setUp() {
        underTest = new DatabaseCertificateRotationOutdatedDatahubsCollector(
                stackDtoService,
                regionAwareInternalCrnGeneratorFactory,
                supportV4Endpoint,
                databaseService,
                stackService);
    }

    @Test
    void testRotateCertificateSDXWhenRelatedDatahubsHaveLatestAndOutdatedCerts() {
        String latestCert = "latestCert";
        String dlCrn = "dummyCrn";
        setupSDXAndDatahubFetch(dlCrn, latestCert, Set.of("embedded2", "external1"));

        List<String> response = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getDatahubNamesWithOutdatedCerts(datalake));

        assertNotNull(response);
        assertEquals(List.of("embedded2", "external1"), response);
    }

    @Test
    void testRotateCertificateSDXWhenRelatedDatahubsHaveLatestCerts() {
        String latestCert = "latestCert";
        String dlCrn = "dummyCrn";
        setupSDXAndDatahubFetch(dlCrn, latestCert, Set.of());

        List<String> response = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getDatahubNamesWithOutdatedCerts(datalake));

        assertNotNull(response);
        verify(stackService, times(2)).getByCrn(anyString());
        verify(databaseService, times(2)).getDatabaseServer(any(), any());
    }

    private void setupSDXAndDatahubFetch(String dlCrn, String latestCert, Set<String> stacksWithWrongCerts) {
        when(datalake.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        when(datalake.getEnvironmentCrn()).thenReturn("envCrn");
        when(datalake.getRegion()).thenReturn("region");

        RegionAwareInternalCrnGenerator mock = mock(RegionAwareInternalCrnGenerator.class);
        when(mock.getInternalCrnForServiceAsString()).thenReturn("internalCrn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(mock);
        SslCertificateEntryResponse certificateEntryResponse = mock(SslCertificateEntryResponse.class);
        when(certificateEntryResponse.getVersion()).thenReturn(3);
        when(certificateEntryResponse.getCertPem()).thenReturn(latestCert);
        when(supportV4Endpoint.getLatestCertificate(anyString(), anyString())).thenReturn(certificateEntryResponse);

        List<StackDto> stackV4Responses = new ArrayList<>();
        StackDto embWithLatestCert = createStackDtoMock("embedded1", "crn1", true, latestCert, stacksWithWrongCerts);
        StackDto embWithOutdatedCert = createStackDtoMock("embedded2", "crn2", true, latestCert, stacksWithWrongCerts);
        StackDto extWitOutdatedCert = createStackDtoMock("external1", "crn3", false, latestCert, stacksWithWrongCerts);
        StackDto extWithLatestCert = createStackDtoMock("external2", "crn4", false, latestCert, stacksWithWrongCerts);
        stackV4Responses.addAll(List.of(embWithLatestCert, embWithOutdatedCert, extWitOutdatedCert, extWithLatestCert));
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(any(), anyList())).thenReturn(stackV4Responses);
    }

    private StackDto createStackDtoMock(String name, String crn, boolean embedded, String latestCert, Set<String> stacksWithWrongCerts) {
        StackDto stackDtoMock = mock(StackDto.class, Answers.RETURNS_DEEP_STUBS);
        lenient().when(stackDtoMock.getName()).thenReturn(name);
        when(stackDtoMock.getResourceCrn()).thenReturn(crn);
        when(stackDtoMock.getExternalDatabaseCreationType().isEmbedded()).thenReturn(embedded);
        if (embedded) {
            Stack stack = mock(Stack.class);
            Cluster cluster = mock(Cluster.class);
            if (stacksWithWrongCerts.contains(name)) {
                when(cluster.getDbSslRootCertBundle()).thenReturn("wrong");
            } else {
                when(cluster.getDbSslRootCertBundle()).thenReturn(latestCert);
            }
            when(stack.getCluster()).thenReturn(cluster);
            when(stackService.getByCrn(eq(crn))).thenReturn(stack);
        } else {
            StackDatabaseServerResponse response = new StackDatabaseServerResponse();
            DatabaseServerSslConfig databaseServerSslConfig = new DatabaseServerSslConfig();
            if (stacksWithWrongCerts.contains(name)) {
                databaseServerSslConfig.setSslCertificateActiveVersion(99);
            } else {
                databaseServerSslConfig.setSslCertificateActiveVersion(3);
            }
            response.setSslConfig(databaseServerSslConfig);
            when(databaseService.getDatabaseServer(eq(NameOrCrn.ofCrn(crn)), any())).thenReturn(response);
        }
        return stackDtoMock;
    }

}