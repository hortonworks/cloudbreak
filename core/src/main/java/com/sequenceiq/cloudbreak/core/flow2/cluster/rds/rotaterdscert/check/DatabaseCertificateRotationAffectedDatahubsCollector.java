package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert.check;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class DatabaseCertificateRotationAffectedDatahubsCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCertificateRotationAffectedDatahubsCollector.class);

    private final StackDtoService stackDtoService;

    private final StackService stackService;

    private final WorkspaceService workspaceService;

    public DatabaseCertificateRotationAffectedDatahubsCollector(
        StackDtoService stackDtoService,
        StackService stackService,
        WorkspaceService workspaceService) {
        this.stackDtoService = stackDtoService;
        this.stackService = stackService;
        this.workspaceService = workspaceService;
    }

    public List<String> collectDatahubNamesWhereCertCheckNecessary(String environmentCrn) {
        LOGGER.debug("Checking if Datahub certificate check is necessary.");
        List<String> dataHubsWithHiveMetastoreOrExternalDatabase = new ArrayList<>();
        List<StackDto> distroxViews = new ArrayList<>(
                getIfNotNullOtherwise(stackDtoService.findAllByEnvironmentCrnAndStackType(environmentCrn, List.of(StackType.WORKLOAD)),
                        Collections.emptyList()));
        LOGGER.debug("{} Datahub(s) found in environment {}.", distroxViews.size(), environmentCrn);
        for (StackDto distroxView : distroxViews) {
            Stack datahub = stackService.getByCrn(distroxView.getResourceCrn());
            if (isBlueprintContainsHiveMetastore(datahub.getCluster()) || isDatahubHasExternalDatabase(datahub)) {
                LOGGER.debug("Datahub {} is using external database or contains Hive Metastore, " +
                        "thus certificate check is necessary.", datahub.getResourceCrn());
                dataHubsWithHiveMetastoreOrExternalDatabase.add(datahub.getName());
            }
        }
        LOGGER.debug("No Datahub is using external database or contains Hive Metastore, " +
                "thus certificate check is unnecessary.");
        return dataHubsWithHiveMetastoreOrExternalDatabase;
    }

    private boolean isBlueprintContainsHiveMetastore(Cluster cluster) {
        return getIfNotNullOtherwise(cluster.getBlueprint(),
                blueprint -> getIfNotNullOtherwise(blueprint.getBlueprintJsonText(),
                        blueprintText -> blueprintText.contains("HIVEMETASTORE"),
                        false),
                false);
    }

    private boolean isDatahubHasExternalDatabase(Stack dataHubStack) {
        return getIfNotNullOtherwise(
                dataHubStack.getDatabase(),
                externalDatabase -> !externalDatabase.getExternalDatabaseAvailabilityType().isEmbedded(),
                false
        );
    }

}
