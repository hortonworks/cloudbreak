package com.sequenceiq.cloudbreak.blueprint.filesystem.adls;

import com.sequenceiq.cloudbreak.api.model.AdlsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemScriptConfig;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateConfigurationEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sequenceiq.cloudbreak.api.model.FileSystemType.ADLS;

@Component
public class AdlsFileSystemConfigurator extends AbstractFileSystemConfigurator<AdlsFileSystemConfiguration> {

    @Override
    public List<TemplateConfigurationEntry> getFsProperties(AdlsFileSystemConfiguration fsConfig, Map<String, String> resourceProperties) {
        List<TemplateConfigurationEntry> bpConfigs = new ArrayList<>();
        String clientId = fsConfig.getClientId();
        String credential = fsConfig.getCredential();
        String tenantId = fsConfig.getTenantId();
        String adlsTrackingClusterType = fsConfig.getProperty(AdlsFileSystemConfiguration.ADLS_TRACKING_CLUSTERTYPE_KEY);
        String adlsTrackingClusterName = fsConfig.getProperty(AdlsFileSystemConfiguration.ADLS_TRACKING_CLUSTERNAME_KEY);


        bpConfigs.add(new TemplateConfigurationEntry("core-site", "dfs.adls.oauth2.access.token.provider.type", "ClientCredential"));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", "dfs.adls.oauth2.client.id", clientId));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", "dfs.adls.oauth2.credential", credential));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", "dfs.adls.oauth2.refresh.url",  "https://login.microsoftonline.com/"
                + tenantId + "/oauth2/token"));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", "fs.AbstractFileSystem.adl.impl", "org.apache.hadoop.fs.adl.Adl"));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", "fs.adl.impl", "org.apache.hadoop.fs.adl.AdlFileSystem"));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", "fs.adls.oauth2.resource", "https://management.core.windows.net/"));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", AdlsFileSystemConfiguration.ADLS_TRACKING_CLUSTERNAME_KEY,
                adlsTrackingClusterName));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", AdlsFileSystemConfiguration.ADLS_TRACKING_CLUSTERTYPE_KEY,
                adlsTrackingClusterType));

        return bpConfigs;
    }

    @Override
    public String getDefaultFsValue(AdlsFileSystemConfiguration fsConfig) {
        return "adl://" + fsConfig.getAccountName() + ".azuredatalakestore.net";
    }

    @Override
    public FileSystemType getFileSystemType() {
        return ADLS;
    }

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs(Credential credential, AdlsFileSystemConfiguration fsConfig) {
        return new ArrayList<>();
    }

}
