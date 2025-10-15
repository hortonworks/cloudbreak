package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxService.WORKSPACE_ID_DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.imagecatalog.GenerateImageCatalogV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
class SdxImageCatalogServiceTest {

    private static final Long STACK_ID = 123L;

    private static final String CLUSTER_NAME = "cluster-name";

    private static final String IMAGE_CATALOG = "image-catalog";

    private static final SdxCluster SDX_CLUSTER = new SdxCluster();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:username";

    private static final String EXCEPTION_MESSAGE = "exception message";

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @InjectMocks
    private SdxImageCatalogService underTest;

    @Captor
    private ArgumentCaptor<ChangeImageCatalogV4Request> requestCaptor;

    @BeforeAll
    static void init() {
        SDX_CLUSTER.setId(STACK_ID);
        SDX_CLUSTER.setClusterName(CLUSTER_NAME);
    }

    @Test
    void shouldFailWhenFlowsAreRunning() {
        when(flowLogService.isOtherFlowRunning(STACK_ID)).thenReturn(true);

        assertThatThrownBy(() -> underTest.changeImageCatalog(SDX_CLUSTER, IMAGE_CATALOG))
                .isInstanceOf(FlowsAlreadyRunningException.class)
                .hasMessage("Operation is running for cluster 'cluster-name'. Please try again later.");
    }

    @Test
    void shouldConvertException() {
        doThrow(CloudbreakServiceException.class).when(stackV4Endpoint)
                .changeImageCatalogInternal(eq(WORKSPACE_ID_DEFAULT), eq(CLUSTER_NAME), any(), any());

        assertThatThrownBy(() -> underTest.changeImageCatalog(SDX_CLUSTER, IMAGE_CATALOG))
                .isInstanceOf(CloudbreakServiceException.class);
    }

    @Test
    void shouldCallChangeImageWhenEverythingIsValid() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.changeImageCatalog(SDX_CLUSTER, IMAGE_CATALOG));

        verify(stackV4Endpoint).changeImageCatalogInternal(eq(WORKSPACE_ID_DEFAULT), eq(CLUSTER_NAME), eq(USER_CRN), requestCaptor.capture());
        assertThat(requestCaptor.getValue())
                .returns(IMAGE_CATALOG, ChangeImageCatalogV4Request::getImageCatalog);
    }

    @Test
    void shouldCallGenererateImageCatalog() {
        CloudbreakImageCatalogV3 imageCatalogV3 = mock(CloudbreakImageCatalogV3.class);
        GenerateImageCatalogV4Response response = new GenerateImageCatalogV4Response(imageCatalogV3);
        when(stackV4Endpoint.generateImageCatalogInternal(WORKSPACE_ID_DEFAULT, CLUSTER_NAME, USER_CRN)).thenReturn(response);
        CloudbreakImageCatalogV3 actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateImageCatalog(CLUSTER_NAME));

        assertEquals(imageCatalogV3, actual);
    }

    @Test
    void generateImageCatalogShouldThrowApiExceptionInCaseOfServiceException() {
        when(stackV4Endpoint.generateImageCatalogInternal(WORKSPACE_ID_DEFAULT, CLUSTER_NAME, USER_CRN))
                .thenThrow(new CloudbreakServiceException(EXCEPTION_MESSAGE));
        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateImageCatalog(CLUSTER_NAME)))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessage(EXCEPTION_MESSAGE);
    }
}
