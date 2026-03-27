package com.sequenceiq.cloudbreak.service.rdsconfig;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase.HbaseRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class AtlasRdsConfigProvider extends AbstractRdsConfigProvider {
    private static final String PILLAR_KEY = "atlas";

    private static final String ATLAS_SERVER_COMPONENT = "ATLAS_SERVER";

    private static final String IDBROKER_COMPONENT = "IDBROKER";

    @Value("${cb.atlas.database.user:atlas}")
    private String atlasDbUser;

    @Value("${cb.atlas.database.db:atlas}")
    private String atlasDb;

    @Value("${cb.atlas.database.port:5432}")
    private String atlasDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public DatabaseType getRdsType() {
        return DatabaseType.ATLAS;
    }

    @Override
    public String getDbUser() {
        return atlasDbUser;
    }

    @Override
    public String getDb() {
        return atlasDb;
    }

    @Override
    protected String getDbPort() {
        return atlasDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean knoxGateway) {
        String blueprintText = blueprint.getBlueprintJsonText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return blueprintProcessor.doesCMComponentExistsInBlueprint(ATLAS_SERVER_COMPONENT)
                && blueprintProcessor.doesCMComponentExistsInBlueprint(IDBROKER_COMPONENT)
                && !blueprintProcessor.isServiceTypePresent(HbaseRoles.HBASE)
                && !blueprintProcessor.isServiceTypePresent(HdfsRoles.HDFS);
    }
}
