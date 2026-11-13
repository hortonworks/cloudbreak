package com.sequenceiq.datalake.service.sdx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@ExtendWith(MockitoExtension.class)
public class SdxServiceNotificationStateTest {

    @InjectMocks
    private SdxService underTest;

    private SdxClusterRequest clusterRequest;

    private SdxCluster sdxCluster;

    @BeforeEach
    void setUp() {
        clusterRequest = new SdxClusterRequest();
        sdxCluster = new SdxCluster();
    }

    @Test
    void notificationIsDisabledWhenTheRequestDoesNotAskForIt() {
        assertThat(underTest.getNotificationState(clusterRequest)).isEqualTo(NotificationState.DISABLED);
    }

    @Test
    void explicitlyRequestedNotificationStateIsKept() {
        clusterRequest.setNotificationState(NotificationState.ENABLED);

        assertThat(underTest.getNotificationState(clusterRequest)).isEqualTo(NotificationState.ENABLED);
    }

    @Test
    void notificationStateIsPropagatedToTheInternalStackRequestWhenItHasNone() {
        StackV4Request internalStackV4Request = new StackV4Request();

        underTest.setNotificationState(internalStackV4Request, sdxCluster, NotificationState.DISABLED);

        assertThat(sdxCluster.getNotificationState()).isEqualTo(NotificationState.DISABLED);
        assertThat(internalStackV4Request.getNotificationState()).isEqualTo(NotificationState.DISABLED);
    }

    @Test
    void matchingNotificationStatesAreAccepted() {
        StackV4Request internalStackV4Request = new StackV4Request();
        internalStackV4Request.setNotificationState(NotificationState.ENABLED);

        assertThatCode(() -> underTest.setNotificationState(internalStackV4Request, sdxCluster, NotificationState.ENABLED))
                .doesNotThrowAnyException();
        assertThat(sdxCluster.getNotificationState()).isEqualTo(NotificationState.ENABLED);
    }

    @Test
    void conflictingNotificationStatesAreRejected() {
        StackV4Request internalStackV4Request = new StackV4Request();
        internalStackV4Request.setNotificationState(NotificationState.ENABLED);

        assertThatThrownBy(() -> underTest.setNotificationState(internalStackV4Request, sdxCluster, NotificationState.DISABLED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The request contains DISABLED notificationState but the internal stack request contains ENABLED notificationState.");
    }

    @Test
    void notificationStateIsSetOnTheClusterWithoutAnInternalStackRequest() {
        underTest.setNotificationState(null, sdxCluster, NotificationState.DISABLED);

        assertThat(sdxCluster.getNotificationState()).isEqualTo(NotificationState.DISABLED);
    }
}
