package com.sequenceiq.cloudbreak.cloud.aws.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AwsPageCollectorTest {
    @Test
    public void testCollector() {
        Map<String, TestCollectable> collection = Map.of(
                "0", new TestCollectable("1", List.of("first")),
                "1", new TestCollectable(null, List.of("second")));
        TestToken testToken = new TestToken("0");
        List<String> stringList = AwsPageCollector.collectPages(requestString -> collection.get(testToken.getToken()),
                testToken,
                TestCollectable::getListOfSomething,
                TestCollectable::getToken,
                TestToken::setToken);

        assertThat(stringList, contains("first", "second"));
    }
}