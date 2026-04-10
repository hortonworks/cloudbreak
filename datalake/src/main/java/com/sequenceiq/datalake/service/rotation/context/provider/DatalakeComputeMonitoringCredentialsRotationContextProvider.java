package com.sequenceiq.datalake.service.rotation.context.provider;

import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Component
public class DatalakeComputeMonitoringCredentialsRotationContextProvider extends DatalakeConditionalRotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeComputeMonitoringCredentialsRotationContextProvider.class);

    @Override
    public SecretType getSecret() {
        return DatalakeSecretType.COMPUTE_MONITORING_CREDENTIALS;
    }

    @Override
    public Map<RotationSource, SecretType> getPollingTypes() {
        return Map.of(RotationSource.CLOUDBREAK, CloudbreakSecretType.COMPUTE_MONITORING_CREDENTIALS);
    }

    @Override
    protected Function<SdxCluster, Boolean> getConditionalRotationFunction() {
        return sdxCluster -> {
            try {
                return JsonUtil.readValue(sdxCluster.getStackRequest(), StackV4Request.class).getTelemetry().getFeatures().getMonitoring().getEnabled();
            } catch (Exception e) {
                LOGGER.warn("Couldn't decide if compute monitoring credentials' rotation is applicable, thus allowing it.");
                return true;
            }
        };
    }
}

