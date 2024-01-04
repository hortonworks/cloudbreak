package com.sequenceiq.cloudbreak.cmtemplate.inifile;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class IniFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(IniFile.class);

    private static final String NL_REGEX = "\n";

    private static final Pattern SECTION_PATTERN = Pattern.compile("(\\[+)\\s*([a-zA-Z0-9_-]+)\\s*(]+)");

    private static final int GROUP_SECTION_OPEN = 1;

    private static final int GROUP_SECTION_ID = 2;

    private static final int GROUP_SECTION_CLOSE = 3;

    private static final String COMMENT_MARKER = "#";

    private static final Pattern CONFIG_PATTERN = Pattern.compile("([a-zA-Z0-9_-]+)\\s*=(.*)");

    private static final int GROUP_CONFIG_KEY = 1;

    private static final int GROUP_CONFIG_VALUE = 2;

    private final IniFileSection rootSection;

    // Only to be instantiated via IniFileFactory
    IniFile() {
        rootSection = new IniFileSection();
    }

    @VisibleForTesting
    IniFile(IniFileSection rootSection) {
        this.rootSection = rootSection;
    }

    public void addContent(String content) {
        Deque<IniFileSection> activeSections = new LinkedList<>();
        activeSections.push(rootSection);
        List<String> lines = Arrays.asList(content.split(NL_REGEX));
        lines.forEach(line -> processLine(activeSections, line));
    }

    @Nonnull
    public String print() {
        StringBuilder sb = new StringBuilder();
        rootSection.print(sb);
        return sb.toString();
    }

    private void processLine(Deque<IniFileSection> activeSections, String line) {
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty() || isComment(trimmedLine)) {
            LOGGER.info("Ignoring line: {}", trimmedLine);
            return;
        }

        Matcher sectionMatcher = SECTION_PATTERN.matcher(trimmedLine);
        if (sectionMatcher.matches()) {
            changeSection(activeSections, sectionMatcher);
            return;
        }

        Matcher configMatcher = CONFIG_PATTERN.matcher(trimmedLine);
        if (configMatcher.matches()) {
            addConfig(activeSections, configMatcher);
            return;
        }

        throw new IllegalArgumentException(String.format("Invalid line: %s", trimmedLine));
    }

    private boolean isComment(String trimmedLine) {
        return trimmedLine.startsWith(COMMENT_MARKER);
    }

    private void changeSection(Deque<IniFileSection> activeSections, Matcher sectionMatcher) {
        int activeSectionLevel = activeSections.size() - 1;
        int newSectionLevel = getSectionLevel(sectionMatcher);
        String normalizedSectionName = getNormalizedSectionName(sectionMatcher);
        validateSectionLevel(activeSectionLevel, newSectionLevel, activeSections.peek().getQualifiedName(), normalizedSectionName);

        // This is a no-op if newSectionLevel > activeSectionLevel (actually newSectionLevel == activeSectionLevel + 1)
        for (int i = 0; i < activeSectionLevel - newSectionLevel + 1; i++) {
            activeSections.pop();
        }

        IniFileSection newSection = activeSections.peek().getChildSectionOrCreateIfAbsent(normalizedSectionName);
        activeSections.push(newSection);
        LOGGER.info("New active section '{}'", newSection.getQualifiedName());
    }

    private int getSectionLevel(Matcher sectionMatcher) {
        String open = sectionMatcher.group(GROUP_SECTION_OPEN);
        String close = sectionMatcher.group(GROUP_SECTION_CLOSE);
        int openLength = open.length();
        if (openLength != close.length()) {
            throw new IllegalArgumentException(String.format("Malformed section header: '%s'", sectionMatcher.group()));
        }
        return openLength;
    }

    @Nonnull
    private String getNormalizedSectionName(Matcher sectionMatcher) {
        return sectionMatcher.group(GROUP_SECTION_OPEN) + sectionMatcher.group(GROUP_SECTION_ID) + sectionMatcher.group(GROUP_SECTION_CLOSE);
    }

    private void validateSectionLevel(int activeSectionLevel, int newSectionLevel, String activeSectionNames, String newSectionName) {
        if (newSectionLevel <= activeSectionLevel) {
            return;
        }
        if (newSectionLevel != activeSectionLevel + 1) {
            throw new IllegalArgumentException(
                    String.format("Invalid section level transition: active level %d, new level %d, active sections '%s', new section '%s'",
                            activeSectionLevel, newSectionLevel, activeSectionNames, newSectionName));
        }
    }

    private void addConfig(Deque<IniFileSection> activeSections, Matcher configMatcher) {
        String key = configMatcher.group(GROUP_CONFIG_KEY);
        String value = configMatcher.group(GROUP_CONFIG_VALUE).trim();
        activeSections.peek().addConfig(key, value);
        LOGGER.info("Setting config: key '{}', value '{}'", key, value);
    }

}
