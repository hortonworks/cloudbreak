package com.sequenceiq.datalake.service.sdx;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware.HardwareInfoGroupV4Response;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Service
public class SdxSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxSyncService.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    public void sync(String userCrn, String clusterName) {
        SdxCluster sdxCluster = sdxService.getSdxByNameInAccount(userCrn, clusterName);
        StackV4Response stackV4Response = sdxService.getDetail(sdxCluster.getClusterName(), Collections.singleton("hardware_info"));
        Set<HardwareInfoGroupV4Response> hardwareInfoGroups = stackV4Response.getHardwareInfoGroups();
        LOGGER.info("Hardware info groups for {}: {}", clusterName, hardwareInfoGroups);
        boolean hasUnhealthNodes = hardwareInfoGroups.stream()
                .flatMap(hardwareInfoGroup -> hardwareInfoGroup.getHardwareInfos().stream())
                .anyMatch(hardwareInfo -> "UNHEALTHY".equals(hardwareInfo.getState()));
        if (hasUnhealthNodes) {
            LOGGER.info("{} SDX has unhealth nodes", clusterName);
            sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.UNHEALTHY, "Datalake has unhealth nodes", sdxCluster);
        }
    }
}
