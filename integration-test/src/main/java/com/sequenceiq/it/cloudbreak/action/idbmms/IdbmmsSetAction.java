package com.sequenceiq.it.cloudbreak.action.idbmms;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.idbmms.IdbmmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.IdbmmsClient;

public class IdbmmsSetAction implements Action<IdbmmsTestDto, IdbmmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdbmmsSetAction.class);

    private final String dataAccessRole;

    private final String baselineRole;

    private final String rangerAccessAuthorizerRole;

    public IdbmmsSetAction(String dataAccessRole, String baselineRole, String rangerAccessAuthorizerRole) {
        this.dataAccessRole = dataAccessRole;
        this.baselineRole = baselineRole;
        this.rangerAccessAuthorizerRole = rangerAccessAuthorizerRole;
    }

    @Override
    public IdbmmsTestDto action(TestContext testContext, IdbmmsTestDto testDto, IdbmmsClient client) throws Exception {
        String environmentCrn = testContext.get(EnvironmentTestDto.class).getCrn();
        String actingUserCrn = testContext.getActingUser().getCrn();
        Log.when(LOGGER, format(" Setting IDBroker mappings to environment '%s' with Data Access Role '%s', Baseline Role '%s' " +
                        "and Ranger Cloud Access Authorizer Role '%s'. ", environmentCrn, dataAccessRole, baselineRole, rangerAccessAuthorizerRole));
        SetMappingsResponse setMappingsResponse = client.getDefaultClient()
                .setMappings(actingUserCrn, environmentCrn, dataAccessRole, baselineRole, rangerAccessAuthorizerRole);
        testDto.setResponse(setMappingsResponse);
        LOGGER.info(format(" IDBroker mappings has been set with Ranger Audit: '%s', Data Access: '%s' roles " +
                        "and Ranger Cloud Access Authorizer Role '%s'. ", testDto.getResponse().getBaselineRole(), testDto.getResponse().getDataAccessRole(),
                testDto.getResponse().getRangerCloudAccessAuthorizerRole()));
        Log.when(LOGGER, format(" IDBroker mappings has been set with Ranger Audit: '%s', Data Access: '%s' roles " +
                        "and Ranger Cloud Access Authorizer Role '%s'. ", testDto.getResponse().getBaselineRole(), testDto.getResponse().getDataAccessRole(),
                testDto.getResponse().getRangerCloudAccessAuthorizerRole()));
        return testDto;
    }
}
