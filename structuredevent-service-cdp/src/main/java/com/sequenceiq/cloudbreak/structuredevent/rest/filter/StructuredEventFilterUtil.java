package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_NAME;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.structuredevent.rest.CDPRestCommonService;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.service.audit.rest.CDPRestAuditEventSender;

@Component
public class StructuredEventFilterUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventFilterUtil.class);

    @Inject
    private CDPDefaultStructuredEventClient structuredEventClient;

    @Inject
    private CDPRestCommonService restCommonService;

    @Inject
    private RepositoryBasedDataCollector dataCollector;

    @Inject
    private CdpOperationDetailsFactory cdpOperationDetailsFactory;

    @Inject
    private CDPRestAuditEventSender cdpRestAuditEventSender;

    public void sendStructuredEvent(RestRequestDetails restRequest, RestResponseDetails restResponse, Map<String, String> restParams, Long requestTime,
            String responseBody) {
        boolean valid = checkRestParams(restParams);
        try {
            if (!valid) {
                LOGGER.debug("Cannot create structured event, because rest params are invalid.");
                return;
            }
            CDPStructuredRestCallEvent event = createRestCallEventAndSetResponseBody(restRequest, restResponse, restParams, requestTime, responseBody, TRUE);
            structuredEventClient.sendStructuredEvent(event);
        } catch (UnsupportedOperationException e) {
            LOGGER.debug("Audit log is unnecessary: {}", e.getMessage());
        } catch (Exception ex) {
            LOGGER.warn("Failed to send structured event: " + ex.getMessage(), ex);
        }
    }

    public void sendRestAuditEvent(RestRequestDetails restRequest, RestResponseDetails restResponse, Map<String, String> restParams, Long requestTime,
            String responseBody) {
        try {
            String requestId = restRequest.getRequestId();
            LOGGER.debug("Trying to send audit rest event for request id: '{}'", requestId);
            CDPStructuredRestCallEvent event = createRestCallEventAndSetResponseBody(restRequest, restResponse, restParams, requestTime, responseBody, FALSE);
            cdpRestAuditEventSender.createEvent(event);
        } catch (Exception ex) {
            LOGGER.warn("Failed to send rest audit event: " + ex.getMessage(), ex);
        }
    }

    private boolean checkRestParams(Map<String, String> restParams) {
        boolean empty = restParams.isEmpty();
        if (empty) {
            LOGGER.debug("Rest param is empty");
            return false;
        }
        boolean allNull = restParams.values().stream().allMatch(Objects::isNull);
        if (allNull) {
            LOGGER.debug("Rest param is not empty but all values are null");
            return false;
        }
        return true;
    }

    private CDPStructuredRestCallEvent createRestCallEventAndSetResponseBody(RestRequestDetails restRequest, RestResponseDetails restResponse,
            Map<String, String> restParams, Long requestTime, String responseBody, boolean validateResourceIdentifierExistence) {
        restResponse.setBody(responseBody);
        RestCallDetails restCall = new RestCallDetails();
        restCall.setRestRequest(restRequest);
        restCall.setRestResponse(restResponse);
        restCall.setDuration(System.currentTimeMillis() - requestTime);
        try {
            Map<String, String> params = restCommonService.collectCrnAndNameIfPresent(restCall, null, restParams, RESOURCE_NAME, RESOURCE_CRN);
            dataCollector.fetchDataFromDbIfNeed(params);
            restParams.putAll(params);
        } catch (UnsupportedOperationException unsupportedOperationExc) {
            LOGGER.info("Can't determine resource name or CRN, creating audit entry without them original error: {}", unsupportedOperationExc.getMessage());
            if (validateResourceIdentifierExistence) {
                throw unsupportedOperationExc;
            }
        }
        CDPOperationDetails cdpOperationDetails = cdpOperationDetailsFactory.createCDPOperationDetails(restParams, requestTime);
        return new CDPStructuredRestCallEvent(cdpOperationDetails, restCall, null, null);
    }
}
