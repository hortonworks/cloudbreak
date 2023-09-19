package com.sequenceiq.distrox.v1.distrox.service.upgrade.rds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeService;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class DistroXRdsUpgradeServiceTest {

    @Mock
    private RdsUpgradeService rdsUpgradeService;

    @InjectMocks
    private DistroXRdsUpgradeService underTest;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testTriggerUpgrade(boolean forced) {
        DistroXRdsUpgradeV1Request request = new DistroXRdsUpgradeV1Request();
        request.setForced(forced);
        request.setTargetVersion(TargetMajorVersion.VERSION14);
        NameOrCrn cluster = NameOrCrn.ofName("asdf");
        RdsUpgradeV4Response rdsUpgradeV4Response = new RdsUpgradeV4Response();
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollid");
        rdsUpgradeV4Response.setFlowIdentifier(flowIdentifier);
        rdsUpgradeV4Response.setTargetVersion(request.getTargetVersion());
        when(rdsUpgradeService.upgradeRds(cluster, request.getTargetVersion(), forced)).thenReturn(rdsUpgradeV4Response);

        DistroXRdsUpgradeV1Response response = underTest.triggerUpgrade(cluster, request);

        assertEquals(rdsUpgradeV4Response.getTargetVersion(), response.getTargetVersion());
        assertEquals(rdsUpgradeV4Response.getFlowIdentifier(), response.getFlowIdentifier());
    }

}