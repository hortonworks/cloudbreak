package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getRdsViewOfType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

public abstract class AbstractRdsRoleConfigProvider extends AbstractRoleConfigProvider {

    public abstract DatabaseType dbType();

    public abstract String dbUserKey();

    public abstract String dbPasswordKey();

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getRdsView(source) != null
                && super.isConfigurationNeeded(cmTemplateProcessor, source);
    }

    protected RdsView getRdsView(TemplatePreparationObject source) {
        RdsView rdsViewOfType = getRdsViewOfType(dbType(), source);
        if (rdsViewOfType != null) {
            rdsViewOfType.setSslCertificateFilePath(source.getRdsSslCertificateFilePath());
        }
        return rdsViewOfType;
    }
}
