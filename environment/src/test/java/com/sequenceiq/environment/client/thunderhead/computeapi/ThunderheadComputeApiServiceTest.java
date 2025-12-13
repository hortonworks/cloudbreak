package com.sequenceiq.environment.client.thunderhead.computeapi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderheadcompute.api.DefaultApi;
import com.cloudera.thunderheadcompute.model.DescribeCustomConfigResponse;
import com.sequenceiq.environment.environment.dto.dataservices.CustomDockerRegistryParameters;
import com.sequenceiq.thunderheadcompute.ApiException;

@ExtendWith(MockitoExtension.class)
class ThunderheadComputeApiServiceTest {

    @Mock
    private ThunderheadComputeApiClientFactory computeApiClientFactory;

    @InjectMocks
    private ThunderheadComputeApiService underTest;

    @Test
    void customConfigDescribableWhenDockerConfigIsNull() {
        assertThrows(NullPointerException.class,
                () -> underTest.customConfigDescribable(null),
                "customDockerRegistryParameters must not be null.");
    }

    @Test
    void customConfigDescribableWhenDockerConfigIsNotNull() throws ApiException {
        String dummyCrn = "dummyCrn";
        CustomDockerRegistryParameters customDockerRegistryParameters = CustomDockerRegistryParameters.builder()
                .withCrn(dummyCrn)
                .build();
        DefaultApi defaultApi = mock(DefaultApi.class);
        when(computeApiClientFactory.create()).thenReturn(defaultApi);
        DescribeCustomConfigResponse describeCustomConfigResponse = mock(DescribeCustomConfigResponse.class);
        when(defaultApi.describeCustomConfig(any())).thenReturn(describeCustomConfigResponse);
        when(describeCustomConfigResponse.getCrn()).thenReturn(dummyCrn);

        boolean actual = underTest.customConfigDescribable(customDockerRegistryParameters);

        assertTrue(actual);
    }

    @Test
    void customConfigDescribableWhenComputeApiCallFails() throws ApiException {
        String dummyCrn = "dummyCrn";
        CustomDockerRegistryParameters customDockerRegistryParameters = CustomDockerRegistryParameters.builder()
                .withCrn(dummyCrn)
                .build();
        DefaultApi defaultApi = mock(DefaultApi.class);
        when(computeApiClientFactory.create()).thenReturn(defaultApi);
        when(defaultApi.describeCustomConfig(any())).thenThrow(new ApiException("uh-oh"));

        boolean actual = underTest.customConfigDescribable(customDockerRegistryParameters);

        assertFalse(actual);
    }
}