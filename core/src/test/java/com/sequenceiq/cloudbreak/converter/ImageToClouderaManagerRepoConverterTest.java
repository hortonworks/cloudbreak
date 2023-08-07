package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;

@ExtendWith(MockitoExtension.class)
public class ImageToClouderaManagerRepoConverterTest {

    @Mock
    private Image image;

    @Test
    public void testConvert() {
        ImageToClouderaManagerRepoConverter converter = new ImageToClouderaManagerRepoConverter();
        // Mock image properties
        Map<ImagePackageVersion, String> packageVersions = new HashMap<>();
        packageVersions.put(ImagePackageVersion.CM, "7.2.16");
        when(image.getPackageVersion(ImagePackageVersion.CM)).thenReturn(packageVersions.get(ImagePackageVersion.CM));

        Map<String, String> repoUrls = new HashMap<>();
        repoUrls.put("centos7", "https://example.com/repo/linux");
        when(image.getRepo()).thenReturn(repoUrls);
        when(image.getOsType()).thenReturn("centos7");
        when(image.getCmBuildNumber()).thenReturn("12345");

        // Perform the conversion
        ClouderaManagerRepo clouderaManagerRepo = converter.convert(image);

        // Verify the conversion results
        assertEquals("7.2.16", clouderaManagerRepo.getVersion());
        assertEquals("https://example.com/repo/linux", clouderaManagerRepo.getBaseUrl());
        assertEquals("https://example.com/repo/linux/RPM-GPG-KEY-cloudera", clouderaManagerRepo.getGpgKeyUrl());
        assertEquals("12345", clouderaManagerRepo.getBuildNumber());
    }
}