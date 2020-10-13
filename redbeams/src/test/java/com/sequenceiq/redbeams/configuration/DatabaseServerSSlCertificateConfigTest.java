package com.sequenceiq.redbeams.configuration;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class DatabaseServerSSlCertificateConfigTest {

    private DatabaseServerSSlCertificateConfig underTest = new DatabaseServerSSlCertificateConfig();

    @Test
    public void testConfigReadWhenForAwsHasOneCertAzureHasTwoCertShouldReturnThreeCert() {
        ReflectionTestUtils.setField(
                underTest,
                "certs",
                Map.of("aws", "aws-cert1", "azure", "azure-cert1;azure-cert2"));

        underTest.setupCertsCache();
        Assert.assertTrue(underTest.getCertsByPlatform(CloudPlatform.AWS.name()).size() == 1);
        Assert.assertTrue(underTest.getCertsByPlatform("aws").size() == 1);
        Assert.assertTrue(underTest.getCertsByPlatform("Aws").size() == 1);

        Assert.assertTrue(underTest.getCertsByPlatform(CloudPlatform.AZURE.name()).size() == 2);
        Assert.assertTrue(underTest.getCertsByPlatform("Azure").size() == 2);
        Assert.assertTrue(underTest.getCertsByPlatform("aZure").size() == 2);

        Assert.assertTrue(underTest.getCertsByPlatform("cloud").size() == 0);
        Assert.assertTrue(underTest.getCertsByPlatform(null).size() == 0);
    }
}