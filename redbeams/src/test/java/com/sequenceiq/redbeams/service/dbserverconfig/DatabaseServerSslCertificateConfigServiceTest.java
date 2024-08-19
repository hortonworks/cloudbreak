package com.sequenceiq.redbeams.service.dbserverconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.ClusterDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ClusterDatabaseServerCertificateStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ClusterDatabaseServerCertificateStatusV4Responses;
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

        DatabaseServerCertificateStatusV4Responses response = service.listDatabaseServersCertificateStatus(request, "accountId");

        assertNotNull(response);
        assertEquals(1, response.getResponses().size());
    }

    @Test
    void testListDatabaseServersCertificateStatusWhenMultipleEnvProvided() {
        String environmentCrn1 = "env-1";
        String environmentCrn2 = "env-2";
        String environmentCrn3 = "env-3";
        String environmentCrn4 = "env-4";
        String environmentCrn5 = "env-5";
        String environmentCrn6 = "env-6";
        String environmentCrn7 = "env-7";
        String environmentCrn8 = "env-8";
        String environmentCrn9 = "env-9";
        long time = Instant.now().getEpochSecond() * 1000 + Duration.ofDays(30).toMillis();
        long notExpired = Instant.now().getEpochSecond() * 1000 + Duration.ofDays(60).toMillis();
        long expireIn20Days = Instant.now().getEpochSecond() * 1000 + Duration.ofDays(20).toMillis();

        DatabaseServerCertificateStatusV4Request request = new DatabaseServerCertificateStatusV4Request();
        request.setEnvironmentCrns(Set.of(
                environmentCrn1,
                environmentCrn2,
                environmentCrn3,
                environmentCrn4,
                environmentCrn5,
                environmentCrn6,
                environmentCrn7,
                environmentCrn8,
                environmentCrn9));

        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());

        DatabaseServerConfig databaseServerConfig1 = new DatabaseServerConfig();
        databaseServerConfig1.setEnvironmentId(environmentCrn1);
        databaseServerConfig1.setId(1L);
        databaseServerConfig1.setClusterCrn("crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f431");
        databaseServerConfig1.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response1 = new SslConfigV4Response();
        sslConfigV4Response1.setSslMode(SslMode.ENABLED);
        sslConfigV4Response1.setSslCertificateActiveVersion(1);
        sslConfigV4Response1.setSslCertificateHighestAvailableVersion(2);

        DatabaseServerConfig databaseServerConfig2 = new DatabaseServerConfig();
        databaseServerConfig2.setEnvironmentId(environmentCrn2);
        databaseServerConfig2.setId(2L);
        databaseServerConfig2.setClusterCrn("crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f432");
        databaseServerConfig2.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response2 = new SslConfigV4Response();
        sslConfigV4Response2.setSslMode(SslMode.DISABLED);
        sslConfigV4Response2.setSslCertificateActiveVersion(1);
        sslConfigV4Response2.setSslCertificateHighestAvailableVersion(2);

        DatabaseServerConfig databaseServerConfig3 = new DatabaseServerConfig();
        databaseServerConfig3.setEnvironmentId(environmentCrn3);
        databaseServerConfig3.setId(3L);
        databaseServerConfig3.setClusterCrn("crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f433");
        databaseServerConfig3.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response3 = new SslConfigV4Response();
        sslConfigV4Response3.setSslMode(SslMode.ENABLED);
        sslConfigV4Response3.setSslCertificateActiveVersion(1);
        sslConfigV4Response3.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response3.setSslCertificateExpirationDate(notExpired);

        DatabaseServerConfig databaseServerConfig4 = new DatabaseServerConfig();
        databaseServerConfig4.setEnvironmentId(environmentCrn4);
        databaseServerConfig4.setId(4L);
        databaseServerConfig4.setClusterCrn("crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f434");
        databaseServerConfig4.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response4 = new SslConfigV4Response();
        sslConfigV4Response4.setSslMode(SslMode.DISABLED);
        sslConfigV4Response4.setSslCertificateActiveVersion(1);
        sslConfigV4Response4.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response4.setSslCertificateExpirationDate(notExpired);

        DatabaseServerConfig databaseServerConfig5 = new DatabaseServerConfig();
        databaseServerConfig5.setEnvironmentId(environmentCrn5);
        databaseServerConfig5.setId(5L);
        databaseServerConfig5.setClusterCrn("crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f435");
        databaseServerConfig5.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response5 = new SslConfigV4Response();
        sslConfigV4Response5.setSslMode(SslMode.DISABLED);
        sslConfigV4Response5.setSslCertificateActiveVersion(1);
        sslConfigV4Response5.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response5.setSslCertificateExpirationDate(LocalDateTime
                .of(2000, 1, 1, 0, 0)
                .toEpochSecond(ZoneOffset.UTC));

        DatabaseServerConfig databaseServerConfig6 = new DatabaseServerConfig();
        databaseServerConfig6.setEnvironmentId(environmentCrn6);
        databaseServerConfig6.setId(6L);
        databaseServerConfig6.setClusterCrn("crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f436");
        databaseServerConfig6.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response6 = new SslConfigV4Response();
        sslConfigV4Response6.setSslMode(SslMode.ENABLED);
        sslConfigV4Response6.setSslCertificateActiveVersion(1);
        sslConfigV4Response6.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response6.setSslCertificateExpirationDate(LocalDateTime
                .of(2000, 1, 1, 0, 0)
                .toEpochSecond(ZoneOffset.UTC));

        DatabaseServerConfig databaseServerConfig7 = new DatabaseServerConfig();
        databaseServerConfig7.setEnvironmentId(environmentCrn7);
        databaseServerConfig7.setId(7L);
        databaseServerConfig7.setClusterCrn("crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f437");
        databaseServerConfig7.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response7 = new SslConfigV4Response();
        sslConfigV4Response7.setSslMode(SslMode.ENABLED);
        sslConfigV4Response7.setSslCertificateActiveVersion(1);
        sslConfigV4Response7.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response7.setSslCertificateExpirationDate(expireIn20Days);

        DatabaseServerConfig databaseServerConfig8 = new DatabaseServerConfig();
        databaseServerConfig8.setEnvironmentId(environmentCrn8);
        databaseServerConfig8.setId(8L);
        databaseServerConfig8.setClusterCrn("crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f438");
        databaseServerConfig8.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response8 = new SslConfigV4Response();
        sslConfigV4Response8.setSslMode(SslMode.ENABLED);
        sslConfigV4Response8.setSslCertificateActiveVersion(1);
        sslConfigV4Response8.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response8.setSslCertificateExpirationDate(time);

        DatabaseServerConfig databaseServerConfig9 = new DatabaseServerConfig();
        databaseServerConfig9.setEnvironmentId(environmentCrn8);
        databaseServerConfig9.setId(9L);
        databaseServerConfig9.setClusterCrn("crn:cdp:df:us-west-1:hortonworks:service:3faa9368-6635-4744-b4a8-22501302f439");
        databaseServerConfig9.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response9 = new SslConfigV4Response();
        sslConfigV4Response9.setSslMode(SslMode.ENABLED);
        sslConfigV4Response9.setSslCertificateActiveVersion(1);
        sslConfigV4Response9.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response9.setSslCertificateExpirationDate(time);

        when(databaseServerConfigService.findAllByEnvironmentCrns("accountId",
                Set.of(
                        environmentCrn1,
                        environmentCrn2,
                        environmentCrn3,
                        environmentCrn4,
                        environmentCrn5,
                        environmentCrn6,
                        environmentCrn7,
                        environmentCrn8,
                        environmentCrn9)))
                .thenReturn(Set.of(
                        databaseServerConfig1,
                        databaseServerConfig2,
                        databaseServerConfig3,
                        databaseServerConfig4,
                        databaseServerConfig5,
                        databaseServerConfig6,
                        databaseServerConfig7,
                        databaseServerConfig8,
                        databaseServerConfig9));
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig1))
                .thenReturn(sslConfigV4Response1);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig2))
                .thenReturn(sslConfigV4Response2);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig3))
                .thenReturn(sslConfigV4Response3);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig4))
                .thenReturn(sslConfigV4Response4);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig5))
                .thenReturn(sslConfigV4Response5);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig6))
                .thenReturn(sslConfigV4Response6);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig7))
                .thenReturn(sslConfigV4Response7);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig8))
                .thenReturn(sslConfigV4Response8);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig9))
                .thenReturn(sslConfigV4Response9);

        when(timeUtil.getTimestampThatDaysAfterNow(anyInt())).thenReturn(time);

        DatabaseServerCertificateStatusV4Responses response = service.listDatabaseServersCertificateStatus(request, "accountId");

        assertNotNull(response);
        assertEquals(9, response.getResponses().size());
        Optional<DatabaseServerCertificateStatusV4Response> env1 = response.getResponses()
                .stream()
                .filter(e -> e.getEnvironmentCrn().equals(environmentCrn1))
                .findFirst();
        assertEquals(SslCertStatus.OUTDATED, env1.get().getSslStatus());
        Optional<DatabaseServerCertificateStatusV4Response> env2 = response.getResponses()
                .stream()
                .filter(e -> e.getEnvironmentCrn().equals(environmentCrn2))
                .findFirst();
        assertEquals(SslCertStatus.UP_TO_DATE, env2.get().getSslStatus());
        Optional<DatabaseServerCertificateStatusV4Response> env3 = response.getResponses()
                .stream()
                .filter(e -> e.getEnvironmentCrn().equals(environmentCrn3))
                .findFirst();
        assertEquals(SslCertStatus.UP_TO_DATE, env3.get().getSslStatus());
        Optional<DatabaseServerCertificateStatusV4Response> env4 = response.getResponses()
                .stream()
                .filter(e -> e.getEnvironmentCrn().equals(environmentCrn4))
                .findFirst();
        assertEquals(SslCertStatus.UP_TO_DATE, env4.get().getSslStatus());
        Optional<DatabaseServerCertificateStatusV4Response> env5 = response.getResponses()
                .stream()
                .filter(e -> e.getEnvironmentCrn().equals(environmentCrn5))
                .findFirst();
        assertEquals(SslCertStatus.UP_TO_DATE, env5.get().getSslStatus());
        Optional<DatabaseServerCertificateStatusV4Response> env6 = response.getResponses()
                .stream()
                .filter(e -> e.getEnvironmentCrn().equals(environmentCrn6))
                .findFirst();
        assertEquals(SslCertStatus.OUTDATED, env6.get().getSslStatus());
        Optional<DatabaseServerCertificateStatusV4Response> env7 = response.getResponses()
                .stream()
                .filter(e -> e.getEnvironmentCrn().equals(environmentCrn7))
                .findFirst();
        assertEquals(SslCertStatus.OUTDATED, env7.get().getSslStatus());
        Optional<DatabaseServerCertificateStatusV4Response> env8 = response.getResponses()
                .stream()
                .filter(e -> e.getEnvironmentCrn().equals(environmentCrn8))
                .findFirst();
        assertEquals(SslCertStatus.OUTDATED, env8.get().getSslStatus());
        Optional<DatabaseServerCertificateStatusV4Response> env9 = response.getResponses()
                .stream()
                .filter(e -> e.getEnvironmentCrn().equals(environmentCrn9))
                .findFirst();
        assertEquals(SslCertStatus.UP_TO_DATE, env9.get().getSslStatus());
    }

    @Test
    void testListDatabaseServersCertificateStatusWhenMultipleClusterProvided() {
        String crn1 = "crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f431";
        String crn2 = "crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f432";
        String crn3 = "crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f433";
        String crn4 = "crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f434";
        String crn5 = "crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f435";
        String crn6 = "crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f436";
        String crn7 = "crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f437";
        String crn8 = "crn:cdp:datalake:us-west-1:hortonworks:datalake:3faa9368-6635-4744-b4a8-22501302f438";
        String crn9 = "crn:cdp:df:us-west-1:hortonworks:service:3faa9368-6635-4744-b4a8-22501302f439";
        long time = Instant.now().getEpochSecond() * 1000 + Duration.ofDays(30).toMillis();
        long notExpired = Instant.now().getEpochSecond() * 1000 + Duration.ofDays(60).toMillis();
        long expireIn20Days = Instant.now().getEpochSecond() * 1000 + Duration.ofDays(20).toMillis();

        ClusterDatabaseServerCertificateStatusV4Request request = new ClusterDatabaseServerCertificateStatusV4Request();
        request.setCrns(Set.of(
                crn1,
                crn2,
                crn3,
                crn4,
                crn5,
                crn6,
                crn7,
                crn8,
                crn9));

        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());

        DatabaseServerConfig databaseServerConfig1 = new DatabaseServerConfig();
        databaseServerConfig1.setEnvironmentId("1");
        databaseServerConfig1.setId(1L);
        databaseServerConfig1.setClusterCrn(crn1);
        databaseServerConfig1.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response1 = new SslConfigV4Response();
        sslConfigV4Response1.setSslMode(SslMode.ENABLED);
        sslConfigV4Response1.setSslCertificateActiveVersion(1);
        sslConfigV4Response1.setSslCertificateHighestAvailableVersion(2);

        DatabaseServerConfig databaseServerConfig2 = new DatabaseServerConfig();
        databaseServerConfig2.setEnvironmentId("2");
        databaseServerConfig2.setId(2L);
        databaseServerConfig2.setClusterCrn(crn2);
        databaseServerConfig2.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response2 = new SslConfigV4Response();
        sslConfigV4Response2.setSslMode(SslMode.DISABLED);
        sslConfigV4Response2.setSslCertificateActiveVersion(1);
        sslConfigV4Response2.setSslCertificateHighestAvailableVersion(2);

        DatabaseServerConfig databaseServerConfig3 = new DatabaseServerConfig();
        databaseServerConfig3.setEnvironmentId("3");
        databaseServerConfig3.setId(3L);
        databaseServerConfig3.setClusterCrn(crn3);
        databaseServerConfig3.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response3 = new SslConfigV4Response();
        sslConfigV4Response3.setSslMode(SslMode.ENABLED);
        sslConfigV4Response3.setSslCertificateActiveVersion(1);
        sslConfigV4Response3.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response3.setSslCertificateExpirationDate(notExpired);

        DatabaseServerConfig databaseServerConfig4 = new DatabaseServerConfig();
        databaseServerConfig4.setEnvironmentId("4");
        databaseServerConfig4.setId(4L);
        databaseServerConfig4.setClusterCrn(crn4);
        databaseServerConfig4.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response4 = new SslConfigV4Response();
        sslConfigV4Response4.setSslMode(SslMode.DISABLED);
        sslConfigV4Response4.setSslCertificateActiveVersion(1);
        sslConfigV4Response4.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response4.setSslCertificateExpirationDate(notExpired);

        DatabaseServerConfig databaseServerConfig5 = new DatabaseServerConfig();
        databaseServerConfig5.setEnvironmentId("5");
        databaseServerConfig5.setId(5L);
        databaseServerConfig5.setClusterCrn(crn5);
        databaseServerConfig5.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response5 = new SslConfigV4Response();
        sslConfigV4Response5.setSslMode(SslMode.DISABLED);
        sslConfigV4Response5.setSslCertificateActiveVersion(1);
        sslConfigV4Response5.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response5.setSslCertificateExpirationDate(LocalDateTime
                .of(2000, 1, 1, 0, 0)
                .toEpochSecond(ZoneOffset.UTC));

        DatabaseServerConfig databaseServerConfig6 = new DatabaseServerConfig();
        databaseServerConfig6.setEnvironmentId("6");
        databaseServerConfig6.setId(6L);
        databaseServerConfig6.setClusterCrn(crn6);
        databaseServerConfig6.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response6 = new SslConfigV4Response();
        sslConfigV4Response6.setSslMode(SslMode.ENABLED);
        sslConfigV4Response6.setSslCertificateActiveVersion(1);
        sslConfigV4Response6.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response6.setSslCertificateExpirationDate(LocalDateTime
                .of(2000, 1, 1, 0, 0)
                .toEpochSecond(ZoneOffset.UTC));

        DatabaseServerConfig databaseServerConfig7 = new DatabaseServerConfig();
        databaseServerConfig7.setEnvironmentId("7");
        databaseServerConfig7.setId(7L);
        databaseServerConfig7.setClusterCrn(crn7);
        databaseServerConfig7.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response7 = new SslConfigV4Response();
        sslConfigV4Response7.setSslMode(SslMode.ENABLED);
        sslConfigV4Response7.setSslCertificateActiveVersion(1);
        sslConfigV4Response7.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response7.setSslCertificateExpirationDate(expireIn20Days);

        DatabaseServerConfig databaseServerConfig8 = new DatabaseServerConfig();
        databaseServerConfig8.setEnvironmentId("8");
        databaseServerConfig8.setId(8L);
        databaseServerConfig8.setClusterCrn(crn8);
        databaseServerConfig8.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response8 = new SslConfigV4Response();
        sslConfigV4Response8.setSslMode(SslMode.ENABLED);
        sslConfigV4Response8.setSslCertificateActiveVersion(1);
        sslConfigV4Response8.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response8.setSslCertificateExpirationDate(time);

        DatabaseServerConfig databaseServerConfig9 = new DatabaseServerConfig();
        databaseServerConfig9.setEnvironmentId("9");
        databaseServerConfig9.setId(9L);
        databaseServerConfig9.setClusterCrn(crn9);
        databaseServerConfig9.setDbStack(dbStack);

        SslConfigV4Response sslConfigV4Response9 = new SslConfigV4Response();
        sslConfigV4Response9.setSslMode(SslMode.ENABLED);
        sslConfigV4Response9.setSslCertificateActiveVersion(1);
        sslConfigV4Response9.setSslCertificateHighestAvailableVersion(1);
        sslConfigV4Response9.setSslCertificateExpirationDate(time);

        when(databaseServerConfigService.findAllByClusterCrns(anyString(), anySet()))
                .thenReturn(Set.of(
                        databaseServerConfig1,
                        databaseServerConfig2,
                        databaseServerConfig3,
                        databaseServerConfig4,
                        databaseServerConfig5,
                        databaseServerConfig6,
                        databaseServerConfig7,
                        databaseServerConfig8,
                        databaseServerConfig9));
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig1))
                .thenReturn(sslConfigV4Response1);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig2))
                .thenReturn(sslConfigV4Response2);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig3))
                .thenReturn(sslConfigV4Response3);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig4))
                .thenReturn(sslConfigV4Response4);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig5))
                .thenReturn(sslConfigV4Response5);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig6))
                .thenReturn(sslConfigV4Response6);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig7))
                .thenReturn(sslConfigV4Response7);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig8))
                .thenReturn(sslConfigV4Response8);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig9))
                .thenReturn(sslConfigV4Response9);

        when(timeUtil.getTimestampThatDaysAfterNow(anyInt())).thenReturn(time);

        ClusterDatabaseServerCertificateStatusV4Responses response = service.listDatabaseServersCertificateStatus(request, "accountId");

        assertNotNull(response);
        assertEquals(9, response.getResponses().size());
        Optional<ClusterDatabaseServerCertificateStatusV4Response> cl1 = response.getResponses()
                .stream()
                .filter(e -> e.getCrn().equals(crn1))
                .findFirst();
        assertEquals(SslCertStatus.OUTDATED, cl1.get().getSslStatus());
        Optional<ClusterDatabaseServerCertificateStatusV4Response> cl2 = response.getResponses()
                .stream()
                .filter(e -> e.getCrn().equals(crn2))
                .findFirst();
        assertEquals(SslCertStatus.UP_TO_DATE, cl2.get().getSslStatus());
        Optional<ClusterDatabaseServerCertificateStatusV4Response> cl3 = response.getResponses()
                .stream()
                .filter(e -> e.getCrn().equals(crn3))
                .findFirst();
        assertEquals(SslCertStatus.UP_TO_DATE, cl3.get().getSslStatus());
        Optional<ClusterDatabaseServerCertificateStatusV4Response> cl4 = response.getResponses()
                .stream()
                .filter(e -> e.getCrn().equals(crn4))
                .findFirst();
        assertEquals(SslCertStatus.UP_TO_DATE, cl4.get().getSslStatus());
        Optional<ClusterDatabaseServerCertificateStatusV4Response> cl5 = response.getResponses()
                .stream()
                .filter(e -> e.getCrn().equals(crn5))
                .findFirst();
        assertEquals(SslCertStatus.UP_TO_DATE, cl5.get().getSslStatus());
        Optional<ClusterDatabaseServerCertificateStatusV4Response> cl6 = response.getResponses()
                .stream()
                .filter(e -> e.getCrn().equals(crn6))
                .findFirst();
        assertEquals(SslCertStatus.OUTDATED, cl6.get().getSslStatus());
        Optional<ClusterDatabaseServerCertificateStatusV4Response> cl7 = response.getResponses()
                .stream()
                .filter(e -> e.getCrn().equals(crn7))
                .findFirst();
        assertEquals(SslCertStatus.OUTDATED, cl7.get().getSslStatus());
        Optional<ClusterDatabaseServerCertificateStatusV4Response> cl8 = response.getResponses()
                .stream()
                .filter(e -> e.getCrn().equals(crn8))
                .findFirst();
        assertEquals(SslCertStatus.OUTDATED, cl8.get().getSslStatus());
        Optional<ClusterDatabaseServerCertificateStatusV4Response> cl9 = response.getResponses()
                .stream()
                .filter(e -> e.getCrn().equals(crn9))
                .findFirst();
        assertEquals(SslCertStatus.UP_TO_DATE, cl9.get().getSslStatus());
    }
}