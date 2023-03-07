package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

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

    public void decorateWithJavaProperties(StackDto stackDto, Map<String, SaltPillarProperties> servicePillar) {
        Map<String, Object> config = new HashMap<>();
        addVersion(stackDto, config);
        addSafeLogicProperties(stackDto, config);
        servicePillar.put("java", new SaltPillarProperties("/java/init.sls", singletonMap("java", config)));
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
