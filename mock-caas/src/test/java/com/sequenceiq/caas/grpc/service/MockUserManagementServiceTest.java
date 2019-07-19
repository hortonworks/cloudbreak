package com.sequenceiq.caas.grpc.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.caas.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class MockUserManagementServiceTest {

    public static final String VALID_LICENSE = "License file content";

    public static final String EMPTY_LICENSE = "";

    @InjectMocks
    private MockUserManagementService underTest;

    @Mock
    private JsonUtil jsonUtil;

    @Test
    public void testSetLicenseShouldReturnACloudbreakLicense() throws IOException {
        Path licenseFilePath = Files.createTempFile("license", "txt");
        Files.writeString(licenseFilePath, VALID_LICENSE);
        ReflectionTestUtils.setField(underTest, "cbLicenseFilePath", licenseFilePath.toString());
        underTest.init();

        String actual = ReflectionTestUtils.getField(underTest, "cbLicense").toString();

        Assert.assertEquals(VALID_LICENSE, actual);
        Files.delete(licenseFilePath);
    }

    @Test
    public void testSetLicenseShouldEmptyStringWhenTheFileIsNotExists() {
        ReflectionTestUtils.setField(underTest, "cbLicenseFilePath", "/etc/license");
        underTest.init();

        String actual = ReflectionTestUtils.getField(underTest, "cbLicense").toString();

        Assert.assertEquals(EMPTY_LICENSE, actual);
    }

    @Test
    public void testCreateWorkloadUsername() {
        String username = "&*foO$_#Bar22@baz13.com";
        String expected = "foo_bar22";

        Assert.assertEquals(expected, underTest.sanitizeWorkloadUsername(username));
    }
}