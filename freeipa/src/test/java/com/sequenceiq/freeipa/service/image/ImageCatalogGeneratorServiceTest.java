package com.sequenceiq.freeipa.service.image;

import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog.GenerateImageCatalogResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ImageCatalogGeneratorServiceTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private StackService stackService;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ImageCatalogGeneratorService underTest;

    @Test
    void generate() {
        Stack stack = new Stack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        ImageCatalog imageCatalog = new ImageCatalog(null, null);
        when(imageService.generateImageCatalogForStack(stack)).thenReturn(imageCatalog);

        GenerateImageCatalogResponse result = underTest.generate(ENVIRONMENT_CRN, ACCOUNT_ID);

        Assertions.assertThat(result.getImageCatalog()).isEqualTo(imageCatalog);
    }

}
