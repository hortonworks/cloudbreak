package com.sequenceiq.cloudbreak.service.image;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogServiceDefaultNotFoundTest {
    private static final String[] PROVIDERS = {"aws", "azure", "openstack", "gcp"};

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Mock
    private UserProfileService userProfileService;

    @Spy
    private ImageCatalogVersionFilter versionFilter;

    @Mock
    private ImageCatalogRepository imageCatalogRepository;

    @Mock
    private PreferencesService preferencesService;

    @Mock
    private User user;

    @InjectMocks
    private ImageCatalogService underTest;

    private CloudbreakUser cloudbreakUser;

    @Before
    public void beforeTest() throws Exception {
        String catalogJson = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/service/image/no-default-imagecatalog.json");
        CloudbreakImageCatalogV2 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV2.class);
        cloudbreakUser = getCloudbreakUser();
        when(imageCatalogProvider.getImageCatalogV2("")).thenReturn(catalog);
        when(preferencesService.enabledPlatforms()).thenReturn(new HashSet<>(Arrays.asList(PROVIDERS)));
        when(userProfileService.getOrCreate(user)).thenReturn(new UserProfile());
    }

    @Test(expected = CloudbreakImageNotFoundException.class)
    public void testGetDefaultImageShouldThrowNotFoundException() throws Exception {
        // GIVEN
        ReflectionTestUtils.setField(underTest, "cbVersion", "5.0.0");
        ReflectionTestUtils.setField(underTest, "defaultCatalogUrl", "");
        // WHEN
        underTest.getPrewarmImageDefaultPreferred("gcp", "notimportant", "notimportant", null, cloudbreakUser, user);
        // THEN throw CloudbreakImageNotFoundException
    }

    private CloudbreakUser getCloudbreakUser() {
        return new CloudbreakUser(ImageCatalogServiceTest.USER_ID, ImageCatalogServiceTest.USERNAME, ImageCatalogServiceTest.ACCOUNT
        );
    }
}
