package com.sequenceiq.it.cloudbreak.dto.securityrule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRulesV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class SecurityRulesTestDto extends AbstractCloudbreakTestDto<Object, SecurityRulesV4Response, SecurityRulesTestDto> {

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
    public int order() {
        return 500;
    }

}
