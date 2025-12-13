package com.sequenceiq.thunderhead.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class IniUtilTest {

    private final IniUtil underTest = new IniUtil();

    @Test
    void testParseIniFromReader() throws IOException {
        // GIVEN
        StringReader srData = new StringReader("[default]\naltus_access_key_id=accesKey\naltus_private_key=privateKey");
        // WHEN
        Map<String, Properties> result = underTest.parseIni(srData);
        // THEN
        assertTrue(result.containsKey("default"));
        assertEquals(result.get("default").getProperty("altus_access_key_id"), "accesKey");
        assertEquals(result.get("default").getProperty("altus_private_key"), "privateKey");
    }
}
