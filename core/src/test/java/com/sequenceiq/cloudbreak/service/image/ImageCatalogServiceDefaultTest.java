package com.sequenceiq.cloudbreak.service.image;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.CloudPlarformService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(Parameterized.class)
public class ImageCatalogServiceDefaultTest {
    private static final String[] PROVIDERS = {"aws", "azure", "openstack", "gcp"};

    private final String catalogFile;

    private final String provider;

    private final String expectedImageId;

    private final String cbVersion;

    private final String clusterType;

    private final String clusterVersion;

    private final String os;

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Spy
    private ImageCatalogVersionFilter versionFilter;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private ImageCatalogRepository imageCatalogRepository;

    @Mock
    private CloudPlarformService cloudPlarformService;

    @Mock
    private IdentityUser identityUser;

    @Mock
    private User user;

    @InjectMocks
    private ImageCatalogService underTest;

    public ImageCatalogServiceDefaultTest(String catalogFile, String provider, String clusterType,
            String clusterVersion, String expectedImageId, String cbVersion, String os) {
        this.catalogFile = catalogFile;
        this.provider = provider;
        this.expectedImageId = expectedImageId;
        this.cbVersion = cbVersion;
        this.clusterType = clusterType;
        this.clusterVersion = clusterVersion;
        this.os = os;
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "2.6", "latest-hdp", "5.0.0", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "2.6", "latest-hdp", "5.0.0", "centos7" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "2.6", "latest-amazonlinux-hdp",
                    "5.0.0", "amazonlinux2" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "missing", "third-latest-base-amazonlinux",
                    "5.0.0", "amazonlinux2" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "missing", "forth-latest-base-amazonlinux",
                    "6.0.0", "amazonlinux2" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "missing", "latest-base", "5.0.0", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "missing", "second-latest-base", "6.0.0", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "2.6", "second-latest-hdp", "6.0.0", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "missing", "latest-base", "6.1.0", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "2.6", "second-latest-hdp", "6.1.0", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "missing", "latest-base", "7.0.0-dev.20", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "missing", "second-latest-base", "8.0.0-dev.30", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "missing", "latest-base", "9.0.0", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdp-imagecatalog.json", "aws", "HDP", "2.6", "latest-hdp", "9.0.0", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdf-imagecatalog.json", "aws", "HDF", "2.4", "latest-hdf", "5.0.0", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-hdf-imagecatalog.json", "aws", "HDF", "missing", "latest-base", "5.0.0", "" },
                { "com/sequenceiq/cloudbreak/service/image/default-base-imagecatalog.json", "aws", ImageCatalogService.UNDEFINED,
                        ImageCatalogService.UNDEFINED, "latest-base", "5.0.0", "" }
        });
    }

    @Before
    public void beforeTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        String catalogJson = FileReaderUtils.readFileFromClasspath(catalogFile);
        CloudbreakImageCatalogV2 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV2.class);
        when(imageCatalogProvider.getImageCatalogV2("")).thenReturn(catalog);
        when(cloudPlarformService.enabledPlatforms()).thenReturn(new HashSet<>(Arrays.asList(PROVIDERS)));

        when(userProfileService.getOrCreate(identityUser.getAccount(), identityUser.getUserId(), user)).thenReturn(new UserProfile());
        when(userProfileService.getOrCreate(identityUser.getAccount(), identityUser.getUserId(), identityUser.getUsername(), user))
                .thenReturn(new UserProfile());
    }

    @Test
    public void testGetDefaultImageShouldReturnProperDefaultImage() throws Exception {
        // GIVEN
        ReflectionTestUtils.setField(underTest, "cbVersion", cbVersion);
        ReflectionTestUtils.setField(underTest, "defaultCatalogUrl", "");
        // WHEN
        StatedImage statedImage = underTest.getPrewarmImageDefaultPreferred(provider, clusterType, clusterVersion, os, identityUser, user);
        // THEN
        Assert.assertEquals("Wrong default image has been selected", expectedImageId, statedImage.getImage().getUuid());
    }

    private IdentityUser getIdentityUser() {
        return new IdentityUser(ImageCatalogServiceTest.USER_ID, ImageCatalogServiceTest.USERNAME, ImageCatalogServiceTest.ACCOUNT,
                Collections.emptyList(), "givenName", "familyName", new Date());
    }
}
