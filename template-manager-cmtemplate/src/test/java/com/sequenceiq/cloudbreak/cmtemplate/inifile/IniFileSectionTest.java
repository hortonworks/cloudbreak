package com.sequenceiq.cloudbreak.cmtemplate.inifile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class IniFileSectionTest {

    private static final String NAME = "name";

    private static final String PARENT_SECTION_NAMES = "parentSectionNames";

    private static final String QUALIFIED_NAME = PARENT_SECTION_NAMES + " " + NAME;

    private static final String EMPTY_NAME = "";

    private static final String EMPTY_KEY = "";

    private static final String KEY = "key";

    private static final String KEY_2 = "key2";

    private static final String CHILD_NAME = "childName";

    private static final String CHILD_NAME_2 = "childName2";

    private static final String VALUE = "value";

    private static final String VALUE_2 = "value2";

    @Nonnull
    private static IniFileSection rootSection() {
        return new IniFileSection();
    }

    @Nonnull
    private static IniFileSection regularSectionTopLevel() {
        return new IniFileSection(NAME, EMPTY_NAME);
    }

    @Nonnull
    private static IniFileSection regularSectionLowerLevel() {
        return new IniFileSection(NAME, PARENT_SECTION_NAMES);
    }

    @Test
    void constructorTestRootSection() {
        IniFileSection underTest = rootSection();

        assertThat(underTest.getName()).isEqualTo(EMPTY_NAME);
        assertThat(underTest.getParentSectionNames()).isEqualTo(EMPTY_NAME);
        assertThat(underTest.isRoot()).isEqualTo(true);

        validateEmptyChildSectionsAndConfigs(underTest);
    }

    private void validateEmptyChildSectionsAndConfigs(IniFileSection underTest) {
        Map<String, IniFileSection> childSectionsByName = underTest.getChildSectionsByName();
        assertThat(childSectionsByName).isNotNull();
        assertThat(childSectionsByName).isEmpty();

        Map<String, String> configsByName = underTest.getConfigsByName();
        assertThat(configsByName).isNotNull();
        assertThat(configsByName).isEmpty();
    }

    @ParameterizedTest(name = "name={0}")
    @ValueSource(strings = {EMPTY_NAME})
    @NullSource
    void constructorTestRegularSectionWhenEmptyName(String name) {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> new IniFileSection(name, PARENT_SECTION_NAMES));

        assertThat(illegalArgumentException).hasMessage("'name' must not be empty");
    }

    @Test
    void constructorTestRegularSectionWhenNullParentSectionNames() {
        NullPointerException nullPointerException = assertThrows(NullPointerException.class, () -> new IniFileSection(NAME, null));

        assertThat(nullPointerException).hasMessage("'parentSectionNames' must not be null");
    }

    @Test
    void constructorTestRegularSectionWhenSuccess() {
        IniFileSection underTest = regularSectionLowerLevel();

        assertThat(underTest.getName()).isEqualTo(NAME);
        assertThat(underTest.getParentSectionNames()).isEqualTo(PARENT_SECTION_NAMES);
        assertThat(underTest.isRoot()).isEqualTo(false);

        validateEmptyChildSectionsAndConfigs(underTest);
    }

    static Object[][] getQualifiedNameTestDataProvider() {
        return new Object[][]{
                // testCaseName, underTest, resultExpected
                {"rootSection", rootSection(), EMPTY_NAME},
                {"regularSectionTopLevel", regularSectionTopLevel(), NAME},
                {"regularSectionLowerLevel", regularSectionLowerLevel(), QUALIFIED_NAME},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getQualifiedNameTestDataProvider")
    void getQualifiedNameTest(String testCaseName, IniFileSection underTest, String resultExpected) {
        assertThat(underTest.getQualifiedName()).isEqualTo(resultExpected);
    }

    static Object[][] getQualifiedConfigKeyTestDataProvider() {
        return new Object[][]{
                // testCaseName, underTest, resultExpected
                {"rootSection", rootSection(), KEY},
                {"regularSectionLowerLevel", regularSectionLowerLevel(), QUALIFIED_NAME + " " + KEY},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getQualifiedConfigKeyTestDataProvider")
    void getQualifiedConfigKeyTest(String testCaseName, IniFileSection underTest, String resultExpected) {
        assertThat(underTest.getQualifiedConfigKey(KEY)).isEqualTo(resultExpected);
    }

    @Test
    void getChildSectionOrCreateIfAbsentTestWhenAbsent() {
        IniFileSection underTest = regularSectionLowerLevel();
        Map<String, IniFileSection> childSectionsByName = underTest.getChildSectionsByName();
        validateEmptyChildSectionsAndConfigs(underTest);

        IniFileSection result = underTest.getChildSectionOrCreateIfAbsent(CHILD_NAME);

        assertThat(result).isNotNull();
        assertThat(result.getQualifiedName()).isEqualTo(QUALIFIED_NAME + " " + CHILD_NAME);
        validateEmptyChildSectionsAndConfigs(result);
        assertThat(result.getName()).isEqualTo(CHILD_NAME);
        assertThat(result.getParentSectionNames()).isEqualTo(QUALIFIED_NAME);

        assertThat(childSectionsByName).hasSize(1);
        assertThat(childSectionsByName.get(CHILD_NAME)).isSameAs(result);
    }

    @Test
    void getChildSectionOrCreateIfAbsentTestWhenPresent() {
        IniFileSection underTest = regularSectionLowerLevel();
        Map<String, IniFileSection> childSectionsByName = underTest.getChildSectionsByName();
        validateEmptyChildSectionsAndConfigs(underTest);

        IniFileSection child = underTest.getChildSectionOrCreateIfAbsent(CHILD_NAME);
        validateEmptyChildSectionsAndConfigs(child);
        assertThat(childSectionsByName).hasSize(1);
        assertThat(childSectionsByName.get(CHILD_NAME)).isSameAs(child);

        assertThat(underTest.getChildSectionOrCreateIfAbsent(CHILD_NAME)).isSameAs(child);
        assertThat(childSectionsByName).hasSize(1);
        assertThat(childSectionsByName.get(CHILD_NAME)).isSameAs(child);
    }

    @Test
    void getChildSectionOrCreateIfAbsentTestWhenMultipleChildSections() {
        IniFileSection underTest = regularSectionLowerLevel();
        Map<String, IniFileSection> childSectionsByName = underTest.getChildSectionsByName();
        validateEmptyChildSectionsAndConfigs(underTest);

        IniFileSection result = underTest.getChildSectionOrCreateIfAbsent(CHILD_NAME);
        IniFileSection result2 = underTest.getChildSectionOrCreateIfAbsent(CHILD_NAME_2);

        assertThat(result).isNotNull();
        assertThat(result.getQualifiedName()).isEqualTo(QUALIFIED_NAME + " " + CHILD_NAME);
        validateEmptyChildSectionsAndConfigs(result);
        assertThat(result.getName()).isEqualTo(CHILD_NAME);
        assertThat(result.getParentSectionNames()).isEqualTo(QUALIFIED_NAME);

        assertThat(result2).isNotNull();
        assertThat(result2.getQualifiedName()).isEqualTo(QUALIFIED_NAME + " " + CHILD_NAME_2);
        validateEmptyChildSectionsAndConfigs(result2);
        assertThat(result2.getName()).isEqualTo(CHILD_NAME_2);
        assertThat(result2.getParentSectionNames()).isEqualTo(QUALIFIED_NAME);

        assertThat(childSectionsByName).hasSize(2);
        assertThat(childSectionsByName.get(CHILD_NAME)).isSameAs(result);
        assertThat(childSectionsByName.get(CHILD_NAME_2)).isSameAs(result2);
    }

    @ParameterizedTest(name = "key={0}")
    @ValueSource(strings = {EMPTY_KEY})
    @NullSource
    void addConfigTestWhenEmptyKey(String key) {
        IniFileSection underTest = rootSection();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.addConfig(key, VALUE));

        assertThat(illegalArgumentException).hasMessage("'key' must not be empty");
    }

    @Test
    void addConfigTestWhenNullValue() {
        IniFileSection underTest = rootSection();
        NullPointerException nullPointerException = assertThrows(NullPointerException.class, () -> underTest.addConfig(KEY, null));

        assertThat(nullPointerException).hasMessage("'value' must not be null");
    }

    @Test
    void addConfigTestWhenSingleNewConfig() {
        IniFileSection underTest = rootSection();
        Map<String, String> configsByName = underTest.getConfigsByName();
        validateEmptyChildSectionsAndConfigs(underTest);

        underTest.addConfig(KEY, VALUE);

        assertThat(configsByName).hasSize(1);
        assertThat(configsByName.get(KEY)).isEqualTo(VALUE);
    }

    @Test
    void addConfigTestWhenExistingConfig() {
        IniFileSection underTest = rootSection();
        Map<String, String> configsByName = underTest.getConfigsByName();
        validateEmptyChildSectionsAndConfigs(underTest);

        underTest.addConfig(KEY, VALUE);
        assertThat(configsByName).hasSize(1);
        assertThat(configsByName.get(KEY)).isEqualTo(VALUE);

        underTest.addConfig(KEY, VALUE_2);

        assertThat(configsByName).hasSize(1);
        assertThat(configsByName.get(KEY)).isEqualTo(VALUE_2);
    }

    @Test
    void addConfigTestWhenMultipleNewConfigs() {
        IniFileSection underTest = rootSection();
        Map<String, String> configsByName = underTest.getConfigsByName();
        validateEmptyChildSectionsAndConfigs(underTest);

        underTest.addConfig(KEY, VALUE);
        underTest.addConfig(KEY_2, VALUE_2);

        assertThat(configsByName).hasSize(2);
        assertThat(configsByName.get(KEY)).isEqualTo(VALUE);
        assertThat(configsByName.get(KEY_2)).isEqualTo(VALUE_2);
    }

    @Test
    void printTestWhenNullStringBuilder() {
        IniFileSection underTest = rootSection();
        NullPointerException nullPointerException = assertThrows(NullPointerException.class, () -> underTest.print(null));

        assertThat(nullPointerException).hasMessage("'sb' must not be null");
    }

    @Nonnull
    private static String section(int sectionIndex) {
        return String.format("section_%d", sectionIndex);
    }

    @Nonnull
    private static String key(int sectionIndex, int keyIndex) {
        return String.format("key_%d_%d", sectionIndex, keyIndex);
    }

    @Nonnull
    private static String value(int sectionIndex, int valueIndex) {
        return String.format("value_%d_%d", sectionIndex, valueIndex);
    }

    @Nonnull
    private static IniFileSection sectionTree(boolean globalConfigs, int topChildNum, boolean topChildConfigs, boolean lowerChild,
            boolean lowerChildConfigs) {
        IniFileSection rootSection = rootSection();
        if (globalConfigs) {
            rootSection.addConfig(KEY, VALUE);
            rootSection.addConfig(KEY_2, VALUE_2);
        }
        int sectionIndex = 0;
        for (int i = 0; i < topChildNum; i++) {
            sectionIndex++;
            IniFileSection topChildSection = rootSection.getChildSectionOrCreateIfAbsent(section(sectionIndex));
            if (topChildConfigs) {
                topChildSection.addConfig(key(sectionIndex, 1), value(sectionIndex, 1));
            }
            if (lowerChild) {
                sectionIndex++;
                IniFileSection lowerChildSection = topChildSection.getChildSectionOrCreateIfAbsent(section(sectionIndex));
                if (lowerChildConfigs) {
                    lowerChildSection.addConfig(key(sectionIndex, 1), value(sectionIndex, 1));
                    lowerChildSection.addConfig(key(sectionIndex, 2), value(sectionIndex, 2));
                }
            }
        }
        return rootSection;
    }

    static Object[][] printTestWhenSuccessDataProvider() {
        return new Object[][]{
                // testCaseName, underTest, resultExpected
                {"empty rootSection", rootSection(), ""},
                {"rootSection with global configs only", sectionTree(true, 0, false, false, false), "key=value\nkey2=value2\n"},
                {"rootSection with a single child section only", sectionTree(false, 1, false, false, false), "section_1\n"},
                {"rootSection with a single child section only and child configs", sectionTree(false, 1, true, false, false), "section_1\nkey_1_1=value_1_1\n"},
                {"rootSection with global configs and single child section", sectionTree(true, 1, true, false, false),
                        "key=value\nkey2=value2\nsection_1\nkey_1_1=value_1_1\n"},
                {"rootSection with global configs and two child sections", sectionTree(true, 2, true, false, false),
                        "key=value\nkey2=value2\nsection_1\nkey_1_1=value_1_1\nsection_2\nkey_2_1=value_2_1\n"},
                {"rootSection with global configs and four child sections and two levels", sectionTree(true, 2, true, true, false),
                        "key=value\nkey2=value2\nsection_1\nkey_1_1=value_1_1\nsection_2\nsection_3\nkey_3_1=value_3_1\nsection_4\n"},
                {"rootSection with four child sections and two levels and configs everywhere", sectionTree(true, 2, true, true, true),
                        "key=value\nkey2=value2\nsection_1\nkey_1_1=value_1_1\nsection_2\nkey_2_1=value_2_1\nkey_2_2=value_2_2\n" +
                                "section_3\nkey_3_1=value_3_1\nsection_4\nkey_4_1=value_4_1\nkey_4_2=value_4_2\n"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("printTestWhenSuccessDataProvider")
    void printTestWhenSuccess(String testCaseName, IniFileSection underTest, String resultExpected) {
        StringBuilder sb = new StringBuilder();

        underTest.print(sb);

        assertThat(sb.toString()).isEqualTo(resultExpected);
    }

}
