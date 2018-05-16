package com.sequenceiq.cloudbreak.blueprint.template.views;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.template.views.filesystem.AdlsFileSystemView;
import com.sequenceiq.cloudbreak.blueprint.template.views.filesystem.FileSystemView;
import com.sequenceiq.cloudbreak.blueprint.template.views.filesystem.GcsFileSystemView;
import com.sequenceiq.cloudbreak.blueprint.template.views.filesystem.WasbFileSystemView;

public class FileSystemViewTest {

    @Test
    public void testGcsFileSystemConfiguration() {
        FileSystemConfiguration fileSystemConfiguration = TestUtil.gcsFileSystemConfiguration();
        FileSystemView fileSystemView = new GcsFileSystemView(new FileSystemConfigurationView(fileSystemConfiguration));
        Assert.assertNotNull(fileSystemView);
        Assert.assertEquals(0, fileSystemView.getProperties().size());
    }

    @Test
    public void testAdlsFileSystemConfiguration() {
        FileSystemConfiguration fileSystemConfiguration = TestUtil.adlsFileSystemConfiguration();
        FileSystemView fileSystemView = new AdlsFileSystemView(new FileSystemConfigurationView(fileSystemConfiguration));
        Assert.assertNotNull(fileSystemView);
        Assert.assertEquals(2, fileSystemView.getProperties().size());
    }

    @Test
    public void testWasbFileSystemConfiguration() {
        FileSystemConfiguration fileSystemConfiguration = TestUtil.wasbSecureFileSystemConfiguration();
        FileSystemView fileSystemView = new WasbFileSystemView(new FileSystemConfigurationView(fileSystemConfiguration));
        Assert.assertNotNull(fileSystemView);
        Assert.assertEquals(1, fileSystemView.getProperties().size());
    }
}