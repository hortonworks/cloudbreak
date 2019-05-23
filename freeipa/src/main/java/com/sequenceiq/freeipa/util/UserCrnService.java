package com.sequenceiq.freeipa.util;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.exception.BadRequestException;

@Component
public class UserCrnService {
    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    public String getCurrentAccountId() {
        String userCrn = restRequestThreadLocalService.getUserCrn();
        Crn crn = Crn.fromString(userCrn);
        if (crn != null) {
            return crn.getAccountId();
        } else {
            throw new BadRequestException("Can not guess account ID from CRN");
        }
    }
}
