package com.sequenceiq.it.cloudbreak.action.cdl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.cdl.CdlTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CdlClient;

public class CdlCreateAction implements Action<CdlTestDto, CdlClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdlCreateAction.class);

    @Override
    public CdlTestDto action(TestContext testContext, CdlTestDto testDto, CdlClient client) throws Exception {
        ThreadBasedUserCrnProvider.doAs(testContext.getActingUserCrn().toString(), () -> {
            client.getDefaultClient().createDatalake(testDto.getRequest().getDatalakeName(), testDto.getRequest().getEnvironmentName(), "NONE");
            CdlCrudProto.DatalakeResponse datalake = client.getDefaultClient().findDatalake(testDto.getRequest().getEnvironmentName(), StringUtils.EMPTY);
            testDto.setResponse(datalake);
            LOGGER.info("Created CDL [{}].", datalake);
        });
        return testDto;
    }
}
