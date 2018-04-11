package com.sequenceiq.cloudbreak.service.rdsconfig;

import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateProcessorFactory;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateTextProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class HiveRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "hive";

    private static final String[] PATH = {TemplateTextProcessor.CONFIGURATIONS_NODE, "hive-site"};

    private static final String[] CONFIGURATIONS = {"javax.jdo.option.ConnectionURL", "javax.jdo.option.ConnectionDriverName",
            "javax.jdo.option.ConnectionUserName", "javax.jdo.option.ConnectionPassword"};

    @Value("${cb.hive.database.user:hive}")
    private String hiveDbUser;

    @Value("${cb.hive.database.db:hive}")
    private String hiveDb;

    @Value("${cb.hive.database.port:5432}")
    private String hiveDbPort;

    @Inject
    private TemplateProcessorFactory blueprintProcessorFactory;

    private boolean isRdsConfigNeedForHiveMetastore(Blueprint blueprint) {
        TemplateTextProcessor blueprintProcessor = blueprintProcessorFactory.get(blueprint.getBlueprintText());
        return blueprintProcessor.componentExistsInBlueprint("HIVE_METASTORE")
                && !blueprintProcessor.componentExistsInBlueprint("MYSQL_SERVER")
                && !blueprintProcessor.isAllConfigurationExistsInPathUnderConfigurationNode(createPathListFromConfingurations(PATH, CONFIGURATIONS));
    }

    @Override
    protected String getDbUser() {
        return hiveDbUser;
    }

    @Override
    protected String getDb() {
        return hiveDb;
    }

    @Override
    protected String getDbPort() {
        return hiveDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected RdsType getRdsType() {
        return RdsType.HIVE;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint) {
        return isRdsConfigNeedForHiveMetastore(blueprint);
    }
}
