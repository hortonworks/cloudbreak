package com.sequenceiq.mock.salt.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatus;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.spi.SpiStoreService;

@Component
public class ManageStatusSaltResponse implements SaltResponse {

    @Inject
    private SpiStoreService spiStoreService;

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        MinionStatusSaltResponse minionStatusSaltResponse = new MinionStatusSaltResponse();
        List<MinionStatus> minionStatusList = new ArrayList<>();
        MinionStatus minionStatus = new MinionStatus();
        ArrayList<String> upList = new ArrayList<>();
        minionStatus.setUp(upList);
        ArrayList<String> downList = new ArrayList<>();
        minionStatus.setDown(downList);
        minionStatusList.add(minionStatus);
        minionStatusSaltResponse.setResult(minionStatusList);

        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : spiStoreService.getMetadata(mockUuid)) {
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
                upList.add("host-" + privateIp.replace(".", "-") + ".example.com");
            }
        }
        return minionStatusSaltResponse;
    }

    @Override
    public String cmd() {
        return "manage.status";
    }
}
