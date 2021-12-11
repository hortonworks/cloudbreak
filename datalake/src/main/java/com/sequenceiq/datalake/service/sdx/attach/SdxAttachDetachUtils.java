package com.sequenceiq.datalake.service.sdx.attach;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@Component
public class SdxAttachDetachUtils {
    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private DatabaseServerV4Endpoint redbeamsServerEndpoint;

    public void updateClusterNameAndCrn(SdxCluster sdxCluster, String newName, String newCrn) {
        sdxCluster.setClusterName(newName);
        sdxCluster.setOriginalCrn(sdxCluster.getCrn());
        sdxCluster.setCrn(newCrn);
        sdxCluster.setStackCrn(newCrn);
    }

    public void updateStack(String originalName, String newName, String newCrn) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                stackV4Endpoint.updateNameAndCrn(
                        0L, originalName, initiatorUserCrn, newName, newCrn
                )
        );
    }

    public void updateExternalDatabase(SdxCluster cluster, String originalCrn) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                redbeamsServerEndpoint.updateClusterCrn(
                        cluster.getEnvCrn(), originalCrn, cluster.getCrn(), initiatorUserCrn
                )
        );
    }
}
