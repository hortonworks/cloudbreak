package com.sequenceiq.it.cloudbreak.util;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@Component
public class EnvironmentUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUtil.class);

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Map<UmsVirtualGroupRight, String> getEnvironmentVirtualGroups(TestContext testContext, UmsClient client) {
        String accountId = testContext.getActingUserCrn().getAccountId();
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        Map<UmsVirtualGroupRight, String> virtualGroups = new HashMap<>();
        String virtualGroup = null;

        for (UmsVirtualGroupRight right : UmsVirtualGroupRight.values()) {
            try {
                virtualGroup = client.getDefaultClient().getWorkloadAdministrationGroupName(accountId,
                        right, environmentCrn, regionAwareInternalCrnGeneratorFactory);
            } catch (StatusRuntimeException ex) {
                if (Status.Code.NOT_FOUND != ex.getStatus().getCode()) {
                    LOGGER.info(format(" Virtual groups are missing for right: '%s' ", right.getRight()));
                }
            }
            if (StringUtils.hasText(virtualGroup)) {
                virtualGroups.put(right, virtualGroup);
            }
        }

        if (MapUtils.isNotEmpty(virtualGroups)) {
            Log.then(LOGGER, format(" Virtual groups are present [%s] for environment '%s' ", virtualGroups, environmentCrn));
            LOGGER.info(String.format(" Virtual groups are present [%s] for environment '%s' ", virtualGroups, environmentCrn));
        } else {
            throw new TestFailException(String.format(" Cannot find virtual groups for environment '%s' ", environmentCrn));
        }

        return virtualGroups;
    }
}
