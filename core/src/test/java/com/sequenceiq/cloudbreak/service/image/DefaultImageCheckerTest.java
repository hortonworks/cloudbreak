package com.sequenceiq.cloudbreak.service.image;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.service.CrnsByCategory;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@RunWith(MockitoJUnitRunner.class)
public class DefaultImageCheckerTest {

    private static final String DEFAULT_CRN = "DEFAULT_CRN";

    private static final String OTHER_CRN = "OTHER_CRN";

    @Mock
    private ImageCatalogService imageCatalogService;

    @InjectMocks
    private DefaultImageChecker underTest;

    @Before
    public void setUp() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setResourceCrn(DEFAULT_CRN);
        when(imageCatalogService.getDefaultImageCatalogs()).thenReturn(List.of(imageCatalog));
    }

    @Test
    public void shouldReturnDefaultCrnsWithLegacy() {
        CrnsByCategory result = underTest.getDefaultResourceCrns(List.of(DEFAULT_CRN, OTHER_CRN));

        assertEquals(List.of(DEFAULT_CRN), result.getDefaultResourceCrns());
        assertEquals(List.of(OTHER_CRN), result.getNotDefaultResourceCrns());
    }

}