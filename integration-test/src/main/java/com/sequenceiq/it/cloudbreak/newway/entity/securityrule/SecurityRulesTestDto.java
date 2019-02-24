package com.sequenceiq.it.cloudbreak.newway.entity.securityrule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRulesV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;

@Prototype
public class SecurityRulesTestDto extends AbstractCloudbreakEntity<Object, SecurityRulesV4Response, SecurityRulesTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityRulesTestDto.class);

    private Boolean knoxEnabled = Boolean.FALSE;

    protected SecurityRulesTestDto(TestContext testContext) {
        super(null, testContext);
    }

    public Boolean getKnoxEnabled() {
        return knoxEnabled;
    }

    public SecurityRulesTestDto withKnoxEnabled(Boolean knoxEnabled) {
        this.knoxEnabled = knoxEnabled;
        return this;
    }

    @Override
    public SecurityRulesTestDto valid() {
        return withKnoxEnabled(false);
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
