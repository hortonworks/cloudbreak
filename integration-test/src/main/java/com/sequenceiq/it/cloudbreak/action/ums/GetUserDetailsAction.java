package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class GetUserDetailsAction implements Action<UmsTestDto, UmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetUserDetailsAction.class);

    private final String userCrn;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public GetUserDetailsAction(String userCrn, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.userCrn = userCrn;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    @Override
    public UmsTestDto action(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        Log.when(LOGGER, format(" Getting UMS user '%s' details. ", userCrn));
        Log.whenJson(LOGGER, " Get UMS user details request: ", testDto.getRequest());
        testDto.setResponse(client.getDefaultClient()
                .getUserDetails(userCrn, regionAwareInternalCrnGeneratorFactory));
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
