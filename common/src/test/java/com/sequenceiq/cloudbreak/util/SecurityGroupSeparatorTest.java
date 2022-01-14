package com.sequenceiq.cloudbreak.util;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class SecurityGroupSeparatorTest {

    @Test
    void testMultipleGroupWhenEveryElementPresentedOnlyOnceShouldReturnWithAllElement() {
        Set<String> securityGroupIds = SecurityGroupSeparator.getSecurityGroupIds("a,b,c,d");
        Assertions.assertThat(securityGroupIds).containsExactlyInAnyOrderElementsOf(Set.of("a", "b", "c", "d"));
    }

    @Test
    void testMultipleGroupWhenElementPresentedTwiceShouldReturnWithAllElementOnlyOnce() {
        Set<String> securityGroupIds = SecurityGroupSeparator.getSecurityGroupIds("a,b,c,d,d");
        Assertions.assertThat(securityGroupIds).containsExactlyInAnyOrderElementsOf(Set.of("a", "b", "c", "d"));
    }

    @Test
    void testMultipleGroupWhenElementPresentedTwiceAndSpacesShouldReturnWithAllElementOnlyOnceWithoutSpaces() {
        Set<String> securityGroupIds = SecurityGroupSeparator.getSecurityGroupIds("a, b,c, d,d");
        Assertions.assertThat(securityGroupIds).containsExactlyInAnyOrderElementsOf(Set.of("a", "b", "c", "d"));
    }
}