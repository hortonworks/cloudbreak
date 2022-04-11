package com.sequenceiq.datalake.service.sdx.attach;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@Component
public class SdxAttachDetachUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxAttachDetachUtils.class);

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private DatabaseServerV4Endpoint redbeamsServerEndpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public void updateClusterNameAndCrn(SdxCluster sdxCluster, String newName, String newCrn) {
        sdxCluster.setClusterName(newName);
        sdxCluster.setOriginalCrn(sdxCluster.getCrn());
        sdxCluster.setCrn(newCrn);
        sdxCluster.setStackCrn(newCrn);
    }

    public void updateStack(String originalName, String newName, String newCrn) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(
            regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
            () -> {
                try {
                    stackV4Endpoint.updateNameAndCrn(
                            0L, originalName, initiatorUserCrn, newName, newCrn
                    );
                } catch (NotFoundException e) {
                    LOGGER.warn("Stack not found for original name: '" + originalName +
                            "'. Skipping update of stack.", e);
                }
            }
        );
    }

    public void updateExternalDatabase(SdxCluster cluster, String originalCrn) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> {
                    try {
                        redbeamsServerEndpoint.updateClusterCrn(
                                cluster.getEnvCrn(), originalCrn, cluster.getCrn(), initiatorUserCrn
                        );
                    } catch (NotFoundException e) {
                        LOGGER.warn("External DB not found for original CRN: '" + originalCrn +
                                "'. Skipping update of external DB.", e);
                    }
                }
        );
    }
}
