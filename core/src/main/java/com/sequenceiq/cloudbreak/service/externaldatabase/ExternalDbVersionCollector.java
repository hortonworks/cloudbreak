package com.sequenceiq.cloudbreak.service.externaldatabase;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Component
public class ExternalDbVersionCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalDbVersionCollector.class);

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Optional<String> collectDbVersion(String databaseCrn) {
        DatabaseServerV4Response response = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> databaseServerV4Endpoint.getByCrn(databaseCrn));
        LOGGER.info("Recieved response for [{}]: {}", databaseCrn, response);
        return Optional.ofNullable(response.getMajorVersion()).map(MajorVersion::getMajorVersion);
    }
}
