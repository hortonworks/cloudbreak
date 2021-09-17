package com.sequenceiq.mock.clouderamanager.base;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.clouderamanager.base.batchapi.BatchApiHandler;
import com.sequenceiq.mock.swagger.model.ApiBatchRequest;
import com.sequenceiq.mock.swagger.model.ApiBatchRequestElement;
import com.sequenceiq.mock.swagger.model.ApiBatchResponse;
import com.sequenceiq.mock.swagger.model.ApiBatchResponseElement;

@ExtendWith(MockitoExtension.class)
public class BatchResourceOperationTest {

    public static final String MOCK_UUID = "mockUuid";

    private BatchResourceOperation underTest;

    @Mock
    private ResponseCreatorComponent responseCreatorComponent;

    @Mock
    private BatchApiHandler batchApiHandler;

    @BeforeEach
    public void setup() {
        underTest = new BatchResourceOperation(responseCreatorComponent, List.of(batchApiHandler));
    }

    @Test
    public void testExecuteWhenBodyNull() {
        ArgumentCaptor<ApiBatchResponse> responseArgument = ArgumentCaptor.forClass(ApiBatchResponse.class);
        underTest.execute(MOCK_UUID, null);
        verify(responseCreatorComponent).exec(responseArgument.capture());
        Assertions.assertThat(responseArgument.getValue().isSuccess()).isFalse();
    }

    @Test
    public void testExecuteWhenItemsNull() {
        ApiBatchRequest body = new ApiBatchRequest().items(null);
        ArgumentCaptor<ApiBatchResponse> responseArgument = ArgumentCaptor.forClass(ApiBatchResponse.class);
        underTest.execute(MOCK_UUID, body);
        verify(responseCreatorComponent).exec(responseArgument.capture());
        Assertions.assertThat(responseArgument.getValue().isSuccess()).isFalse();
    }

    @Test
    public void testExecuteWhenItemsEmpty() {
        ApiBatchRequest body = new ApiBatchRequest().items(new ArrayList<>());
        ArgumentCaptor<ApiBatchResponse> responseArgument = ArgumentCaptor.forClass(ApiBatchResponse.class);
        underTest.execute(MOCK_UUID, body);
        verify(responseCreatorComponent).exec(responseArgument.capture());
        Assertions.assertThat(responseArgument.getValue().isSuccess()).isTrue();
    }

    @Test
    public void testExecuteWhenItemsCanProcessAnd200() {
        ApiBatchRequestElement apiBatchRequestElement = new ApiBatchRequestElement();
        ApiBatchRequest body = new ApiBatchRequest().items(List.of(apiBatchRequestElement));

        when(batchApiHandler.canProcess(apiBatchRequestElement)).thenReturn(true);
        when(batchApiHandler.process(MOCK_UUID, apiBatchRequestElement)).thenReturn(new ApiBatchResponseElement().statusCode(BigDecimal.valueOf(200)));

        ArgumentCaptor<ApiBatchResponse> responseArgument = ArgumentCaptor.forClass(ApiBatchResponse.class);
        underTest.execute(MOCK_UUID, body);
        verify(responseCreatorComponent).exec(responseArgument.capture());
        Assertions.assertThat(responseArgument.getValue().isSuccess()).isTrue();
    }

    @Test
    public void testExecuteWhenItemsCanProcessAnd302() {
        ApiBatchRequestElement apiBatchRequestElement = new ApiBatchRequestElement();
        ApiBatchRequest body = new ApiBatchRequest().items(List.of(apiBatchRequestElement));

        when(batchApiHandler.canProcess(apiBatchRequestElement)).thenReturn(true);
        when(batchApiHandler.process(MOCK_UUID, apiBatchRequestElement)).thenReturn(new ApiBatchResponseElement().statusCode(BigDecimal.valueOf(302)));

        ArgumentCaptor<ApiBatchResponse> responseArgument = ArgumentCaptor.forClass(ApiBatchResponse.class);
        underTest.execute(MOCK_UUID, body);
        verify(responseCreatorComponent).exec(responseArgument.capture());
        Assertions.assertThat(responseArgument.getValue().isSuccess()).isTrue();
    }

    @Test
    public void testExecuteWhenItemsCanProcessAnd400() {
        ApiBatchRequestElement apiBatchRequestElement = new ApiBatchRequestElement();
        ApiBatchRequest body = new ApiBatchRequest().items(List.of(apiBatchRequestElement));

        when(batchApiHandler.canProcess(apiBatchRequestElement)).thenReturn(true);
        when(batchApiHandler.process(MOCK_UUID, apiBatchRequestElement)).thenReturn(new ApiBatchResponseElement().statusCode(BigDecimal.valueOf(400)));

        ArgumentCaptor<ApiBatchResponse> responseArgument = ArgumentCaptor.forClass(ApiBatchResponse.class);
        underTest.execute(MOCK_UUID, body);
        verify(responseCreatorComponent).exec(responseArgument.capture());
        Assertions.assertThat(responseArgument.getValue().isSuccess()).isFalse();
    }

    @Test
    public void testExecuteWhenItemsCannotProcess() {
        ApiBatchRequestElement apiBatchRequestElement = new ApiBatchRequestElement();
        ApiBatchRequest body = new ApiBatchRequest().items(List.of(apiBatchRequestElement));

        when(batchApiHandler.canProcess(apiBatchRequestElement)).thenReturn(false);

        ArgumentCaptor<ApiBatchResponse> responseArgument = ArgumentCaptor.forClass(ApiBatchResponse.class);
        underTest.execute(MOCK_UUID, body);
        verify(responseCreatorComponent).exec(responseArgument.capture());
        Assertions.assertThat(responseArgument.getValue().isSuccess()).isFalse();
    }
}
