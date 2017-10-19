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

import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class ZeppelinConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZeppelinConfigProvider.class);

    private static final String ZEPPELIN_MASTER = "ZEPPELIN_MASTER";

    //This is needed since the API has hanged beetween Ambari 2.4 and 2.5, in case of Ambari 2.4 zeppelin-env needs to be set
    private static final String HDP_2_5_VERSION = "2.5";

    private static final String ZEPPELIN_MASTER_CONFIG_FILES_2_5 = "zeppelin-env";

    private static final String ZEPPELIN_MASTER_CONFIG_FILES_2_6 = "zeppelin-shiro-ini";

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Inject
    private ClusterComponentConfigProvider componentConfigProvider;

    public String addToBlueprint(Stack stack, String blueprintText) {
        if (blueprintProcessor.componentExistsInBlueprint(ZEPPELIN_MASTER, blueprintText)) {
            LOGGER.info("Zeppelin exists in Blueprint");
            List<BlueprintConfigurationEntry> configs = getConfigs(stack.getId(), stack.getCluster());
            blueprintText = blueprintProcessor.addConfigEntries(blueprintText, configs, false);
        }
        return blueprintText;
    }

    private List<BlueprintConfigurationEntry> getConfigs(Long stackId, Cluster cluster) {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        try {
            Map<String, Object> model = new HashMap<>();
            Gateway gateway = cluster.getGateway();
            model.put("zeppelin_admin_password", cluster.getPassword());
            model.put("knoxGateway", gateway.getEnableGateway());
            String shiroIniContent = processTemplateIntoString(freemarkerConfiguration.getTemplate("hdp/zeppelin/shiro_ini_content.ftl", "UTF-8"), model);

            StackRepoDetails stackRepoDetails = componentConfigProvider.getHDPRepo(cluster.getId());
            if (stackRepoDetails != null && stackRepoDetails.getHdpVersion() != null && !stackRepoDetails.getHdpVersion().startsWith(HDP_2_5_VERSION)) {
                configs.add(new BlueprintConfigurationEntry(ZEPPELIN_MASTER_CONFIG_FILES_2_6, "shiro_ini_content", shiroIniContent));
            } else {
                configs.add(new BlueprintConfigurationEntry(ZEPPELIN_MASTER_CONFIG_FILES_2_5, "shiro_ini_content", shiroIniContent));
            }
        } catch (TemplateException | IOException e) {
            LOGGER.error("Failed to read zeppelin config", e);
        }
        return configs;
    }
}
