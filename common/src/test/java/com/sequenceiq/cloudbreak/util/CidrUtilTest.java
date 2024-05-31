package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class CidrUtilTest {

    @Test
    void testCidrSetWhenCidrIsNull() {
        assertThat(CidrUtil.cidrSet(null)).isEmpty();
    }

    @Test
    void testCidrSetWhenCidrIsEmptyString() {
        assertThat(CidrUtil.cidrSet("")).isEmpty();
    }

    @Test
    void testCidrSetWhenCidrContainsOneElement() {
        Set<String> result = CidrUtil.cidrSet("0.0.0.0/0");
        assertThat(result).hasSize(1);
        assertThat(result).containsOnly("0.0.0.0/0");
    }

    @Test
    void testCidrSetWhenCidrContainsMultipleElements() {
        Set<String> result = CidrUtil.cidrSet("0.0.0.0/0,1.1.1.1/1");
        assertThat(result).hasSize(2);
        assertThat(result).containsOnly("0.0.0.0/0", "1.1.1.1/1");
    }
}