package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.ProductDetailsView;

@ExtendWith(MockitoExtension.class)
class S3ExpressBucketValidatorTest {

    @InjectMocks
    private S3ExpressBucketValidator s3ExpressBucketValidator;

    @Mock
    private ClouderaManagerProduct cmp;

    @Mock
    private TemplatePreparationObject source;

    @BeforeEach
    void setUp() {
        doReturn("CDH").when(cmp).getName();
        ClouderaManagerRepo cm = mock(ClouderaManagerRepo.class);
        doReturn(new ProductDetailsView(cm, List.of(cmp))).when(source).getProductDetailsView();
    }

    @Test
    void testValidateVersionForS3ExpressBucket() {
        doReturn("7.2.18").when(cmp).getVersion();
        assertTrue(s3ExpressBucketValidator.validateVersionForS3ExpressBucket(source));
    }

    @Test
    void testValidateVersionForS3ExpressBucketReturnsFalse() {
        doReturn("7.2.17").when(cmp).getVersion();
        assertFalse(s3ExpressBucketValidator.validateVersionForS3ExpressBucket(source));
    }
}
