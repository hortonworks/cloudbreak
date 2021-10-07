package com.sequenceiq.freeipa.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsBase;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ImageCatalogChangeServiceTest {

    private static final String ACCOUNT_ID = "account-id";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:cloudera:environment:ded616a1-799b-42ec-9b8b-510a829b901d";

    private static final String IMAGE_CATALOG = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-dev-freeipa-image-catalog.json";

    private static final Long STACK_ID = 123L;

    private static final String IMAGE_ID = "image-id";

    private static final String OS = "centos7";

    private static final String STACK_NAME = "stack-name";

    private static final Stack STACK = new Stack();

    @Mock
    private StackService stackService;

    @Mock
    private ImageService imageService;

    @Mock
    private FlowLogService flowLogService;

    @InjectMocks
    private ImageCatalogChangeService underTest;

    @Captor
    private ArgumentCaptor<ImageSettingsRequest> imageSettingsCaptor;

    @BeforeAll
    static void init() {
        STACK.setId(STACK_ID);
        STACK.setName(STACK_NAME);
        final ImageEntity image = new ImageEntity();
        image.setImageId(IMAGE_ID);
        image.setOs(OS);
        STACK.setImage(image);
    }

    @BeforeEach
    void setUp() {
        lenient().when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(STACK);
    }

    @Test
    void shouldFailWhenStackNotFound() {
        when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenThrow(NotFoundException.class);

        assertThatThrownBy(() -> underTest.changeImageCatalog(ENVIRONMENT_CRN, ACCOUNT_ID, IMAGE_CATALOG))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldFailWhenFlowsAreRunning() {
        when(flowLogService.isOtherFlowRunning(STACK_ID)).thenReturn(true);

        assertThatThrownBy(() -> underTest.changeImageCatalog(ENVIRONMENT_CRN, ACCOUNT_ID, IMAGE_CATALOG))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessage("Operation is running for stack 'stack-name'. Please try again later.");
    }

    @Test
    void shouldFailWhenImageIsNotPresentInNewCatalog() {
        when(imageService.changeImage(eq(STACK), any())).thenThrow(ImageNotFoundException.class);

        assertThatThrownBy(() -> underTest.changeImageCatalog(ENVIRONMENT_CRN, ACCOUNT_ID, IMAGE_CATALOG))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessage("Could not find current image in new catalog");
    }

    @Test
    void shouldCallChangeImageWhenEverythingIsValid() {
        underTest.changeImageCatalog(ENVIRONMENT_CRN, ACCOUNT_ID, IMAGE_CATALOG);

        verify(imageService).changeImage(eq(STACK), imageSettingsCaptor.capture());
        assertThat(imageSettingsCaptor.getValue())
                .returns(IMAGE_CATALOG, ImageSettingsBase::getCatalog)
                .returns(IMAGE_ID, ImageSettingsBase::getId)
                .returns(OS, ImageSettingsBase::getOs);
    }
}
