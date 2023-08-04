package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserDataReplacerTest {

    @Test
    void replace() {
        String result = new UserDataReplacer("export PARAM=true\n")
                .replace("PARAM", false)
                .getUserData();
        assertThat(result).isEqualTo("export PARAM=false\n");
    }

    @Test
    void add() {
        String result = new UserDataReplacer("export PARAM=true\n")
                .replaceQuoted("PARAM2", "newValue")
                .getUserData();
        assertThat(result).isEqualTo("export PARAM=true\nexport PARAM2=\"newValue\"\n");
    }

    @Test
    void remove() {
        String result = new UserDataReplacer("export PARAM=true\nexport PARAM2=\"oldValue\"\n")
                .replaceQuoted("PARAM2", null)
                .getUserData();
        assertThat(result).isEqualTo("export PARAM=true\n");
    }

    @Test
    void same() {
        String result = new UserDataReplacer("export PARAM=true\n")
                .replace("PARAM", true)
                .getUserData();
        assertThat(result).isEqualTo("export PARAM=true\n");
    }

    @Test
    void bothNull() {
        String result = new UserDataReplacer("export PARAM=true\n")
                .replace("PARAM2", null)
                .getUserData();
        assertThat(result).isEqualTo("export PARAM=true\n");
    }

    @Test
    void extractValueWithQuotes() {
        String result = new UserDataReplacer("export CCM_V2_AGENT_ACCESS_KEY_ID=\"accessKeyId\"")
                .extractValue("CCM_V2_AGENT_ACCESS_KEY_ID");
        assertThat(result).isEqualTo("accessKeyId");
    }

    @Test
    void extractValueWithoutQuotes() {
        String result = new UserDataReplacer("export IS_CCM_V2_JUMPGATE_ENABLED=true")
                .extractValue("IS_CCM_V2_JUMPGATE_ENABLED");
        assertThat(result).isEqualTo("true");
    }

}
