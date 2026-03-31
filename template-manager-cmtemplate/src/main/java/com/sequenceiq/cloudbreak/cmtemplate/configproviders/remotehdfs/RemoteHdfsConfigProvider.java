package com.sequenceiq.cloudbreak.cmtemplate.configproviders.remotehdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.DFS_NAMENODE_KERBEROS_PRINCIPAL;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.dto.TrustView;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class RemoteHdfsConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteHdfsConfigProvider.class);

    @Inject
    private HdfsConfigHelper hdfsConfigHelper;

    public void populateRemoteHdfsPropertiesForStubDfs(TemplatePreparationObject source, StringBuilder hdfsCoreSiteSafetyValveValue) {
        hdfsCoreSiteSafetyValveValue.append(getNameNodePrincipalSafetyValveProperty(source));
        hdfsCoreSiteSafetyValveValue.append(getRemoteHdfsNameserviceSafetyValveProperties(source));
    }

    private String getNameNodePrincipalSafetyValveProperty(TemplatePreparationObject source) {
        return source.getTrustView()
                .map(TrustView::realm)
                .map(String::toUpperCase)
                .map(trustedRealm -> getSafetyValveProperty(DFS_NAMENODE_KERBEROS_PRINCIPAL, "hdfs/_HOST@" + trustedRealm))
                .orElse("");
    }

    private String getRemoteHdfsNameserviceSafetyValveProperties(TemplatePreparationObject source) {
        RdcView rdcView = source.getDatalakeView().get().getRdcView();
        String datalakeNameService = hdfsConfigHelper.getNameService(rdcView);
        if (StringUtils.isNotBlank(datalakeNameService)) {
            LOGGER.info("Datalake nameservice for HDFS is {}", datalakeNameService);
            return new StringBuilder()
                    .append(getSafetyValveProperty("dfs.nameservices", datalakeNameService))
                    .append(hdfsConfigHelper.getNameServiceConfigSafetyValveValue(rdcView))
                    .toString();
        } else {
            LOGGER.info("Datalake does not have nameservice for HDFS");
            return "";
        }
    }

}
