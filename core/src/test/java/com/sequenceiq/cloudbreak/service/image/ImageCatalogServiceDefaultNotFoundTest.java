package com.sequenceiq.cloudbreak.service.image;

import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogServiceDefaultNotFoundTest {
    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private ImageCatalogRepository imageCatalogRepository;

    @InjectMocks
    private ImageCatalogService underTest;

    @Before
    public void beforeTest() throws Exception {
        String catalogJson = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/service/image/no-default-imagecatalog.json");
        CloudbreakImageCatalogV2 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV2.class);
        when(imageCatalogProvider.getImageCatalogV2("")).thenReturn(catalog);

        IdentityUser user = getIdentityUser();
        when(authenticatedUserService.getCbUser()).thenReturn(user);

        when(userProfileService.get(user.getAccount(), user.getUserId(), user.getUsername())).thenReturn(new UserProfile());
    }

    @Test(expected = CloudbreakImageNotFoundException.class)
    public void testGetDefaultImageShouldThrowNotFoundException() throws Exception {
        // GIVEN
        ReflectionTestUtils.setField(underTest, "cbVersion", "5.0.0");
        ReflectionTestUtils.setField(underTest, "defaultCatalogUrl", "");
        // WHEN
        underTest.getDefaultImage("gcp", "notimportant", "notimportant");
        // THEN throw CloudbreakImageNotFoundException
    }

    private IdentityUser getIdentityUser() {
        return new IdentityUser(ImageCatalogServiceTest.USER_ID, ImageCatalogServiceTest.USERNAME, ImageCatalogServiceTest.ACCOUNT,
                Collections.emptyList(), "givenName", "familyName", new Date());
    }
}
