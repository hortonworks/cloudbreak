package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.check;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateEntryResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.support.SupportV4Endpoint;

@Component
public class DatabaseCertificateRotationOutdatedDatahubsCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCertificateRotationOutdatedDatahubsCollector.class);

    private final StackDtoService stackDtoService;

    private final StackService stackService;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private final SupportV4Endpoint supportV4Endpoint;

    private final DatabaseService databaseService;

    public DatabaseCertificateRotationOutdatedDatahubsCollector(
        StackDtoService stackDtoService,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory,
        SupportV4Endpoint supportV4Endpoint,
        DatabaseService databaseService,
        StackService stackService) {
        this.stackDtoService = stackDtoService;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
        this.supportV4Endpoint = supportV4Endpoint;
        this.databaseService = databaseService;
        this.stackService = stackService;
    }

    public List<String> getDatahubNamesWithOutdatedCerts(StackView datalake) {
        List<String> datahubNamesWithOutdatedCerts = new ArrayList<>();
        List<StackDto> stackViewV4Responses =
                stackDtoService.findAllByEnvironmentCrnAndStackType(
                        datalake.getEnvironmentCrn(),
                        List.of(WORKLOAD));
        SslCertificateEntryResponse latestCertificate = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> supportV4Endpoint.getLatestCertificate(datalake.getCloudPlatform(), datalake.getRegion()));
        for (StackDto response : stackViewV4Responses) {
            if (externalDbConfigured(response)) {
                StackDatabaseServerResponse databaseServerByCrn =
                        databaseService.getDatabaseServer(NameOrCrn.ofCrn(response.getResourceCrn()), response.getAccountId());
                int currentVersion = databaseServerByCrn.getSslConfig().getSslCertificateActiveVersion();
                int expectedVersion = latestCertificate.getVersion();
                if (currentVersion != latestCertificate.getVersion()) {
                    LOGGER.info("Data Hub with external DB and name {} ssl active verison {} the expected {}.",
                            response.getName(),
                            currentVersion,
                            expectedVersion);
                    datahubNamesWithOutdatedCerts.add(response.getName());
                }
            } else {
                Stack detailedStack = stackService.getByCrn(response.getResourceCrn());
                if (certBundleHasTheLatesCertificate(detailedStack, latestCertificate)) {
                    LOGGER.info("Data Hub with Embedded DB and name {} has no {} certificate.",
                            response.getName(),
                            latestCertificate.getCertPem());
                    datahubNamesWithOutdatedCerts.add(response.getName());
                }
            }
        }
        return datahubNamesWithOutdatedCerts;
    }

    private boolean certBundleHasTheLatesCertificate(Stack detailedStack, SslCertificateEntryResponse latestCertificate) {
        return !detailedStack.getCluster().getDbSslRootCertBundle().contains(latestCertificate.getCertPem());
    }

    private boolean externalDbConfigured(StackDto response) {
        return response.getExternalDatabaseCreationType() != null && !response.getExternalDatabaseCreationType().isEmbedded();
    }

}
