package com.sequenceiq.cloudbreak.templateprocessor.template.views;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;

public class FileSystemViewTest {

    @Test
    public void testGcsFileSystemConfiguration() {
        FileSystemConfiguration fileSystemConfiguration = TestUtil.gcsFileSystemConfiguration();
        FileSystemView fileSystemView = new FileSystemView(fileSystemConfiguration);
        Assert.assertNotNull(fileSystemView);
        Assert.assertEquals(3, fileSystemView.getProperties().size());
    }

    @Test
    public void testAdlsFileSystemConfiguration() {
        FileSystemConfiguration fileSystemConfiguration = TestUtil.adlsFileSystemConfiguration();
        FileSystemView fileSystemView = new FileSystemView(fileSystemConfiguration);
        Assert.assertNotNull(fileSystemView);
        Assert.assertEquals(4, fileSystemView.getProperties().size());
    }

    @Test
    public void testWasbFileSystemConfiguration() {
        FileSystemConfiguration fileSystemConfiguration = TestUtil.wasbFileSystemConfiguration();
        FileSystemView fileSystemView = new FileSystemView(fileSystemConfiguration);
        Assert.assertNotNull(fileSystemView);
        Assert.assertEquals(2, fileSystemView.getProperties().size());
    }

    @Test
    public void testWasbIntegratedFileSystemConfiguration() {
        FileSystemConfiguration fileSystemConfiguration = TestUtil.wasbIntegratedFileSystemConfiguration();
        FileSystemView fileSystemView = new FileSystemView(fileSystemConfiguration);
        Assert.assertNotNull(fileSystemView);
        Assert.assertEquals(6, fileSystemView.getProperties().size());
    }
}