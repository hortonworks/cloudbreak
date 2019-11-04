package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateConfigInjector;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class ProcessAutoRestartInjector implements CmTemplateConfigInjector {

    private static final Set<String> SERVICES = Set.of(
            "ATLAS",
            "HBASE",
            HdfsRoles.HDFS,
            HiveRoles.HIVE,
            HiveRoles.HIVE_ON_TEZ,
            "KAFKA",
            KnoxRoles.KNOX,
            "LIVY",
            RangerRoles.RANGER,
            "SOLR",
            "SPARK_ON_YARN",
            YarnRoles.YARN,
            "ZEPPELIN"
    );

    private static final Set<String> IGNORED_ROLES = Set.of(
            "GATEWAY", HdfsRoles.BALANCER
    );

    private final ApiClusterTemplateConfig autoRestartConfig = config("process_auto_restart", Boolean.TRUE.toString());

    private final List<ApiClusterTemplateConfig> configs = List.of(autoRestartConfig);

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(
            ApiClusterTemplateRoleConfigGroup roleConfigGroup,
            ApiClusterTemplateService service,
            TemplatePreparationObject source
    ) {
        return shouldAutoRestart(service.getServiceType(), roleConfigGroup.getRoleType()) ? configs : List.of();
    }

    private boolean shouldAutoRestart(String service, String roleType) {
        return SERVICES.contains(service) && !IGNORED_ROLES.contains(roleType);
    }

}
