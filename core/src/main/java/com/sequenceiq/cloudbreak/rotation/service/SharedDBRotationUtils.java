package com.sequenceiq.cloudbreak.rotation.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.externaldatabase.PollingConfig;
import com.sequenceiq.cloudbreak.service.rdsconfig.HiveRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Component
public class SharedDBRotationUtils {

    private static final int POLL_CONFIG_SLEEP_TIME_SEC = 5;

    private static final int POLL_CONFIG_TIMEOUT_MIN = 15;

    @Inject
    private RedbeamsDbServerConfigurer redbeamsDbServerConfigurer;

    @Inject
    private DatabaseCommon dbCommon;

    @Inject
    private HiveRdsConfigProvider hiveRdsConfigProvider;

    @Inject
    private HiveMetastoreConfigProvider hiveMetastoreConfigProvider;

    @Inject
    private FlowService flowService;

    public String getJdbcConnectionUrl(String dbServerCrn) {
        DatabaseServerV4Response dbServer = redbeamsDbServerConfigurer.getDatabaseServer(dbServerCrn);
        return dbCommon.getJdbcConnectionUrl(dbServer.getDatabaseVendor(), dbServer.getHost(), dbServer.getPort(), Optional.of(hiveRdsConfigProvider.getDb()));
    }

    public String getNewDatabaseUserName(StackDto stack) {
        return hiveRdsConfigProvider.getDbUser() + '_' + stack.getName() + '_' + new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
    }

    public void pollFlow(FlowIdentifier flowIdentifier) {
        PollingConfig pollingConfig = getFlowPollingConfig();
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                .run(() -> {
                    FlowCheckResponse flowState = flowService.getFlowState(flowIdentifier.getPollableId());
                    if (flowState.getHasActiveFlow()) {
                        return AttemptResults.justContinue();
                    } else if (flowState.getLatestFlowFinalizedAndFailed()) {
                        return AttemptResults.breakFor("Flow failed!");
                    } else {
                        return AttemptResults.justFinish();
                    }
                });
    }

    public Table<String, String, String> getConfigTableForRotationInCM(RDSConfig rdsConfig) {
        Table<String, String, String> configTable = HashBasedTable.create();
        configTable.put(hiveMetastoreConfigProvider.getServiceType(), hiveMetastoreConfigProvider.dbUserKey(), rdsConfig.getConnectionUserName());
        configTable.put(hiveMetastoreConfigProvider.getServiceType(), hiveMetastoreConfigProvider.dbPasswordKey(), rdsConfig.getConnectionPassword());
        return configTable;
    }

    private PollingConfig getFlowPollingConfig() {
        return PollingConfig.builder()
                .withStopPollingIfExceptionOccured(true)
                .withSleepTime(POLL_CONFIG_SLEEP_TIME_SEC)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(POLL_CONFIG_TIMEOUT_MIN)
                .withTimeoutTimeUnit(TimeUnit.MINUTES)
                .build();
    }
}
