package com.sequenceiq.freeipa.events;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_ID;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.structuredevent.rest.CustomCrnOrNameProvider;

public abstract class AbstractCustomCrnOrNameProvider implements CustomCrnOrNameProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCustomCrnOrNameProvider.class);

    private final Set<String> environmentParams = Set.of("environment", "environmentCrn");

    @Override
    public Map<String, String> provide(RestCallDetails restCallDetails, CDPOperationDetails operationDetails, Map<String, String> restParams,
            String nameField, String crnField) {
        Map<String, String> param = new HashMap<>();
        try {
            Optional<NameValuePair> environmentValue = findEnvironmentCrnInQueryParams(restCallDetails);
            environmentValue.ifPresent(nameValuePair -> addFieldToParams(param, operationDetails, nameField, crnField, nameValuePair));
        } catch (URISyntaxException e) {
            LOGGER.warn("Cannot provide name and crn because the uri is invalid or any error occurred: {}", e.getMessage(), e);
        }
        return param;
    }

    private void addFieldToParams(Map<String, String> restParams, CDPOperationDetails operationDetails, String nameField, String crnField,
            NameValuePair nameValuePair) {
        String accountId;
        if (operationDetails == null) {
            accountId = ThreadBasedUserCrnProvider.getAccountId();
        } else {
            accountId = operationDetails.getAccountId();
        }
        List<? extends AccountAwareResource> resources = getResource(nameValuePair.getValue(), accountId);
        restParams.put(nameField, resources.stream().map(AccountAwareResource::getName).collect(Collectors.joining(",")));
        restParams.put(crnField, resources.stream().map(AccountAwareResource::getResourceCrn).collect(Collectors.joining(",")));
        restParams.put(RESOURCE_ID, resources.stream().map(r -> r.getId().toString()).collect(Collectors.joining(",")));
    }

    protected abstract List<? extends AccountAwareResource> getResource(String environmentCrn, String accountId);

    private Optional<NameValuePair> findEnvironmentCrnInQueryParams(RestCallDetails restCallDetails) throws URISyntaxException {
        String requestUri = restCallDetails.getRestRequest().getRequestUri();
        URI uri = new URI(requestUri);
        List<NameValuePair> queryParams = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
        return queryParams.stream().filter(p -> environmentParams.contains(p.getName())).findFirst();
    }
}
