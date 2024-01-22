package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelState;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

@ExtendWith(MockitoExtension.class)
abstract class AbstractClouderaManagerParcelListenerTaskTest<T extends AbstractClouderaManagerParcelListenerTask> {

    static final String CLUSTER_NAME = "cluster-name";

    static final String PRODUCT = "product";

    static final String VERSION = "version";

    @Mock
    ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Mock
    ClusterEventService clusterEventService;

    @Mock
    ParcelResourceApi parcelResourceApi;

    T underTest;

    @Mock
    ClouderaManagerCommandPollerObject pollerObject;

    @Mock
    ApiClient apiClient;

    @Mock
    ApiParcel apiParcel;

    @Mock
    ApiParcelState apiParcelState;

    abstract T setUpUnderTest();

    @BeforeEach
    void setUp() throws Exception {
        underTest = setUpUnderTest();
        when(pollerObject.getApiClient()).thenReturn(apiClient);
        when(clouderaManagerApiPojoFactory.getParcelResourceApi(apiClient)).thenReturn(parcelResourceApi);
        when(parcelResourceApi.readParcel(anyString(), anyString(), anyString())).thenReturn(apiParcel);
        lenient().when(apiParcel.getState()).thenReturn(apiParcelState);
        lenient().when(apiParcelState.getErrors()).thenReturn(List.of());
    }

    @Test
    void shouldHandleParcelStateAndStageNull() {
        when(apiParcel.getStage()).thenReturn(null);
        when(apiParcel.getState()).thenReturn(null);

        assertThatCode(() -> underTest.doStatusCheck(pollerObject)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowExceptionWhenApiParcelStateHasError() {
        when(apiParcelState.getErrors()).thenReturn(List.of("error1", "error2"));

        assertThatThrownBy(() -> underTest.doStatusCheck(pollerObject))
                .isInstanceOf(ClouderaManagerOperationFailedException.class)
                .hasMessage("Command [%s] failed: error1; error2", underTest.getCommandName());
    }

    @ParameterizedTest
    @EnumSource(ParcelStatus.class)
    void doStatusCheck(ParcelStatus parcelStatus) throws ApiException {
        when(apiParcel.getStage()).thenReturn(parcelStatus.name());

        boolean result = underTest.doStatusCheck(pollerObject);

        boolean expectedStatusCheckResult = underTest.getExpectedParcelStatuses().contains(parcelStatus);
        assertThat(result).isEqualTo(expectedStatusCheckResult);
    }

}
