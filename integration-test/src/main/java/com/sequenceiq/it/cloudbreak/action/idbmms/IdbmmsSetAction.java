package com.sequenceiq.it.cloudbreak.action.idbmms;

import static java.lang.String.format;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto;
import com.sequenceiq.it.cloudbreak.IdbmmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.idbmms.IdbmmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class IdbmmsSetAction implements Action<IdbmmsTestDto, IdbmmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdbmmsSetAction.class);

    private final String dataAccessRole;

    private final String baselineRole;

    public IdbmmsSetAction(String dataAccessRole, String baselineRole) {
        this.dataAccessRole = dataAccessRole;
        this.baselineRole = baselineRole;
    }

    @Override
    public IdbmmsTestDto action(TestContext testContext, IdbmmsTestDto testDto, IdbmmsClient client) throws Exception {
        String userCrn = testContext.getActingUserCrn().toString();
        String environmentCrn = testContext.get(EnvironmentTestDto.class).getCrn();
        Log.when(LOGGER, format(" Set IDBMMS Ranger Audit: '%s' and Data Access: '%s' to environment '%s' with user '%s'. ",
                testDto.getRequest().getBaselineRole(), testDto.getRequest().getDataAccessRole(), environmentCrn, userCrn));
        Log.whenJson(LOGGER, format(" IDBMMS mapping request:%n "), testDto.getRequest());
        IdBrokerMappingManagementProto.SetMappingsResponse setMappingsResponse = client.getDefaultClient()
                .setMappings(userCrn, environmentCrn, dataAccessRole, baselineRole, Optional.empty());
        testDto.setResponse(setMappingsResponse);
        LOGGER.info(format(" IDBMMS has been set with Ranger Audit: '%s' and Data Access: '%s' roles. ", testDto.getResponse().getBaselineRole(),
                testDto.getResponse().getDataAccessRole()));
        Log.whenJson(LOGGER, format(" IDBMMS has been set with response:%n "), testDto.getResponse());
        return testDto;
    }
}
