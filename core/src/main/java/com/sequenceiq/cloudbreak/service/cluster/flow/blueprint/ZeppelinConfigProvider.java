package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class ZeppelinConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZeppelinConfigProvider.class);

    private static final String ZEPPELIN_MASTER = "ZEPPELIN_MASTER";
    private static final String ZEPPELIN_MASTER_CONFIG_FILE = "zeppelin-env";

    @Inject
    private BlueprintProcessor blueprintProcessor;

    public String addToBlueprint(Stack stack, String blueprintText) {
        if (blueprintProcessor.componentExistsInBlueprint(ZEPPELIN_MASTER, blueprintText)) {
            LOGGER.info("Zeppelin exists in Blueprint");
            List<BlueprintConfigurationEntry> configs = getConfigs(stack);
            blueprintText = blueprintProcessor.addConfigEntries(blueprintText, configs, true);
        }
        return blueprintText;
    }


    private List<BlueprintConfigurationEntry> getConfigs(Stack stack) {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        try {
            String shiroIniContent = FileReaderUtils.readFileFromClasspath("hdp/zeppelin/shiro_ini_content.txt").replaceAll("ZEPPELIN_PASSWORD", stack.getCluster().getPassword());
            configs.add(new BlueprintConfigurationEntry(ZEPPELIN_MASTER_CONFIG_FILE, "shiro_ini_content", shiroIniContent));
            String zeppelinEnvContent = FileReaderUtils.readFileFromClasspath("hdp/zeppelin/zeppelin_env_content.txt");
            configs.add(new BlueprintConfigurationEntry(ZEPPELIN_MASTER_CONFIG_FILE, "zeppelin_env_content", zeppelinEnvContent));
        } catch (IOException e) {
            LOGGER.error("Failed to read zeppelin config", e);
        }

        return configs;
    }
}
