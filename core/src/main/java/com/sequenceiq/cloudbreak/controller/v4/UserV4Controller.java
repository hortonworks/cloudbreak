package com.sequenceiq.cloudbreak.controller.v4;

import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.user.UserV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.user.responses.UserV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserV4Response;
import com.sequenceiq.cloudbreak.api.model.users.UserIdComparator;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;

@Controller
@Transactional(TxType.NEVER)
public class UserV4Controller implements UserV4Endpoint {

    @Inject
    private UserService userService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public UserV4Responses getAll() {
        CloudbreakUser user = restRequestThreadLocalService.getCloudbreakUser();
        SortedSet<UserV4Response> results = new TreeSet<>(new UserIdComparator());
        results.addAll(converterUtil.convertAllAsSet(userService.getAll(user), UserV4Response.class));
        return new UserV4Responses(results);
    }

}
