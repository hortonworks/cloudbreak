package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CONFIGURATION_UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.SHARED_SERVICE_DB_SECRET_ROTATION_CM_CONFIG_UPDATE_DH;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.SHARED_SERVICE_DB_SECRET_ROTATION_CM_CONFIG_UPDATE_DL;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.SHARED_SERVICE_DB_SECRET_ROTATION_CONFIG_UPDATE_FAILED_DH;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.SHARED_SERVICE_DB_SECRET_ROTATION_CONFIG_UPDATE_FAILED_DL;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.SHARED_SERVICE_DB_SECRET_ROTATION_CONFIG_UPDATE_FINISHED_DH;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.SHARED_SERVICE_DB_SECRET_ROTATION_CONFIG_UPDATE_FINISHED_DL;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.SHARED_SERVICE_DB_SECRET_ROTATION_PILLAR_CONFIG_UPDATE_DH;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.SHARED_SERVICE_DB_SECRET_ROTATION_PILLAR_CONFIG_UPDATE_DL;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HIVE_SERVICE;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ClusterServicesRestartService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Service
public class DatahubSharedServiceRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubSharedServiceRotationService.class);

    @Inject
    private StackDtoService stackService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private ClusterServicesRestartService clusterServicesRestartService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public void updateAllRelevantDatahub(StackDto datalake) {
        List<StackDto> datahubList = stackService.findAllByEnvironmentCrnAndStackType(datalake.getEnvironmentCrn(), List.of(WORKLOAD));
        datahubList.forEach(datahub -> {
            try {
                if (isHmsPresent(datahub)) {
                    stackUpdater.updateStackStatus(datahub.getId(), CONFIGURATION_UPDATE_IN_PROGRESS);
                    pillarUpdateOnDatahub(datahub, datalake);
                    cmServiceRestartOnDatahub(datahub, datalake);
                    moveDatahubToAvailableState(datahub, datalake);
                } else {
                    LOGGER.info("Data Hub [{}] hasn't got HMS sevice, skipping config update.", datahub.getResourceCrn());
                }
            } catch (Exception e) {
                handleConfigUpdateErrorOnDatahub(datahub, e, datalake);
            }
        });
    }

    public void validateAllDatahubAvailable(StackDto datalake) {
        List<StackDto> datahubList = stackService.findAllByEnvironmentCrnAndStackType(datalake.getEnvironmentCrn(), List.of(WORKLOAD));
        if (!datahubList.stream()
                .filter(datahub -> isHmsPresent(datahub))
                .allMatch(datahub -> Status.AVAILABLE.equals(datahub.getStatus()))) {
            throw new SecretRotationException("All Data Hub clusters in the environment should be available in order to " +
                    "rotate HMS database password properly!");
        }
    }

    private void handleConfigUpdateErrorOnDatahub(StackDto datahub, Exception e, StackDto datalake) {
        LOGGER.error("Updating Data Hub [{}] failed. Reason: ", datahub.getResourceCrn(), e);
        notifyDatahubAndCorrespondingDatalake(UPDATE_FAILED.name(), datahub, SHARED_SERVICE_DB_SECRET_ROTATION_CONFIG_UPDATE_FAILED_DH,
                datalake, SHARED_SERVICE_DB_SECRET_ROTATION_CONFIG_UPDATE_FAILED_DL);
        stackUpdater.updateStackStatus(datahub.getId(), DetailedStackStatus.CONFIGURATION_UPDATE_FAILED);
    }

    private void moveDatahubToAvailableState(StackDto datahub, StackDto datalake) {
        LOGGER.info("Updating Data Hub [{}] successfully finished.", datahub.getResourceCrn());
        stackUpdater.updateStackStatus(datahub.getId(), DetailedStackStatus.AVAILABLE);
        notifyDatahubAndCorrespondingDatalake(AVAILABLE.name(), datahub, SHARED_SERVICE_DB_SECRET_ROTATION_CONFIG_UPDATE_FINISHED_DH,
                datalake, SHARED_SERVICE_DB_SECRET_ROTATION_CONFIG_UPDATE_FINISHED_DL);
    }

    private void cmServiceRestartOnDatahub(StackDto datahub, StackDto datalake) throws CloudbreakException {
        LOGGER.info("Updating CM configuration for Data Hub [{}]", datahub.getResourceCrn());
        notifyDatahubAndCorrespondingDatalake(CONFIGURATION_UPDATE_IN_PROGRESS.name(), datahub, SHARED_SERVICE_DB_SECRET_ROTATION_CM_CONFIG_UPDATE_DH,
                datalake, SHARED_SERVICE_DB_SECRET_ROTATION_CM_CONFIG_UPDATE_DL);
        clusterServicesRestartService.refreshCluster(datahub);
    }

    private void pillarUpdateOnDatahub(StackDto datahub, StackDto datalake) {
        LOGGER.info("Updating pillar config for Data Hub [{}]", datahub.getResourceCrn());
        notifyDatahubAndCorrespondingDatalake(CONFIGURATION_UPDATE_IN_PROGRESS.name(), datahub, SHARED_SERVICE_DB_SECRET_ROTATION_PILLAR_CONFIG_UPDATE_DH,
                datalake, SHARED_SERVICE_DB_SECRET_ROTATION_PILLAR_CONFIG_UPDATE_DL);
        clusterHostServiceRunner.updateClusterConfigs(datahub, false);
    }

    private void notifyDatahubAndCorrespondingDatalake(String eventType, StackDto datahub, ResourceEvent datahubEvent,
            StackDto datalake, ResourceEvent datalakeEvent) {
        flowMessageService.fireEventAndLog(datahub.getId(), eventType, datahubEvent);
        flowMessageService.fireEventAndLog(datalake.getId(), eventType, datalakeEvent, datahub.getResourceCrn());
    }

    private boolean isHmsPresent(StackDto stack) {
        CmTemplateProcessor cm = cmTemplateProcessorFactory.get(stack.getBlueprintJsonText());
        return cm.isServiceTypePresent(HIVE_SERVICE);
    }
}
