package com.sequenceiq.cloudbreak.rotation.secret.demo;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;

public class DemoSecretExceptionProviderUtil {

    public static final String ROTATION_FAILURE_KEY = "rotation_failure";

    public static final String ROLLBACK_FAILURE_KEY = "rollback_failure";

    public static final String FINALIZE_FAILURE_KEY = "finalize_failure";

    public static final String PREVALIDATE_FAILURE_KEY = "prevalidate_failure";

    public static final String POSTVALIDATE_FAILURE_KEY = "postvalidate_failure";

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoSecretExceptionProviderUtil.class);

    private DemoSecretExceptionProviderUtil() {

    }

    public static Runnable getCustomJobRunnableByProperties(String propertyKey, String resourceCrn, Map<String, String> additionalProperties) {
        if (MapUtils.emptyIfNull(additionalProperties).containsKey(propertyKey)) {
            return () -> {
                throw new SecretRotationException(String.format("Simulate secret rotation failure for resource %s.", resourceCrn));
            };
        } else {
            return () -> LOGGER.info("This is a demo rotation, nothing will actually happen for resource {}", resourceCrn);
        }
    }
}
