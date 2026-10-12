package com.sequenceiq.environment.environment.flow.encryptionprofile.validator;

import java.util.Collections;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

@Component
public class EncryptionProfileClusterResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileClusterResolver.class);

    private static final String LEGACY_ENCRYPTION_PROFILE_NAME = "cdp_default_fips_v1";

    private final SdxService sdxService;

    private final DatahubService datahubService;

    public EncryptionProfileClusterResolver(SdxService sdxService, DatahubService datahubService) {
        this.sdxService = sdxService;
        this.datahubService = datahubService;
    }

    public boolean isDatalake(StackViewV4Response stack) {
        return StackType.DATALAKE.name().equals(stack.getStackType());
    }

    public boolean hasOwnEncryptionProfile(StackViewV4Response stack) {
        String clusterEncryptionProfileCrn = isDatalake(stack)
                ? getDatalakeEncryptionProfileCrn(stack.getCrn())
                : getDatahubEncryptionProfileCrn(stack.getCrn());
        boolean ownEncryptionProfile = hasOwnEncryptionProfile(clusterEncryptionProfileCrn);
        if (ownEncryptionProfile) {
            LOGGER.info("Cluster {} has its own encryption profile [{}]", stack.getName(), clusterEncryptionProfileCrn);
        }
        return ownEncryptionProfile;
    }

    private String getDatalakeEncryptionProfileCrn(String datalakeCrn) {
        return Optional.ofNullable(sdxService.getDetailByCrn(datalakeCrn))
                .map(SdxClusterDetailResponse::getStackV4Response)
                .map(StackV4Response::getCluster)
                .map(ClusterV4Response::getEncryptionProfileCrn)
                .orElse(null);
    }

    private String getDatahubEncryptionProfileCrn(String datahubCrn) {
        return Optional.ofNullable(datahubService.getByCrn(datahubCrn, Collections.emptySet()))
                .map(StackV4Response::getCluster)
                .map(ClusterV4Response::getEncryptionProfileCrn)
                .orElse(null);
    }

    private boolean hasOwnEncryptionProfile(String encryptionProfileCrn) {
        if (StringUtils.isBlank(encryptionProfileCrn)) {
            return false;
        }
        Crn crn = Crn.safeFromString(encryptionProfileCrn);
        return !LEGACY_ENCRYPTION_PROFILE_NAME.equals(crn.getResource());
    }
}
