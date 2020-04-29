package com.sequenceiq.cloudbreak.audit.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.AttemptAuditEventResult;
import com.sequenceiq.cloudbreak.audit.model.ResultApiRequestData;
import com.sequenceiq.cloudbreak.audit.model.ResultEventData;
import com.sequenceiq.cloudbreak.audit.model.ResultServiceEventData;

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

    private final AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter underTest = new AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter();

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
        underTest.convert(source);
    }

    @Test
    void convertWithoutResultEventData() {
        AttemptAuditEventResult source = makeAttemptAuditEventResult(null);

        AuditProto.AttemptAuditEventResult target = underTest.convert(source);
        assertGeneric(target);
        assertThat(target.getEventTypeCase()).isEqualTo(AuditProto.AttemptAuditEventResult.EventTypeCase.EVENTTYPE_NOT_SET);
    }

    @Test
    void convertWithResultServiceEventData() {
        ResultServiceEventData rsed = ResultServiceEventData.builder()
                .withResourceCrns(CRNS)
                .withResultDetails(RESULT_DETAILS)
                .build();
        AttemptAuditEventResult source = makeAttemptAuditEventResult(rsed);

        AuditProto.AttemptAuditEventResult target = underTest.convert(source);
        assertGeneric(target);
        assertThat(target.getEventTypeCase()).isEqualTo(AuditProto.AttemptAuditEventResult.EventTypeCase.RESULTSERVICEEVENTDATA);
        assertThat(target.getResultServiceEventData().getResourceCrnList()).hasSameElementsAs(CRNS);
        assertThat(target.getResultServiceEventData().getResultDetails()).isEqualTo(RESULT_DETAILS);
    }

    @Test
    void convertWithResultApiRequestData() {
        ResultApiRequestData rsed = ResultApiRequestData.builder().withResponseParameters(RESPONSE_PARAMETERS).build();
        AttemptAuditEventResult source = makeAttemptAuditEventResult(rsed);

        AuditProto.AttemptAuditEventResult target = underTest.convert(source);
        assertGeneric(target);
        assertThat(target.getEventTypeCase()).isEqualTo(AuditProto.AttemptAuditEventResult.EventTypeCase.RESULTAPIREQUESTDATA);
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
}
