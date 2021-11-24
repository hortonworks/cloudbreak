package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import static com.sequenceiq.cloudbreak.structuredevent.rest.filter.LoggingStream.MAX_CONTENT_LENGTH;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_NAME;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.structuredevent.rest.CDPRestCommonService;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;

@Component
public class StructuredEventFilterUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventFilterUtil.class);

    @Value("${cdp.structuredevent.rest.contentlogging}")
    private Boolean contentLogging;

    @Inject
    private CDPDefaultStructuredEventClient structuredEventClient;

    @Inject
    private CDPRestCommonService restCommonService;

    @Inject
    private RepositoryBasedDataCollector dataCollector;

    @Inject
    private RestEventFilterRelatedObjectFactory restEventFilterRelatedObjectFactory;

    public InputStream logInboundEntity(StringBuilder content, InputStream stream, Charset charset) throws IOException {
        if (contentLogging) {
            if (!stream.markSupported()) {
                stream = new BufferedInputStream(stream, MAX_CONTENT_LENGTH + 1);
            }
            stream.mark(MAX_CONTENT_LENGTH + 1);
            byte[] entity = new byte[MAX_CONTENT_LENGTH + 1];
            int entitySize = IOUtils.read(stream, entity);
            if (entitySize != -1) {
                content.append(new String(entity, 0, Math.min(entitySize, MAX_CONTENT_LENGTH), charset));
                if (entitySize > MAX_CONTENT_LENGTH) {
                    content.append("...more...");
                }
            }
            content.append('\n');
            stream.reset();
        }
        return stream;
    }

    public void sendStructuredEvent(RestRequestDetails restRequest, RestResponseDetails restResponse, Map<String, String> restParams, Long requestTime,
            String responseBody) {
        boolean valid = checkRestParams(restParams);
        try {
            if (!valid) {
                LOGGER.debug("Cannot create structured event, because rest params are invalid.");
                return;
            }
            restResponse.setBody(responseBody);
            RestCallDetails restCall = new RestCallDetails();
            restCall.setRestRequest(restRequest);
            restCall.setRestResponse(restResponse);
            restCall.setDuration(System.currentTimeMillis() - requestTime);
            Map<String, String> params = restCommonService.collectCrnAndNameIfPresent(restCall, null, restParams, RESOURCE_NAME, RESOURCE_CRN);
            dataCollector.fetchDataFromDbIfNeed(params);
            restParams.putAll(params);
            CDPOperationDetails cdpOperationDetails = restEventFilterRelatedObjectFactory.createCDPOperationDetails(restParams, requestTime);
            CDPStructuredRestCallEvent structuredEvent = new CDPStructuredRestCallEvent(cdpOperationDetails, restCall, null, null);
            structuredEventClient.sendStructuredEvent(structuredEvent);
        } catch (UnsupportedOperationException e) {
            LOGGER.debug("Audit log is unnecessary: {}", e.getMessage());
        } catch (Exception ex) {
            LOGGER.warn("Failed to send structured event: " + ex.getMessage(), ex);
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
}
