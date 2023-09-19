package com.sequenceiq.distrox.v1.distrox.service.upgrade.rds;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeService;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Response;

@Service
public class DistroXRdsUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRdsUpgradeService.class);

    @Inject
    private RdsUpgradeService rdsUpgradeService;

    public DistroXRdsUpgradeV1Response triggerUpgrade(NameOrCrn cluster, DistroXRdsUpgradeV1Request request) {
        TargetMajorVersion targetVersion = request.getTargetVersion();
        RdsUpgradeV4Response rdsUpgradeV4Response = rdsUpgradeService.upgradeRds(cluster, targetVersion, Boolean.TRUE.equals(request.getForced()));
        DistroXRdsUpgradeV1Response response = new DistroXRdsUpgradeV1Response(
                rdsUpgradeV4Response.getFlowIdentifier(), rdsUpgradeV4Response.getTargetVersion());
        LOGGER.debug("Rds upgrade requested, response {}", response);
        return response;
    }
}
