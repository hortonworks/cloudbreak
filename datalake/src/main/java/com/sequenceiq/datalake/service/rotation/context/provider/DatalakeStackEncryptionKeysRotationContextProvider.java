package com.sequenceiq.datalake.service.rotation.context.provider;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
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
public class DatalakeStackEncryptionKeysRotationContextProvider extends DatalakeConditionalRotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeStackEncryptionKeysRotationContextProvider.class);

    @Override
    public SecretType getSecret() {
        return DatalakeSecretType.STACK_ENCRYPTION_KEYS;
    }

    @Override
    public Map<RotationSource, SecretType> getPollingTypes() {
        return Map.of(RotationSource.CLOUDBREAK, CloudbreakSecretType.STACK_ENCRYPTION_KEYS);
    }

    @Override
    protected Function<SdxCluster, Boolean> getConditionalRotationFunction() {
        return sdxCluster -> {
            try {
                return StringUtils.equals(JsonUtil.readValue(sdxCluster.getStackRequest(), StackV4Request.class).getVariant(), "AWS_NATIVE_GOV");
            } catch (Exception e) {
                LOGGER.warn("Couldn't decide if compute monitoring credentials' rotation is applicable, thus allowing it.");
                return true;
            }
        };
    }
}
