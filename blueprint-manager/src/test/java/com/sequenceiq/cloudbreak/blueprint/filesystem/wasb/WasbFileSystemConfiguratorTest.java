package com.sequenceiq.cloudbreak.blueprint.filesystem.wasb;

import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemScriptConfig;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplateConfigurationEntry;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

public class WasbFileSystemConfiguratorTest {

    private final WasbFileSystemConfigurator underTest = new WasbFileSystemConfigurator();

    @Test
    public void testGetFsProperties() {
        WasbFileSystemConfiguration config = new WasbFileSystemConfiguration();
        config.setAccountName("accountName");
        config.setAccountKey("accountKey");

        List<TemplateConfigurationEntry> actual = underTest.getFsProperties(config, null);

        List<TemplateConfigurationEntry> expected = Arrays.asList(
                new TemplateConfigurationEntry("core-site", "fs.AbstractFileSystem.wasbs.impl", "org.apache.hadoop.fs.azure.Wasbs"),
                new TemplateConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"),
                new TemplateConfigurationEntry("core-site", "fs.azure.account.key.accountName.blob.core.windows.net", "accountKey"),
                new TemplateConfigurationEntry("core-site", "fs.azure.selfthrottling.read.factor", "1.0"),
                new TemplateConfigurationEntry("core-site", "fs.azure.selfthrottling.write.factor", "1.0"));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetDefaultFsValueWhenSecure() {
        WasbFileSystemConfiguration config = new WasbFileSystemConfiguration();
        config.setSecure(true);
        config.setAccountName("accountName");
        config.addProperty("container", "container");

        String actual = underTest.getDefaultFsValue(config);

        String expected = "wasbs://container@accountName.blob.core.windows.net";

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetDefaultFsValueWhenNotSecure() {
        WasbFileSystemConfiguration config = new WasbFileSystemConfiguration();
        config.setSecure(false);
        config.setAccountName("accountName");
        config.addProperty("container", "container");

        String actual = underTest.getDefaultFsValue(config);

        String expected = "wasb://container@accountName.blob.core.windows.net";

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetScriptConfigs() {
        List<FileSystemScriptConfig> actual = underTest.getScriptConfigs(new Credential(), new WasbFileSystemConfiguration());

        Assert.assertEquals(emptyList(), actual);
    }
}
