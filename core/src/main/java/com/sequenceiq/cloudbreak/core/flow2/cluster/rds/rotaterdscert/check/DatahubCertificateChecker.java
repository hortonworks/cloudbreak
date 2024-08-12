package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert.check;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class DatahubCertificateChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubCertificateChecker.class);

    private final DatabaseCertificateRotationOutdatedDatahubsCollector databaseCertificateRotationOutdatedDatahubsCollector;

    private final DatabaseCertificateRotationAffectedDatahubsCollector databaseCertificateRotationAffectedDatahubsCollector;

    public DatahubCertificateChecker(
        DatabaseCertificateRotationOutdatedDatahubsCollector databaseCertificateRotationOutdatedDatahubsCollector,
        DatabaseCertificateRotationAffectedDatahubsCollector databaseCertificateRotationAffectedDatahubsCollector) {
        this.databaseCertificateRotationOutdatedDatahubsCollector = databaseCertificateRotationOutdatedDatahubsCollector;
        this.databaseCertificateRotationAffectedDatahubsCollector = databaseCertificateRotationAffectedDatahubsCollector;
    }

    public List<String> collectDatahubsWhichMustBeUpdated(StackView stack) {
        if (StackType.DATALAKE.equals(stack.getType())) {
            List<String> datahubNamesWithOutdatedCerts = databaseCertificateRotationOutdatedDatahubsCollector
                    .getDatahubNamesWithOutdatedCerts(stack);
            List<String> datahubNamesWithHiveMetastoreOrExternalDatabase = databaseCertificateRotationAffectedDatahubsCollector
                    .collectDatahubNamesWhereCertCheckNecessary(stack.getEnvironmentCrn());
            return datahubNamesWithOutdatedCerts.stream()
                    .filter(c -> datahubNamesWithHiveMetastoreOrExternalDatabase.contains(c))
                    .collect(Collectors.toList());
        } else {
            return List.of();
        }
    }
}
