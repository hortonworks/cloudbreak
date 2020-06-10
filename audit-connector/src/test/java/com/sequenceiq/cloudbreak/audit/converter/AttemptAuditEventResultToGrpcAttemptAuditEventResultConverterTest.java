package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.converter.builder.AttemptAuditEventResultBuilderProvider;
import com.sequenceiq.cloudbreak.audit.model.AttemptAuditEventResult;
import com.sequenceiq.cloudbreak.audit.model.ResultApiRequestData;
import com.sequenceiq.cloudbreak.audit.model.ResultEventData;
import com.sequenceiq.cloudbreak.audit.model.ResultServiceEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttemptAuditEventResultToGrpcAttemptAuditEventResultConverterTest {

    private static final String UUID_ID = "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String ENV1_CRN = "crn:cdp:environments:us-west-1:accountId:environment:ac5ba74b-c35e-45e9-9f47-123456789876";

    private static final String ENV2_CRN = "crn:cdp:environments:us-west-1:accountId:environment:bc5ba74b-c35e-45e9-9f47-123456789876";

    private static final List<String> CRNS = List.of(ENV1_CRN, ENV2_CRN);

    private static final String REQUEST_ID = "requestId";

    private static final String RESULT_CODE = "resultCode";

    private static final String RESULT_DETAILS = "{\"key\":\"value\"}";

    private static final String RESULT_MESSAGE = "message";

    private static final String RESPONSE_PARAMETERS = "responseParameters";

    private AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter underTest;

    private AttemptAuditEventResultBuilderUpdater mockAttemptAuditEventResultBuilderUpdater;

    private AttemptAuditEventResultBuilderProvider builderProvider;

    private AuditProto.AttemptAuditEventResult.Builder mockBuilder;

    private AuditProto.AttemptAuditEventResult result;

    @BeforeEach
    void setUp() {
        builderProvider = mock(AttemptAuditEventResultBuilderProvider.class);
        mockBuilder = mock(AuditProto.AttemptAuditEventResult.Builder.class);
        result = mock(AuditProto.AttemptAuditEventResult.class);
        when(mockBuilder.build()).thenReturn(result);
        when(builderProvider.getNewAttemptAuditEventResultBuilder()).thenReturn(mockBuilder);
        when(mockBuilder.setId(any())).thenReturn(mockBuilder);
        when(mockBuilder.setResultCode(any())).thenReturn(mockBuilder);
        underTest = new AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter(new LinkedHashMap<>(), builderProvider);
        mockAttemptAuditEventResultBuilderUpdater = mock(AttemptAuditEventResultBuilderUpdater.class);
    }

    @Test
    void testPreventPossibleNullValuesNoResult() {
        AttemptAuditEventResult source = makeMinimalAttemptAuditEventResult(null);
        underTest.convert(source);
    }

    @Test
    void testPreventPossibleNullValuesServiceApiRequestResult() {
        ResultEventData resultServiceEventData = ResultServiceEventData.builder()
                .withResourceCrns(CRNS)
                .build();
        AttemptAuditEventResult source = makeMinimalAttemptAuditEventResult(resultServiceEventData);
        underTest = new AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter(createMockUtilizer(ResultServiceEventData.class), builderProvider);

        underTest.convert(source);
        verify(mockAttemptAuditEventResultBuilderUpdater, times(1)).update(any(), any());
    }

    @Test
    void testWhenResultEventDataIsNullThenNoUtilizerCallHappens() {
        AttemptAuditEventResult source = makeMinimalAttemptAuditEventResult(null);

        underTest.convert(source);
        verify(mockAttemptAuditEventResultBuilderUpdater, never()).update(any(), any());
    }

    @Test
    void convertWithoutResultEventData() {
        AttemptAuditEventResult source = makeAttemptAuditEventResult(null);

        AuditProto.AttemptAuditEventResult target = underTest.convert(source);
        assertEquals(target, result);
        verify(mockBuilder, times(1)).build();
        verify(builderProvider, times(1)).getNewAttemptAuditEventResultBuilder();
        verify(mockAttemptAuditEventResultBuilderUpdater, never()).update(any(), any());
    }

    @Test
    void convertWithResultServiceEventData() {
        ResultServiceEventData rsed = ResultServiceEventData.builder()
                .withResourceCrns(CRNS)
                .withResultDetails(RESULT_DETAILS)
                .build();
        AttemptAuditEventResult source = makeAttemptAuditEventResult(rsed);
        underTest = new AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter(createMockUtilizer(ResultServiceEventData.class), builderProvider);

        AuditProto.AttemptAuditEventResult target = underTest.convert(source);
        assertEquals(target, result);
        verify(mockBuilder, times(1)).build();
        verify(builderProvider, times(1)).getNewAttemptAuditEventResultBuilder();
        verify(mockAttemptAuditEventResultBuilderUpdater, times(1)).update(any(), any());
    }

    @Test
    void convertWithResultApiRequestData() {
        ResultApiRequestData rsed = ResultApiRequestData.builder().withResponseParameters(RESPONSE_PARAMETERS).build();
        AttemptAuditEventResult source = makeAttemptAuditEventResult(rsed);
        underTest = new AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter(createMockUtilizer(ResultApiRequestData.class), builderProvider);

        AuditProto.AttemptAuditEventResult target = underTest.convert(source);

        assertEquals(target, result);
        verify(mockBuilder, times(1)).build();
        verify(builderProvider, times(1)).getNewAttemptAuditEventResultBuilder();
        verify(mockAttemptAuditEventResultBuilderUpdater, times(1)).update(any(), any());
    }

    @Test
    void convertWithUnknownResultEventDataThrows() {

        class Unknown extends ResultEventData {
        }

        AttemptAuditEventResult source = makeAttemptAuditEventResult(new Unknown());

        assertThatThrownBy(() -> underTest.convert(source)).isInstanceOf(IllegalArgumentException.class);
    }

    private void assertGeneric(AuditProto.AttemptAuditEventResult target) {
        assertThat(target.getId()).isEqualTo(UUID_ID);
        assertThat(target.getResultCode()).isEqualTo(RESULT_CODE);
        assertThat(target.getResultMessage()).isEqualTo(RESULT_MESSAGE);
    }

    private AttemptAuditEventResult makeAttemptAuditEventResult(ResultEventData resultEventData) {
        return AttemptAuditEventResult.builder()
                .withId(UUID_ID)
                .withActorCrn(USER_CRN)
                .withRequestId(REQUEST_ID)
                .withResultCode(RESULT_CODE)
                .withResultMessage(RESULT_MESSAGE)
                .withResultEventData(resultEventData)
                .build();
    }

    private AttemptAuditEventResult makeMinimalAttemptAuditEventResult(ResultEventData resultEventData) {
        return AttemptAuditEventResult.builder()
                .withId(UUID_ID)
                .withActorCrn(USER_CRN)
                .withResultCode(RESULT_CODE)
                .withResultEventData(resultEventData)
                .build();
    }

    private Map<Class, AttemptAuditEventResultBuilderUpdater> createMockUtilizer(Class clazz) {
        return Map.of(clazz, mockAttemptAuditEventResultBuilderUpdater);
    }

}
