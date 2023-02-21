package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.http.rest.PagedIterable;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandlerParameters;

@ExtendWith(MockitoExtension.class)
public class AzureListResultTest {

    @Mock
    private AzureExceptionHandler azureExceptionHandler;

    @Mock
    private PagedIterable<String> pagedIterable;

    @Test
    void testWhenGetStreamThenExceptionHandlerIsCalled() {
        AzureListResult<String> azureListResult = new AzureListResult<>(pagedIterable, azureExceptionHandler);

        azureListResult.getStream();

        verify(azureExceptionHandler).handleException(any(), any(Stream.class));
    }

    @Test
    void testWhenGetStreamWithExceptionHandlerParametersThenExceptionHandlerWithParametersIsCalled() {
        AzureListResult<String> azureListResult = new AzureListResult<>(pagedIterable, azureExceptionHandler);
        AzureExceptionHandlerParameters azureExceptionHandlerParameters = AzureExceptionHandlerParameters.builder().build();

        azureListResult.getStream(azureExceptionHandlerParameters);

        verify(azureExceptionHandler).handleException(any(), any(Stream.class), eq(azureExceptionHandlerParameters));
    }

    @Test
    void testGetAll() {
        AzureListResult<String> azureListResult = new AzureListResult<>(pagedIterable, azureExceptionHandler);
        when(azureExceptionHandler.handleException(any(), any(Stream.class))).thenReturn(Stream.of());

        azureListResult.getAll();

        verify(azureExceptionHandler).handleException(any(), any(Stream.class));
    }

}
