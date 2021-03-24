package com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie.OozieRoleConfigProvider.isOozieHA;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class OozieHAConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String OOZIE_LB_HOST = "oozie_load_balancer";

    private static final String OOZIE_LB_HTTP_PORT = "oozie_load_balancer_http_port";

    private static final String OOZIE_LB_HTTPS_PORT = "oozie_load_balancer_https_port";

    private static final String OOZIE_HTTP_PORT = "11000";

    private static final String OOZIE_HTTPS_PORT = "11443";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        // TODO: DISTX-548 Add an internal load-balancer for Oozie. As a workaround use the first FQDN in the hostgroup as load-balancer.
        return List.of(
            config(OOZIE_LB_HOST, firstOozieFQDN(source)),
            config(OOZIE_LB_HTTP_PORT, OOZIE_HTTP_PORT),
            config(OOZIE_LB_HTTPS_PORT, OOZIE_HTTPS_PORT)
        );
    }

    @Override
    public String getServiceType() {
        return OozieRoles.OOZIE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(OozieRoles.OOZIE_SERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes())
            && isOozieHA(source)
            && firstOozieFQDN(source) != null;
    }

    private static String firstOozieFQDN(TemplatePreparationObject source) {
        return source.getHostGroupsWithComponent(OozieRoles.OOZIE_SERVER)
            .flatMap(hostGroup -> hostGroup.getHosts().stream())
            .findFirst()
            .orElse(null);
    }
}
