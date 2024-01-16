package com.sequenceiq.redbeams.flow;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class RedbeamsEventParameterFactory extends EventParameterFactory {

    private final DBStackService stackService;

    public RedbeamsEventParameterFactory(CrnUserDetailsService crnUserDetailsService, DBStackService stackService) {
        super(crnUserDetailsService);
        this.stackService = stackService;
    }

    @Override
    protected Optional<String> getUserCrnByResourceId(Long resourceId) {
        return Optional.ofNullable(stackService.getById(resourceId))
                .map(DBStack::getOwnerCrn)
                .map(Crn::toString);
    }
}
