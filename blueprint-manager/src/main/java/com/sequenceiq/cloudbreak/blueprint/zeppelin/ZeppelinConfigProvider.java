package com.sequenceiq.cloudbreak.blueprint.zeppelin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class ZeppelinConfigProvider implements BlueprintComponentConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZeppelinConfigProvider.class);

    //This is needed since the API has hanged beetween Ambari 2.4 and 2.5, in case of Ambari 2.4 zeppelin-env needs to be set
    private static final String HDP_2_5_VERSION = "2.5";

    private static final String ZEPPELIN_MASTER_CONFIG_FILES_2_5 = "zeppelin-env";

    private static final String ZEPPELIN_MASTER_CONFIG_FILES_2_6 = "zeppelin-shiro-ini";

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Override
    public String customTextManipulation(BlueprintPreparationObject source, String blueprintText) {
        LOGGER.info("Zeppelin exists in Blueprint");
        List<BlueprintConfigurationEntry> configs = getConfigs(source.getStack().getCluster(), source.getStackRepoDetails());
        return blueprintProcessor.addConfigEntries(blueprintText, configs, false);
    }

    @Override
    public Set<String> components() {
        return Sets.newHashSet("ZEPPELIN_MASTER");
    }

    private List<BlueprintConfigurationEntry> getConfigs(Cluster cluster, StackRepoDetails stackRepoDetails) {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        try {
            Map<String, Object> model = new HashMap<>();
            Gateway gateway = cluster.getGateway();
            model.put("zeppelin_admin_password", cluster.getPassword());
            model.put("knoxGateway", gateway.getEnableGateway());
            String shiroIniContent = FreeMarkerTemplateUtils
                    .processTemplateIntoString(freemarkerConfiguration.getTemplate("hdp/zeppelin/shiro_ini_content.ftl", "UTF-8"), model);

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
