package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;

public class GetUserDetailsAction implements Action<UmsTestDto, UmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetUserDetailsAction.class);

    private final String userCrn;

    public GetUserDetailsAction(String userCrn) {
        this.userCrn = userCrn;
    }

    @Override
    public UmsTestDto action(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        Log.when(LOGGER, format(" Getting UMS user '%s' details. ", userCrn));
        Log.whenJson(LOGGER, " Get UMS user details request: ", testDto.getRequest());
        testDto.setResponse(client.getDefaultClient(testContext).getUserDetails(userCrn));
        UserManagementProto.User user = testDto.getResponse();
        LOGGER.info(format(" User details %ncrn: %s %nworkload username: %s %nfirst name: %s %nlast name: %s %nstate: %s %ncreation date: %s " +
                        "%nemail: %s %nexternal user id: %s %nSFDC contact id: %s ", user.getCrn(), user.getWorkloadUsername(), user.getFirstName(),
                user.getLastName(), user.getState().name(), user.getCreationDate(), user.getEmail(), user.getExternalUserId(), user.getSfdcContactId()));
        Log.when(LOGGER, format(" User details %ncrn: %s %nworkload username: %s %nfirst name: %s %nlast name: %s %nstate: %s %ncreation date: %s " +
                        "%nemail: %s %nexternal user id: %s %nSFDC contact id: %s ", user.getCrn(), user.getWorkloadUsername(), user.getFirstName(),
                user.getLastName(), user.getState().name(), user.getCreationDate(), user.getEmail(), user.getExternalUserId(), user.getSfdcContactId()));
        return testDto;
    }
}
