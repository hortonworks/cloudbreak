package com.sequenceiq.it.cloudbreak.assertion.database;

import static java.lang.String.format;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.BaseMicroserviceClientDependentAssertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;

public class RedbeamsDatabaseTestAssertion extends BaseMicroserviceClientDependentAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDatabaseTestAssertion.class);

    private static final String EXHAUSTED_FALLBACK_REASON = "exhausted";

    private static final Set<CloudPlatform> PLATFORMS_WITH_DATABASE_INSTANCE_TYPE_FALLBACK = EnumSet.of(CloudPlatform.AWS, CloudPlatform.AZURE);

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
                LOGGER.info("Database server '{}' is provisioned with the expected instance type '{}'.", databaseServerCrn, actualInstanceType);
                return entity;
            }
            if (customInstanceTypeEntitled) {
                if (!customInstanceType.equalsIgnoreCase(actualInstanceType)) {
                    throw new TestFailException(format("Custom database instance type entitlement is granted, expected database server '%s' to use "
                            + "instance type '%s' but it uses '%s'!", databaseServerCrn, customInstanceType, actualInstanceType));
                }
                LOGGER.info("Database server '{}' is provisioned with the expected custom instance type '{}'.", databaseServerCrn, actualInstanceType);
            } else {
                if (customInstanceType.equalsIgnoreCase(actualInstanceType)) {
                    throw new TestFailException(format("Custom database instance type entitlement is not granted, yet database server '%s' uses the "
                            + "custom instance type '%s'!", databaseServerCrn, actualInstanceType));
                }
                LOGGER.info("Custom database instance type entitlement is not granted; requested '{}' was correctly ignored, database server "
                        + "'{}' uses the default instance type '{}'.", customInstanceType, databaseServerCrn, actualInstanceType);
            }
            return entity;
        };
    }

    public static <T extends CloudbreakTestDto, U extends MicroserviceClient> Assertion<T, U> simulatedDatabaseCapacityFailureEntitled() {
        return (testContext, entity, client) -> {
            if (!isEntitled(testContext, Entitlement.CDP_SIMULATE_DATABASE_CAPACITY_FAILURE)) {
                throw new TestFailException(format("The '%s' entitlement is not granted for the acting user's account, the database instance type "
                        + "fallback cannot be exercised!", Entitlement.CDP_SIMULATE_DATABASE_CAPACITY_FAILURE.name()));
            }
            return entity;
        };
    }

    public static <T extends CloudbreakTestDto, U extends MicroserviceClient> Assertion<T, U> hasInstanceType(
            Function<T, String> databaseServerCrnExtractor, String expectedInstanceType) {
        return (testContext, entity, client) -> {
            DatabaseServerV4Response databaseServer = getDatabaseServer(testContext, databaseServerCrnExtractor.apply(entity));
            if (!expectedInstanceType.equalsIgnoreCase(databaseServer.getInstanceType())) {
                throw new TestFailException(format("Expected database server '%s' (fallbacks %s) to be provisioned with instance type '%s' but its "
                                + "instance type is '%s'!", databaseServer.getCrn(), databaseServer.getFallbackInstanceTypes(), expectedInstanceType,
                        databaseServer.getInstanceType()));
            }
            LOGGER.info("Database server '{}' is provisioned with the expected instance type '{}'.", databaseServer.getCrn(),
                    databaseServer.getInstanceType());
            return entity;
        };
    }

    public static <T extends CloudbreakTestDto, U extends MicroserviceClient> Assertion<T, U> failedWithExhaustedInstanceTypeFallback(
            Function<T, String> databaseServerCrnExtractor) {
        return (testContext, entity, client) -> {
            DatabaseServerV4Response databaseServer = getDatabaseServer(testContext, databaseServerCrnExtractor.apply(entity));
            if (databaseServer.getStatus() != Status.CREATE_FAILED) {
                throw new TestFailException(format("Expected database server '%s' to be in %s status but it is in %s status!", databaseServer.getCrn(),
                        Status.CREATE_FAILED, databaseServer.getStatus()));
            }
            if (!StringUtils.containsIgnoreCase(databaseServer.getStatusReason(), EXHAUSTED_FALLBACK_REASON)) {
                throw new TestFailException(format("Expected database server '%s' to fail because all instance types were exhausted, but its status "
                        + "reason is '%s'!", databaseServer.getCrn(), databaseServer.getStatusReason()));
            }
            LOGGER.info("Database server '{}' failed as expected with status reason '{}'.", databaseServer.getCrn(),
                    databaseServer.getStatusReason());
            return entity;
        };
    }

    public static <T extends CloudbreakTestDto, U extends MicroserviceClient> Assertion<T, U> usesPrimaryInstanceTypeWithEntitlementAwareFallbacks(
            Function<T, String> databaseServerCrnExtractor) {
        return (testContext, entity, client) -> {
            DatabaseServerV4Response databaseServer = getDatabaseServer(testContext, databaseServerCrnExtractor.apply(entity));
            String instanceType = databaseServer.getInstanceType();
            List<String> fallbackInstanceTypes = databaseServer.getFallbackInstanceTypes();
            if (StringUtils.isBlank(instanceType)) {
                throw new TestFailException(format("Database server '%s' has no instance type!", databaseServer.getCrn()));
            }
            if (CollectionUtils.emptyIfNull(fallbackInstanceTypes).stream().anyMatch(instanceType::equalsIgnoreCase)) {
                throw new TestFailException(format("Expected database server '%s' to be provisioned with its primary instance type but it is "
                        + "provisioned with fallback instance type '%s' (fallbacks %s)!", databaseServer.getCrn(), instanceType, fallbackInstanceTypes));
            }
            boolean fallbackEntitled = isEntitled(testContext, Entitlement.CDP_FALLBACK_DATABASE_INSTANCETYPE);
            CloudPlatform cloudPlatform = testContext.getCloudPlatform();
            boolean fallbackExpected = fallbackEntitled && PLATFORMS_WITH_DATABASE_INSTANCE_TYPE_FALLBACK.contains(cloudPlatform);
            String region = testContext.getCloudProvider().region();
            if (fallbackExpected && CollectionUtils.isEmpty(fallbackInstanceTypes)) {
                throw new TestFailException(format("The '%s' entitlement is granted, yet database server '%s' has no fallback instance types in "
                        + "region '%s'! Check that the region has database instance types configured in its enabled-regions definition.",
                        Entitlement.CDP_FALLBACK_DATABASE_INSTANCETYPE.name(), databaseServer.getCrn(), region));
            } else if (!fallbackExpected && CollectionUtils.isNotEmpty(fallbackInstanceTypes)) {
                throw new TestFailException(format("Database server '%s' on %s has fallback instance types %s in region '%s', but none were expected "
                        + "('%s' entitlement granted: %s, platforms supporting database instance type fallback: %s)!", databaseServer.getCrn(),
                        cloudPlatform, fallbackInstanceTypes, region, Entitlement.CDP_FALLBACK_DATABASE_INSTANCETYPE.name(), fallbackEntitled,
                        PLATFORMS_WITH_DATABASE_INSTANCE_TYPE_FALLBACK));
            }
            LOGGER.info("Database server '{}' uses its primary instance type '{}' with fallback instance types {} in region '{}'.",
                    databaseServer.getCrn(), instanceType, fallbackInstanceTypes, region);
            return entity;
        };
    }

    private static DatabaseServerV4Response getDatabaseServer(TestContext testContext, String databaseServerCrn) {
        if (StringUtils.isBlank(databaseServerCrn)) {
            throw new TestFailException("Database server CRN is not available, cannot assert the database server!");
        }
        RedbeamsClient redbeamsClient = getClient(testContext, testContext.getActingUser(), RedbeamsClient.class);
        return redbeamsClient.getDefaultClient(testContext)
                .databaseServerV4Endpoint()
                .getByCrn(databaseServerCrn);
    }

    private static boolean isEntitled(TestContext testContext, Entitlement entitlement) {
        CloudbreakClient cloudbreakClient = getClient(testContext, testContext.getActingUser(), CloudbreakClient.class);
        return cloudbreakClient.getDefaultClient(testContext)
                .userProfileV4Endpoint()
                .get()
                .getEntitlements()
                .contains(entitlement.name());
    }
}
