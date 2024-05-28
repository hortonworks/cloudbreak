package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class CDLStubDfsConfigProvider extends StubDfsConfigProvider {
    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
            return source.getDatalakeView().map(view ->  Crn.ResourceType.INSTANCE.equals(Crn.safeFromString(view.getCrn()).getResourceType())).orElse(false)
                && cmTemplateProcessor.getCmVersion().isPresent()
                && isVersionNewerOrEqualThanLimited(cmTemplateProcessor.getCmVersion().get(), CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_7_1)
                && !cmTemplateProcessor.isRoleTypePresentInService(HDFS, Lists.newArrayList(NAMENODE))
                && source.getFileSystemConfigurationView().isPresent();
    }
}
