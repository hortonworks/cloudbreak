package com.sequenceiq.it.cloudbreak.assertion.database;

import static java.lang.String.format;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.BaseMicroserviceClientDependentAssertion;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

public class RedbeamsDatabaseTestAssertion extends BaseMicroserviceClientDependentAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseTestAssertion.class);

    private RedbeamsDatabaseTestAssertion() {
    }

    public static Assertion<RedbeamsDatabaseTestDto, RedbeamsClient> containsDatabaseName(String databaseName, Integer expectedCount) {
        return (testContext, entity, redbeamsClient) -> {
            boolean countCorrect = entity.getResponses()
                    .stream()
                    .filter(databaseV4Response -> databaseV4Response.getName().contentEquals(databaseName))
                    .count() == expectedCount;
            if (!countCorrect) {
                throw new IllegalArgumentException("Database count for " + databaseName + " is not as expected!");
            }
            return entity;
        };
    }

    public static <T extends CloudbreakTestDto, U extends MicroserviceClient> Assertion<T, U> hasEntitlementAwareDatabaseInstanceType(
            Function<T, String> databaseServerCrnExtractor, String customInstanceType) {
        return (testContext, entity, client) -> {
            if (StringUtils.isBlank(customInstanceType)) {
                LOGGER.info("Custom database instance type is blank, skipping database instance type assertion.");
                return entity;
            }
            String databaseServerCrn = databaseServerCrnExtractor.apply(entity);
            if (StringUtils.isBlank(databaseServerCrn)) {
                throw new TestFailException("Database server CRN is not available, cannot assert database instance type!");
            }
            RedbeamsClient redbeamsClient = getClient(testContext, testContext.getActingUser(), RedbeamsClient.class);
            DatabaseServerV4Response databaseServer = redbeamsClient.getDefaultClient(testContext)
                    .databaseServerV4Endpoint()
                    .getByCrn(databaseServerCrn);
            String actualInstanceType = databaseServer.getInstanceType();
            boolean customInstanceTypeEntitled;
            try {
                CloudbreakClient cloudbreakClient = getClient(testContext, testContext.getActingUser(), CloudbreakClient.class);
                customInstanceTypeEntitled = cloudbreakClient.getDefaultClient(testContext)
                        .userProfileV4Endpoint()
                        .get()
                        .getEntitlements()
                        .contains(Entitlement.CDP_CUSTOM_DATABASE_INSTANCETYPE.name());
            } catch (Exception e) {
                LOGGER.warn("Failed to retrieve entitlements from user profile, falling back to simple instance type assertion.", e);
                if (!customInstanceType.equalsIgnoreCase(actualInstanceType)) {
                    throw new TestFailException(format("Expected database server '%s' to use instance type '%s' but it uses '%s'!",
                            databaseServerCrn, customInstanceType, actualInstanceType));
                }
                LOGGER.info(format("Database server '%s' is provisioned with the expected instance type '%s'.", databaseServerCrn, actualInstanceType));
                return entity;
            }
            if (customInstanceTypeEntitled) {
                if (!customInstanceType.equalsIgnoreCase(actualInstanceType)) {
                    throw new TestFailException(format("Custom database instance type entitlement is granted, expected database server '%s' to use "
                            + "instance type '%s' but it uses '%s'!", databaseServerCrn, customInstanceType, actualInstanceType));
                }
                LOGGER.info(format("Database server '%s' is provisioned with the expected custom instance type '%s'.",
                        databaseServerCrn, actualInstanceType));
            } else {
                if (customInstanceType.equalsIgnoreCase(actualInstanceType)) {
                    throw new TestFailException(format("Custom database instance type entitlement is not granted, yet database server '%s' uses the "
                            + "custom instance type '%s'!", databaseServerCrn, actualInstanceType));
                }
                LOGGER.info(format("Custom database instance type entitlement is not granted; requested '%s' was correctly ignored, database server "
                        + "'%s' uses the default instance type '%s'.", customInstanceType, databaseServerCrn, actualInstanceType));
            }
            return entity;
        };
    }
}
