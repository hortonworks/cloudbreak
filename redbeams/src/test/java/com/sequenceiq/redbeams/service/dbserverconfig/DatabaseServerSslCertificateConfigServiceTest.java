package com.sequenceiq.redbeams.service.dbserverconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.common.domain.SslCertStatus;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.TimeUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerCertificateStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;
import com.sequenceiq.redbeams.converter.v4.databaseserver.DatabaseServerConfigToDatabaseServerV4ResponseConverter;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DatabaseServerSslCertificateConfigServiceTest {

    @Mock
    private DatabaseServerConfigService databaseServerConfigService;

    @Mock
    private DatabaseServerConfigToDatabaseServerV4ResponseConverter databaseServerConfigToDatabaseServerV4ResponseConverter;

    @Mock
    private TimeUtil timeUtil;

    @InjectMocks
    private DatabaseServerSslCertificateConfigService service;

    @Test
    void testListDatabaseServersCertificateStatus() {
        String environmentCrn = "env-123";
        DatabaseServerCertificateStatusV4Request request = new DatabaseServerCertificateStatusV4Request();
        request.setEnvironmentCrns(Set.of(environmentCrn));

        DatabaseServerConfig databaseServerConfig = mock(DatabaseServerConfig.class);
        DBStack dbStack = mock(DBStack.class);
        when(dbStack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        when(databaseServerConfig.getDbStack()).thenReturn(Optional.of(dbStack));

        when(databaseServerConfigService.findAll(DatabaseServerSslCertificateConfigService.DEFAULT_WORKSPACE, environmentCrn))
                .thenReturn(Set.of(databaseServerConfig));

        DatabaseServerCertificateStatusV4Responses response = service.listDatabaseServersCertificateStatus(request);

        assertNotNull(response);
        assertEquals(1, response.getResponses().size());
    }

    @Test
    void testListDatabaseServersCertificateStatusWhenMultipleEnvProvided() {
        String environmentCrn1 = "env-123";
        String environmentCrn2 = "env-567";
        DatabaseServerCertificateStatusV4Request request = new DatabaseServerCertificateStatusV4Request();
        request.setEnvironmentCrns(Set.of(environmentCrn1, environmentCrn2));

        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());

        DatabaseServerConfig databaseServerConfig1 = new DatabaseServerConfig();
        databaseServerConfig1.setEnvironmentId(environmentCrn1);
        databaseServerConfig1.setId(1L);
        databaseServerConfig1.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response1 = new SslConfigV4Response();
        sslConfigV4Response1.setSslMode(SslMode.ENABLED);
        sslConfigV4Response1.setSslCertificateActiveVersion(1);
        sslConfigV4Response1.setSslCertificateHighestAvailableVersion(2);

        DatabaseServerConfig databaseServerConfig2 = new DatabaseServerConfig();
        databaseServerConfig2.setEnvironmentId(environmentCrn1);
        databaseServerConfig2.setId(2L);
        databaseServerConfig2.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response2 = new SslConfigV4Response();
        sslConfigV4Response2.setSslMode(SslMode.ENABLED);
        sslConfigV4Response2.setSslCertificateActiveVersion(1);
        sslConfigV4Response2.setSslCertificateHighestAvailableVersion(2);

        DatabaseServerConfig databaseServerConfig3 = new DatabaseServerConfig();
        databaseServerConfig3.setEnvironmentId(environmentCrn2);
        databaseServerConfig3.setId(3L);
        databaseServerConfig3.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response3 = new SslConfigV4Response();
        sslConfigV4Response3.setSslMode(SslMode.ENABLED);
        sslConfigV4Response3.setSslCertificateActiveVersion(1);
        sslConfigV4Response3.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response3.setSslCertificateExpirationDate(2);

        DatabaseServerConfig databaseServerConfig4 = new DatabaseServerConfig();
        databaseServerConfig4.setEnvironmentId(environmentCrn2);
        databaseServerConfig4.setId(4L);
        databaseServerConfig4.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response4 = new SslConfigV4Response();
        sslConfigV4Response4.setSslMode(SslMode.ENABLED);
        sslConfigV4Response4.setSslCertificateActiveVersion(1);
        sslConfigV4Response4.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response4.setSslCertificateExpirationDate(2);

        when(databaseServerConfigService.findAll(DatabaseServerSslCertificateConfigService.DEFAULT_WORKSPACE, Set.of(environmentCrn1, environmentCrn2)))
                .thenReturn(Set.of(databaseServerConfig1, databaseServerConfig2, databaseServerConfig3, databaseServerConfig4));
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig1))
                .thenReturn(sslConfigV4Response1);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig2))
                .thenReturn(sslConfigV4Response2);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig3))
                .thenReturn(sslConfigV4Response3);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig4))
                .thenReturn(sslConfigV4Response4);

        when(timeUtil.getTimestampThatDaysBeforeNow(90)).thenReturn(1L);

        DatabaseServerCertificateStatusV4Responses response = service.listDatabaseServersCertificateStatus(request);

        assertNotNull(response);
        assertEquals(2, response.getResponses().size());
        Optional<DatabaseServerCertificateStatusV4Response> env1 = response.getResponses()
                .stream()
                .filter(e -> e.getEnvironmentCrn().equals(environmentCrn1))
                .findFirst();
        assertEquals(SslCertStatus.OUTDATED, env1.get().getSslStatus());
        Optional<DatabaseServerCertificateStatusV4Response> env2 = response.getResponses()
                .stream()
                .filter(e -> e.getEnvironmentCrn().equals(environmentCrn2))
                .findFirst();
        assertEquals(SslCertStatus.OUTDATED, env2.get().getSslStatus());
    }
}