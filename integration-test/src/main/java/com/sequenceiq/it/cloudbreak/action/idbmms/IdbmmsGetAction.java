package com.sequenceiq.it.cloudbreak.action.idbmms;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.idbmms.IdbmmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.IdbmmsClient;

public class IdbmmsGetAction implements Action<IdbmmsTestDto, IdbmmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdbmmsGetAction.class);

    @Override
    public IdbmmsTestDto action(TestContext testContext, IdbmmsTestDto testDto, IdbmmsClient client) throws Exception {
        String environmentCrn = testContext.get(EnvironmentTestDto.class).getCrn();
        String actingUserCrn = testContext.getActingUser().getCrn();
        Log.when(LOGGER, format(" Getting IDBroker mappings from environment '%s'. ", environmentCrn));
        GetMappingsResponse getMappingsResponse = client.getDefaultClient()
                .getMappings(actingUserCrn, environmentCrn);
        testDto.setMappingsDetails(getMappingsResponse);
        LOGGER.info(format(" IDBroker mappings has been set with Ranger Audit: '%s', Data Access: '%s' " +
                        "and Ranger Cloud Access Authorizer: '%s' roles at environment '%s'. ", getMappingsResponse.getBaselineRole(),
                getMappingsResponse.getDataAccessRole(), getMappingsResponse.getRangerCloudAccessAuthorizerRole(),
                environmentCrn));
        Log.when(LOGGER, format(" IDBroker mappings has been set with Ranger Audit: '%s', Data Access: '%s' " +
                        "and Ranger Cloud Access Authorizer: '%s' roles at environment '%s'. ", getMappingsResponse.getBaselineRole(),
                getMappingsResponse.getDataAccessRole(), getMappingsResponse.getRangerCloudAccessAuthorizerRole(),
                environmentCrn));
        return testDto;
    }
}
