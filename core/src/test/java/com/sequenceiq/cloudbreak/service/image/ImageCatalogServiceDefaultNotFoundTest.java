package com.sequenceiq.cloudbreak.service.image;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.CloudPlarformService;
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
    private CloudPlarformService cloudPlarformService;

    @Mock
    private User user;

    @InjectMocks
    private ImageCatalogService underTest;

    private IdentityUser identityUser;

    @Before
    public void beforeTest() throws Exception {
        String catalogJson = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/service/image/no-default-imagecatalog.json");
        CloudbreakImageCatalogV2 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV2.class);
        identityUser = getIdentityUser();
        when(imageCatalogProvider.getImageCatalogV2("")).thenReturn(catalog);
        when(cloudPlarformService.enabledPlatforms()).thenReturn(new HashSet<>(Arrays.asList(PROVIDERS)));
        when(userProfileService.getOrCreate(identityUser.getAccount(), identityUser.getUserId(), user)).thenReturn(new UserProfile());
    }

    @Test(expected = CloudbreakImageNotFoundException.class)
    public void testGetDefaultImageShouldThrowNotFoundException() throws Exception {
        // GIVEN
        ReflectionTestUtils.setField(underTest, "cbVersion", "5.0.0");
        ReflectionTestUtils.setField(underTest, "defaultCatalogUrl", "");
        // WHEN
        underTest.getPrewarmImageDefaultPreferred("gcp", "notimportant", "notimportant", null, identityUser, user);
        // THEN throw CloudbreakImageNotFoundException
    }

    private IdentityUser getIdentityUser() {
        return new IdentityUser(ImageCatalogServiceTest.USER_ID, ImageCatalogServiceTest.USERNAME, ImageCatalogServiceTest.ACCOUNT,
                Collections.emptyList(), "givenName", "familyName", new Date());
    }
}
