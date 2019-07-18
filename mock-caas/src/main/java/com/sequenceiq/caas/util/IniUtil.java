package com.sequenceiq.caas.util;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.stereotype.Component;

@Component
public class IniUtil {

    public Map<String, Properties> parseIni(Reader reader) throws IOException {
        Map<String, Properties> result = new HashMap<>();
        new Properties() {

            private Properties section;

            @Override
            public Object put(Object key, Object value) {
                String header = String.format("%s %s", key, value).trim();
                if (header.startsWith("[") && header.endsWith("]")) {
                    section = new Properties();
                    return result.put(header.substring(1, header.length() - 1), section);
                } else {
                    return section.put(key, value);
                }
            }

            @Override
            public synchronized boolean equals(Object o) {
                return super.equals(o);
            }

            @Override
            public synchronized int hashCode() {
                return super.hashCode();
            }
        }.load(reader);
        return result;
    }

}
