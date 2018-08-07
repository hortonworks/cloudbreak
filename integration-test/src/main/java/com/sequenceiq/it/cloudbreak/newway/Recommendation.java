package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class Recommendation extends RecommendationEntity {

    static Function<IntegrationTestContext, Recommendation> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, Recommendation.class);
    }

    static Function<IntegrationTestContext, Recommendation> getNew() {
        return testContext -> new Recommendation();
    }

    public static Recommendation request() {
        return new Recommendation();
    }

    public static Recommendation isCreated() {
        Recommendation recommendation = new Recommendation();
        recommendation.setCreationStrategy(RecommendationAction::createInGiven);
        return recommendation;
    }

    public static Action<Recommendation> post(String key) {
        return new Action<>(getTestContext(key), RecommendationAction::post);
    }

    public static Action<Recommendation> post() {
        return post(RECOMMENDATION);
    }

    public static Assertion<Recommendation> assertThis(BiConsumer<Recommendation, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
