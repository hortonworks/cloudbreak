package com.sequenceiq.it.cloudbreak.newway.entity.subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SubscriptionV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SubscriptionV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;

@Prototype
public class SubscriptionTestDto extends AbstractCloudbreakEntity<SubscriptionV4Request, SubscriptionV4Response, SubscriptionTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionTestDto.class);

    protected SubscriptionTestDto(TestContext testContext) {
        super(new SubscriptionV4Request(), testContext);
    }

    public String getEndpointUrl() {
        return getRequest().getEndpointUrl();
    }

    public SubscriptionTestDto withEndpointUrl(String endpointUrl) {
        getRequest().setEndpointUrl(endpointUrl);
        return this;
    }

    @Override
    public SubscriptionTestDto valid() {
        return withEndpointUrl("https://localhost//");
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.debug("this entry point does not have any clean up operation");
    }

    @Override
    public int order() {
        return 500;
    }

}
