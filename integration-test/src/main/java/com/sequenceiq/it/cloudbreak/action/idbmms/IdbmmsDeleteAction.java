package com.sequenceiq.it.cloudbreak.action.idbmms;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.idbmms.IdbmmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.IdbmmsClient;

public class IdbmmsDeleteAction implements Action<IdbmmsTestDto, IdbmmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdbmmsDeleteAction.class);

    @Override
    public IdbmmsTestDto action(TestContext testContext, IdbmmsTestDto testDto, IdbmmsClient client) throws Exception {
        String environmentCrn = testContext.get(EnvironmentTestDto.class).getCrn();
        String actingUserCrn = testContext.getActingUser().getCrn();
        Log.when(LOGGER, format(" Deleting IDBroker mappings from environment: %s", environmentCrn));
        DeleteMappingsResponse deleteMappingsResponse = client.getDefaultClient()
                .deleteMappings(actingUserCrn, environmentCrn);
        testDto.setDeleteMappingsDetails(deleteMappingsResponse);
        Log.when(LOGGER, format(" IDBroker mappings has been deleted with details %s! ", deleteMappingsResponse));
        return testDto;
    }
}
