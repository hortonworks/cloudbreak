package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static com.sequenceiq.common.api.type.EnvironmentType.isHybridFromEnvironmentTypeString;
import static java.util.Collections.singletonMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.util.CertProcessor;
import com.sequenceiq.cloudbreak.util.JavaUtil;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class JavaPillarDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaPillarDecorator.class);

    private static final String CRYPTO_COMPLY_PATH = "cryptoComplyPath";

    private static final String BOUNCY_CASTLE_TLS_PATH = "bouncyCastleTlsPath";

    private static final String BOUNCY_CASTLE_UTIL_TLS_PATH = "bouncyCastleUtilTlsPath";

    private static final String SASL_PROVIDER_PATH = "saslProviderPath";

    @Value("${cb.safelogic.8.cryptocomply.path:https://archive.releng.gov-dev.cloudera.com/p/safelogic/ccj-3.0.2.1/ccj-3.0.2.1.jar}")
    private String cryptoComplyPathForJava8;

    @Value("${cb.safelogic.17.cryptocomply.path:https://archive.releng.gov-dev.cloudera.com/p/safelogic/ccj-4.0.0/ccj-4.0.0-fips.jar}")
    private String cryptoComplyPathForJava17;

    @Value("${cb.safelogic.8.bouncycastletls.path:https://archive.releng.gov-dev.cloudera.com/p/safelogic/bctls-ccj-3.0.2.1/bctls.jar}")
    private String bouncyCastleTlsPathForJava8;

    @Value("${cb.safelogic.17.bouncycastletls.path:https://archive.releng.gov-dev.cloudera.com/p/safelogic/ccj-4.0.0/bctls-2.0.17.1.jar}")
    private String bouncyCastleTlsPathForJava17;

    @Value("${cb.safelogic.17.bouncycastleutiltls.path:https://archive.releng.gov-dev.cloudera.com/p/safelogic/ccj-4.0.0/bcutil-2.0.1.jar}")
    private String bouncyCastleUtilTlsPathForJava17;

    @Value("${cb.safelogic.17.saslprovider.path:https://archive.releng.gov-dev.cloudera.com/p/safelogic/" +
            "sasl-sha256aes-0.1.0.7.1.9.1021-6/sasl-sha256aes-0.1.0.7.1.9.1021-6.jar}")
    private String saslProviderPathForJava17;

    @Inject
    private PlatformAwareSdxConnector sdxConnector;

    @Inject
    private CertProcessor certProcessor;

    public Map<String, SaltPillarProperties> createJavaPillars(StackDto stackDto, DetailedEnvironmentResponse detailedEnvironmentResponse) {
        Map<String, Object> config = new HashMap<>();
        addVersion(stackDto, config);
        addSafeLogicProperties(stackDto, config);
        config.putAll(createCertificatePillar(stackDto, detailedEnvironmentResponse));
        return Map.of("java", new SaltPillarProperties("/java/init.sls", singletonMap("java", config)));
    }

    private Map<String, Object> createCertificatePillar(StackDto stackDto, DetailedEnvironmentResponse detailedEnvironmentResponse) {
        if (isHybridFromEnvironmentTypeString(detailedEnvironmentResponse.getEnvironmentType())
                && StringUtils.isNotBlank(detailedEnvironmentResponse.getRemoteEnvironmentCrn())
                && StackType.WORKLOAD == stackDto.getType()) {
            Optional<String> caCertificates = sdxConnector.getCACertsForEnvironment(detailedEnvironmentResponse.getCrn());
            if (caCertificates.isPresent() && StringUtils.isNotBlank(caCertificates.get())) {
                String[] certs = certProcessor.itemizeSingleLargeCertInput(caCertificates.get());
                Map<String, String> certByFingerPrint = Arrays.stream(certs)
                        .collect(Collectors.toMap(certProcessor::calculateSha256FingerprintForCert, Function.identity(), (c1, c2) -> c1));
                return Map.of("rootCertificates", certByFingerPrint);
            } else {
                LOGGER.info("Root Certificate is missing or empty");
                return Map.of();
            }
        } else {
            return Map.of();
        }
    }

    private void addVersion(StackDto stackDto, Map<String, Object> config) {
        Integer javaVersion = stackDto.getStack().getJavaVersion();
        if (javaVersion != null) {
            LOGGER.debug("Creating java pillar with version {}", javaVersion);
            config.put("version", javaVersion);
        } else {
            LOGGER.debug("Skip java version pillar as the version is not specified");
        }
    }

    private void addSafeLogicProperties(StackDto stackDto, Map<String, Object> config) {
        if (stackDto.isOnGovPlatformVariant()) {
            LOGGER.debug("Adding SafeLogic properties");
            Map<String, Object> safeLogicProperties = new HashMap<>();
            addCryptoComplyPath(stackDto, safeLogicProperties);
            addBouncyCastleTlsPath(stackDto, safeLogicProperties);
            addBouncyCastleUtilTlsPath(stackDto, safeLogicProperties);
            addSaslProviderPath(stackDto, safeLogicProperties);
            config.put("safelogic", safeLogicProperties);
        }
    }

    private void addSaslProviderPath(StackDto stackDto, Map<String, Object> config) {
        if (java17OrHigher(stackDto)) {
            addSafeLogicProperty(config, SASL_PROVIDER_PATH, saslProviderPathForJava17);
        }
    }

    private void addBouncyCastleUtilTlsPath(StackDto stackDto, Map<String, Object> config) {
        if (java17OrHigher(stackDto)) {
            addSafeLogicProperty(config, BOUNCY_CASTLE_UTIL_TLS_PATH, bouncyCastleUtilTlsPathForJava17);
        }
    }

    private void addBouncyCastleTlsPath(StackDto stackDto, Map<String, Object> config) {
        if (java17OrHigher(stackDto)) {
            addSafeLogicProperty(config, BOUNCY_CASTLE_TLS_PATH, bouncyCastleTlsPathForJava17);
        } else {
            addSafeLogicProperty(config, BOUNCY_CASTLE_TLS_PATH, bouncyCastleTlsPathForJava8);
        }
    }

    private void addCryptoComplyPath(StackDto stackDto, Map<String, Object> config) {
        if (java17OrHigher(stackDto)) {
            addSafeLogicProperty(config, CRYPTO_COMPLY_PATH, cryptoComplyPathForJava17);
        } else {
            addSafeLogicProperty(config, CRYPTO_COMPLY_PATH, cryptoComplyPathForJava8);
        }
    }

    private boolean java17OrHigher(StackDto stackDto) {
        return stackDto.getStack().getJavaVersion() != null && stackDto.getStack().getJavaVersion() >= JavaUtil.JAVA_17;
    }

    private void addSafeLogicProperty(Map<String, Object> config, String name, String value) {
        if (StringUtils.isBlank(value)) {
            String message = "Required SafeLogic property is blank for application: " + name;
            LOGGER.error(message);
            throw new IllegalStateException(message);
        }
        config.put(name, value);
    }
}
