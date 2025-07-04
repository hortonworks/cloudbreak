package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static com.sequenceiq.common.api.type.EnvironmentType.isHybridFromEnvironmentTypeString;
import static java.util.Collections.singletonMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class JavaPillarDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaPillarDecorator.class);

    @Value("${cb.safelogic.cryptocomply.path:}")
    private String cryptoComplyPath;

    @Value("${cb.safelogic.cryptocomply.hash:}")
    private String cryptoComplyHash;

    @Value("${cb.safelogic.bouncycastletls.path:}")
    private String bouncyCastleTlsPath;

    @Value("${cb.safelogic.bouncycastletls.hash:}")
    private String bouncyCastleTlsHash;

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
                        .collect(Collectors.toMap(certProcessor::calculateSha256FingerprintForCert, cert -> cert));
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
            addSafeLogicProperty(safeLogicProperties, "cryptoComplyPath", cryptoComplyPath);
            addSafeLogicProperty(safeLogicProperties, "cryptoComplyHash", cryptoComplyHash);
            addSafeLogicProperty(safeLogicProperties, "bouncyCastleTlsPath", bouncyCastleTlsPath);
            addSafeLogicProperty(safeLogicProperties, "bouncyCastleTlsHash", bouncyCastleTlsHash);
            config.put("safelogic", safeLogicProperties);
        }
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
