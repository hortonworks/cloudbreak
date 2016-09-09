package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import static com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class ZeppelinConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZeppelinConfigProvider.class);

    private static final String ZEPPELIN_MASTER = "ZEPPELIN_MASTER";
    private static final String ZEPPELIN_MASTER_CONFIG_FILE = "zeppelin-env";

    @Inject
    private Configuration freemarkerConfiguration;

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
            Map<String, Object> model = new HashMap<>();
            model.put("zeppelin_admin_password", stack.getCluster().getPassword());
            String shiroIniContent = processTemplateIntoString(freemarkerConfiguration.getTemplate("hdp/zeppelin/shiro_ini_content.ftl", "UTF-8"), model);
            configs.add(new BlueprintConfigurationEntry(ZEPPELIN_MASTER_CONFIG_FILE, "shiro_ini_content", shiroIniContent));
        } catch (TemplateException | IOException e) {
            LOGGER.error("Failed to read zeppelin config", e);
        }
        return configs;
    }
}
