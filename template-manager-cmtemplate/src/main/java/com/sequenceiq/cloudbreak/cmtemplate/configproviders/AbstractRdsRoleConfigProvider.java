package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getRdsConfigOfType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

public abstract class AbstractRdsRoleConfigProvider extends AbstractRoleConfigProvider {

    protected abstract DatabaseType dbType();

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getRdsConfig(source) != null
                && super.isConfigurationNeeded(cmTemplateProcessor, source);
    }

    protected RDSConfig getRdsConfig(TemplatePreparationObject source) {
        return getRdsConfigOfType(dbType(), source);
    }

    protected RdsView getRdsView(TemplatePreparationObject source) {
        return new RdsView(getRdsConfig(source));
    }

}
