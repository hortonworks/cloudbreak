package com.sequenceiq.cloudbreak.conf;

import static com.cedarsoftware.util.io.JsonWriter.CUSTOM_WRITER_MAP;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

@Configuration
public class JsonIoConfig {

    @PostConstruct
    public void setupJsonReader() {
        JsonReader.assignInstantiator("com.google.common.collect.RegularImmutableBiMap", new JsonReader.MapFactory());
        JsonReader.assignInstantiator("com.google.common.collect.RegularImmutableMap", new JsonReader.MapFactory());
        JsonReader.assignInstantiator("com.google.common.collect.EmptyImmutableBiMap", new JsonReader.MapFactory());
        JsonReader.assignInstantiator("com.google.common.collect.SingletonImmutableBiMap", new JsonReader.MapFactory());
        JsonReader.assignInstantiator("java.util.Collections$EmptyMap", new JsonReader.MapFactory());
        JsonReader.assignInstantiator("java.util.Collections$SingletonMap", new JsonReader.MapFactory());

        JsonReader.assignInstantiator("com.google.common.collect.SingletonImmutableList", new JsonReader.CollectionFactory());
        JsonReader.assignInstantiator("com.google.common.collect.RegularImmutableList", new JsonReader.CollectionFactory());
        JsonReader.assignInstantiator("java.util.Collections$EmptyList", new JsonReader.CollectionFactory());
        JsonReader.assignInstantiator("java.util.Collections$SingletonList", new JsonReader.CollectionFactory());

        JsonReader.assignInstantiator("com.google.common.collect.RegularImmutableSet", new JsonReader.CollectionFactory());
        JsonReader.assignInstantiator("java.util.Collections$EmptySet", new JsonReader.CollectionFactory());
        JsonReader.assignInstantiator("java.util.Collections$SingletonSet", new JsonReader.CollectionFactory());
    }

    @Bean(name = "JsonWriterOptions")
    public Map<String, Object> getCustomWriteOptions() {
        Map<Class<?>, JsonWriter.JsonClassWriterBase> customWriters = new HashMap<>();
        customWriters.put(Exception.class, new NonPrimitiveFormJsonWriter() {
            @Override
            public void write(Object o, boolean showType, Writer output) throws IOException {
                output.write("\"detailMessage\":");
                JsonWriter.writeJsonUtf8String(((Exception) o).getMessage(), output);
            }
        });

        return Collections.singletonMap(CUSTOM_WRITER_MAP, customWriters);
    }

    private abstract static class NonPrimitiveFormJsonWriter implements JsonWriter.JsonClassWriter {
        @Override
        public boolean hasPrimitiveForm() {
            return false;
        }

        @Override
        public void writePrimitiveForm(Object o, Writer output) throws IOException {
            throw new UnsupportedOperationException("Primitive form write not allowed");
        }
    }
}
