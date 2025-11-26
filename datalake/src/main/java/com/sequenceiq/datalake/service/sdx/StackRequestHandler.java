package com.sequenceiq.datalake.service.sdx;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Service
public class StackRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackRequestHandler.class);

    @Inject
    private CDPConfigService cdpConfigService;

    public StackV4Request getStackRequest(SdxClusterShape shape, StackV4Request internalStackV4Request,
            CloudPlatform cloudPlatform, String runtimeVersion, ImageSettingsV4Request imageSettingsV4Request,
            Architecture architecture) {
        StackV4Request stackV4Request = internalStackV4Request;

        if (!SdxClusterShape.CUSTOM.equals(shape)) {
            CDPConfigKey cdpConfigKey = new CDPConfigKey(cloudPlatform, shape, runtimeVersion, architecture);
            stackV4Request = cdpConfigService.getConfigForKey(cdpConfigKey);
            if (stackV4Request == null) {
                String message = "Can't find template for " + cdpConfigKey;
                LOGGER.error(message);
                throw new BadRequestException(message);
            }
            if (imageSettingsV4Request != null) {
                stackV4Request.setImage(imageSettingsV4Request);
            }
            ClouderaManagerV4Request clouderaManagerV4Request = Optional.ofNullable(internalStackV4Request)
                    .map(StackV4Request::getCluster)
                    .map(ClusterV4Request::getCm)
                    .orElse(null);
            if (clouderaManagerV4Request != null) {
                stackV4Request.getCluster().setCm(clouderaManagerV4Request);
            }
        }

        return stackV4Request;
    }

    public void setStackRequestParams(StackV4Request stackV4Request, Integer javaVersion, boolean razEnabled, boolean rmsEnabled, String encryptionProfileCrn) {
        if (javaVersion != null) {
            stackV4Request.setJavaVersion(javaVersion);
        }
        // We have provided a --ranger-raz-enabled flag in the CLI, but it will
        // get overwritten if you use a custom json (using --cli-json). To avoid
        // this, we will set the raz enablement here. See CB-7474 for more details
        stackV4Request.getCluster().setRangerRazEnabled(razEnabled);
        stackV4Request.getCluster().setRangerRmsEnabled(rmsEnabled);
        stackV4Request.getCluster().setEncryptionProfileCrn(encryptionProfileCrn);
    }
}
