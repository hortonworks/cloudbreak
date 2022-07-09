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

public class IdbmmsGetAction implements Action<IdbmmsTestDto, IdbmmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdbmmsGetAction.class);

    @Override
    public IdbmmsTestDto action(TestContext testContext, IdbmmsTestDto testDto, IdbmmsClient client) throws Exception {
        String userCrn = testContext.getActingUserCrn().toString();
        String environmentCrn = testContext.get(EnvironmentTestDto.class).getCrn();
        IdBrokerMappingManagementProto.GetMappingsResponse getMappingsResponse = client.getDefaultClient()
                .getMappings(userCrn, environmentCrn, Optional.empty());
        testDto.setMappingsDetails(getMappingsResponse);
        LOGGER.info(format(" IDBMMS has been set with Ranger Audit: '%s' and Data Access: '%s' roles at environment '%s'. ",
                getMappingsResponse.getBaselineRole(), getMappingsResponse.getDataAccessRole(), environmentCrn));
        Log.when(LOGGER, format(" IDBMMS has been set with Ranger Audit: '%s' and Data Access: '%s' roles at environment '%s'. ",
                getMappingsResponse.getBaselineRole(), getMappingsResponse.getDataAccessRole(), environmentCrn));
        return testDto;
    }
}
