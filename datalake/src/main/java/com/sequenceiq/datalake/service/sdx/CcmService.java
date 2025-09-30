package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.CCMV2_JUMPGATE_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.CCMV2_REQUIRED_VERSION;

import java.util.Comparator;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.api.type.Tunnel;

@Service
public class CcmService {

    public void validateCcmV2Requirement(Tunnel tunnel, String runtimeVersion) {
        Comparator<Versioned> versionComparator = new VersionComparator();

        if (tunnel != null && runtimeVersion != null) {
            switch (tunnel) {
                case CCMV2:
                    if (versionComparator.compare(() -> CCMV2_REQUIRED_VERSION, () -> runtimeVersion) > 0) {
                        throw new BadRequestException(String.format("Runtime version %s does not support Cluster Connectivity Manager. " +
                                "Please try creating a datalake with runtime version at least %s.", runtimeVersion, CCMV2_REQUIRED_VERSION));
                    }
                    break;
                case CCMV2_JUMPGATE:
                    if (versionComparator.compare(() -> CCMV2_JUMPGATE_REQUIRED_VERSION, () -> runtimeVersion) > 0) {
                        throw new BadRequestException(String.format("Runtime version %s does not support Cluster Connectivity Manager. " +
                                "Please try creating a datalake with runtime version at least %s.", runtimeVersion, CCMV2_JUMPGATE_REQUIRED_VERSION));
                    }
                    break;
                default:
                    break;
            }
        }
    }
}