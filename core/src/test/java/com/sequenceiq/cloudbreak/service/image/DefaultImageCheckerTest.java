package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.defaults.CrnsByCategory;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@ExtendWith(MockitoExtension.class)
class DefaultImageCheckerTest {

    private static final String DEFAULT_CRN = "DEFAULT_CRN";

    private static final String OTHER_CRN = "OTHER_CRN";

    @Mock
    private ImageCatalogService imageCatalogService;

    @InjectMocks
    private DefaultImageChecker underTest;

    @BeforeEach
    void setUp() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setResourceCrn(DEFAULT_CRN);
        when(imageCatalogService.getDefaultImageCatalogs()).thenReturn(List.of(imageCatalog));
    }

    @Test
    void shouldReturnDefaultCrnsWithLegacy() {
        CrnsByCategory result = underTest.getDefaultResourceCrns(List.of(DEFAULT_CRN, OTHER_CRN));

        assertEquals(List.of(DEFAULT_CRN), result.getDefaultResourceCrns());
        assertEquals(List.of(OTHER_CRN), result.getNotDefaultResourceCrns());
    }

}