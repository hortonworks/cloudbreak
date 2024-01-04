package com.sequenceiq.cloudbreak.cmtemplate.inifile;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

class IniFileSection {

    private static final Logger LOGGER = LoggerFactory.getLogger(IniFileSection.class);

    private static final String EMPTY_NAME = "";

    private static final String QUALIFIED_DELIMITER = " ";

    private static final char NL = '\n';

    private static final String CONFIG_VALUE_DELIMITER = "=";

    private final Map<String, IniFileSection> childSectionsByName = new LinkedHashMap<>();

    private final Map<String, String> configsByName = new LinkedHashMap<>();

    private final String name;

    private final String parentSectionNames;

    private final boolean root;

    // Constructor for the root section
    IniFileSection() {
        name = EMPTY_NAME;
        parentSectionNames = EMPTY_NAME;
        root = true;
    }

    // Constructor for non-root sections
    IniFileSection(String name, String parentSectionNames) {
        if (isNullOrEmpty(name)) {
            throw new IllegalArgumentException("'name' must not be empty");
        }
        this.name = name;
        this.parentSectionNames = requireNonNull(parentSectionNames, "'parentSectionNames' must not be null");
        root = false;
    }

    @Nonnull
    String getQualifiedName() {
        if (root) {
            return EMPTY_NAME;
        } else {
            return parentSectionNames.isEmpty() ? name : String.join(QUALIFIED_DELIMITER, parentSectionNames, name);
        }
    }

    @VisibleForTesting
    @Nonnull
    String getQualifiedConfigKey(String key) {
        String qualifiedName = getQualifiedName();
        return qualifiedName.isEmpty() ? key : String.join(QUALIFIED_DELIMITER, qualifiedName, key);
    }

    @Nonnull
    IniFileSection getChildSectionOrCreateIfAbsent(String childName) {
        return childSectionsByName.computeIfAbsent(childName, k -> new IniFileSection(childName, getQualifiedName()));
    }

    void addConfig(String key, String value) {
        if (isNullOrEmpty(key)) {
            throw new IllegalArgumentException("'key' must not be empty");
        }
        requireNonNull(value, "'value' must not be null");
        if (configsByName.containsKey(key)) {
            String oldValue = configsByName.get(key);
            LOGGER.warn("Overwriting config '{}': old value '{}', new value '{}'", getQualifiedConfigKey(key), oldValue, value);
        }
        configsByName.put(key, value);
    }

    void print(StringBuilder sb) {
        requireNonNull(sb, "'sb' must not be null");
        if (!root) {
            sb.append(name)
                    .append(NL);
        }
        configsByName.forEach((key, value) -> sb.append(key)
                .append(CONFIG_VALUE_DELIMITER)
                .append(value)
                .append(NL));
        childSectionsByName.values().forEach(s -> s.print(sb));
    }

    @VisibleForTesting
    @Nonnull
    Map<String, IniFileSection> getChildSectionsByName() {
        return childSectionsByName;
    }

    @VisibleForTesting
    @Nonnull
    Map<String, String> getConfigsByName() {
        return configsByName;
    }

    @VisibleForTesting
    @Nonnull
    String getName() {
        return name;
    }

    @VisibleForTesting
    @Nonnull
    String getParentSectionNames() {
        return parentSectionNames;
    }

    @VisibleForTesting
    boolean isRoot() {
        return root;
    }

}
