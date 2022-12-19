package com.sequenceiq.cloudbreak.cmtemplate.inifile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IniFileTest {

    @Mock
    private IniFileSection rootSection;

    @InjectMocks
    private IniFile underTest;

    @Test
    void printTest() {
        doAnswer(invocation -> {
            StringBuilder sb = invocation.getArgument(0, StringBuilder.class);
            sb.append("foo");
            return null;
        }).when(rootSection).print(any(StringBuilder.class));

        assertThat(underTest.print()).isEqualTo("foo");
    }

    @Test
    void addContentTestWhenNull() {
        assertThrows(NullPointerException.class, () -> underTest.addContent(null));

        verifyNoInteractions(rootSection);
    }

    @ParameterizedTest(name = "content={0}")
    @ValueSource(strings = {"", "\n", "\n\n"})
    void addContentTestWhenEmpty(String content) {
        underTest.addContent(content);

        verifyNoInteractions(rootSection);
    }

    @ParameterizedTest(name = "content={0}")
    @ValueSource(strings = {" ", "  \n", "\t"})
    void addContentTestWhenWhitespaceOnly(String content) {
        underTest.addContent(content);

        verifyNoInteractions(rootSection);
    }

    @ParameterizedTest(name = "content={0}")
    @ValueSource(strings = {"#", "  #", " # ", "#foo", "## hello", "#a#", "#[foo]", "#setting=something"})
    void addContentTestWhenCommentOnly(String content) {
        underTest.addContent(content);

        verifyNoInteractions(rootSection);
    }

    @ParameterizedTest(name = "content={0}")
    @ValueSource(strings = {"[foo]]", "[[foo]"})
    void addContentTestWhenMismatchingSectionBracketCounts(String content) {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> underTest.addContent(content));

        assertThat(illegalArgumentException).hasMessage(String.format("Malformed section header: '%s'", content));

        verifyNoInteractions(rootSection);
    }

    @ParameterizedTest(name = "content={0}")
    @ValueSource(strings = {"[foo]", "[ foo]", "[foo ]", "[ foo ]", " [foo] ", " [ foo ] "})
    void addContentTestWhenSingleSectionSimple(String content) {
        when(rootSection.getQualifiedName()).thenReturn("root");
        IniFileSection newSection = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[foo]")).thenReturn(newSection);
        when(newSection.getQualifiedName()).thenReturn("first");

        underTest.addContent(content);

        verifyNoMoreInteractions(rootSection, newSection);
    }

    @Test
    void addContentTestWhenSingleSectionComplex() {
        when(rootSection.getQualifiedName()).thenReturn("root");
        IniFileSection newSection = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789]")).thenReturn(newSection);
        when(newSection.getQualifiedName()).thenReturn("first");

        underTest.addContent("[abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789]");

        verifyNoMoreInteractions(rootSection, newSection);
    }

    @Test
    void addContentTestWhenSingleSectionButInvalidLevelTransition() {
        when(rootSection.getQualifiedName()).thenReturn("root");

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> underTest.addContent("[[foo]]"));

        assertThat(illegalArgumentException)
                .hasMessage("Invalid section level transition: active level 0, new level 2, active sections 'root', new section '[[foo]]'");

        verifyNoMoreInteractions(rootSection);
    }

    @Test
    void addContentTestWhenSingleSectionTwice() {
        when(rootSection.getQualifiedName()).thenReturn("root");
        IniFileSection newSection = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[foo]")).thenReturn(newSection);
        when(newSection.getQualifiedName()).thenReturn("first");

        underTest.addContent("[foo]\n[foo]");

        verifyNoMoreInteractions(rootSection, newSection);
    }

    @Test
    void addContentTestWhenTwoSectionsSameLevel() {
        when(rootSection.getQualifiedName()).thenReturn("root");
        IniFileSection newSection1 = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[foo]")).thenReturn(newSection1);
        when(newSection1.getQualifiedName()).thenReturn("first");
        IniFileSection newSection2 = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[bar]")).thenReturn(newSection2);
        when(newSection2.getQualifiedName()).thenReturn("second");

        underTest.addContent("[foo]\n[bar]");

        verifyNoMoreInteractions(rootSection, newSection1, newSection2);
    }

    @Test
    void addContentTestWhenTwoSectionsDifferentLevels() {
        when(rootSection.getQualifiedName()).thenReturn("root");
        IniFileSection newSection1 = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[foo]")).thenReturn(newSection1);
        when(newSection1.getQualifiedName()).thenReturn("first");
        IniFileSection newSection2 = mock(IniFileSection.class);
        when(newSection1.getChildSectionOrCreateIfAbsent("[[bar]]")).thenReturn(newSection2);
        when(newSection2.getQualifiedName()).thenReturn("second");

        underTest.addContent("[foo]\n[[bar]]");

        verifyNoMoreInteractions(rootSection, newSection1, newSection2);
    }

    @Test
    void addContentTestWhenThreeSectionsIncreasingLevels() {
        when(rootSection.getQualifiedName()).thenReturn("root");
        IniFileSection newSection1 = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[foo]")).thenReturn(newSection1);
        when(newSection1.getQualifiedName()).thenReturn("first");
        IniFileSection newSection2 = mock(IniFileSection.class);
        when(newSection1.getChildSectionOrCreateIfAbsent("[[bar]]")).thenReturn(newSection2);
        when(newSection2.getQualifiedName()).thenReturn("second");
        IniFileSection newSection3 = mock(IniFileSection.class);
        when(newSection2.getChildSectionOrCreateIfAbsent("[[[dummy]]]")).thenReturn(newSection3);
        when(newSection3.getQualifiedName()).thenReturn("third");

        underTest.addContent("[foo]\n[[ bar]]\n[[[dummy ]]]");

        verifyNoMoreInteractions(rootSection, newSection1, newSection2, newSection3);
    }

    @Test
    void addContentTestWhenThreeSectionsLevelsOneTwoOne() {
        when(rootSection.getQualifiedName()).thenReturn("root");
        IniFileSection newSection1 = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[foo]")).thenReturn(newSection1);
        when(newSection1.getQualifiedName()).thenReturn("first");
        IniFileSection newSection2 = mock(IniFileSection.class);
        when(newSection1.getChildSectionOrCreateIfAbsent("[[bar]]")).thenReturn(newSection2);
        when(newSection2.getQualifiedName()).thenReturn("second");
        IniFileSection newSection3 = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[dummy]")).thenReturn(newSection3);
        when(newSection3.getQualifiedName()).thenReturn("third");

        underTest.addContent("[foo]\n[[bar]]\n[dummy]");

        verifyNoMoreInteractions(rootSection, newSection1, newSection2, newSection3);
    }

    @Test
    void addContentTestWhenFourSectionsLevelsOneTwoThreeTwo() {
        when(rootSection.getQualifiedName()).thenReturn("root");
        IniFileSection newSection1 = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[foo]")).thenReturn(newSection1);
        when(newSection1.getQualifiedName()).thenReturn("first");
        IniFileSection newSection2 = mock(IniFileSection.class);
        when(newSection1.getChildSectionOrCreateIfAbsent("[[bar]]")).thenReturn(newSection2);
        when(newSection2.getQualifiedName()).thenReturn("second");
        IniFileSection newSection3 = mock(IniFileSection.class);
        when(newSection2.getChildSectionOrCreateIfAbsent("[[[dummy]]]")).thenReturn(newSection3);
        when(newSection3.getQualifiedName()).thenReturn("third");
        IniFileSection newSection4 = mock(IniFileSection.class);
        when(newSection1.getChildSectionOrCreateIfAbsent("[[boom]]")).thenReturn(newSection4);
        when(newSection4.getQualifiedName()).thenReturn("fourth");

        underTest.addContent("[foo]\n[[bar]]\n[[[dummy]]]\n[[boom]]");

        verifyNoMoreInteractions(rootSection, newSection1, newSection2, newSection3, newSection4);
    }

    @Test
    void addContentTestWhenFourSectionsLevelsOneTwoThreeOne() {
        when(rootSection.getQualifiedName()).thenReturn("root");
        IniFileSection newSection1 = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[foo]")).thenReturn(newSection1);
        when(newSection1.getQualifiedName()).thenReturn("first");
        IniFileSection newSection2 = mock(IniFileSection.class);
        when(newSection1.getChildSectionOrCreateIfAbsent("[[bar]]")).thenReturn(newSection2);
        when(newSection2.getQualifiedName()).thenReturn("second");
        IniFileSection newSection3 = mock(IniFileSection.class);
        when(newSection2.getChildSectionOrCreateIfAbsent("[[[dummy]]]")).thenReturn(newSection3);
        when(newSection3.getQualifiedName()).thenReturn("third");
        IniFileSection newSection4 = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[boom]")).thenReturn(newSection4);
        when(newSection4.getQualifiedName()).thenReturn("fourth");

        underTest.addContent("[foo]\n[[bar]]\n[[[dummy]]]\n[boom]");

        verifyNoMoreInteractions(rootSection, newSection1, newSection2, newSection3, newSection4);
    }

    @ParameterizedTest(name = "content={0}")
    @ValueSource(strings = {"setting=something", "setting =something", "setting= something", "setting = something", " setting=something ",
            " setting = something "})
    void addContentTestWhenSingleGlobalConfigWithValue(String content) {
        underTest.addContent(content);

        verify(rootSection).addConfig("setting", "something");
        verifyNoMoreInteractions(rootSection);
    }

    @ParameterizedTest(name = "content={0}")
    @ValueSource(strings = {"setting=", "setting= "})
    void addContentTestWhenSingleGlobalConfigNoValue(String content) {
        underTest.addContent(content);

        verify(rootSection).addConfig("setting", "");
        verifyNoMoreInteractions(rootSection);
    }

    @Test
    void addContentTestWhenSingleGlobalConfigWithValueComplex() {
        underTest.addContent("abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789=[something]#text=,'$';{\"\"}");

        verify(rootSection).addConfig("abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789", "[something]#text=,'$';{\"\"}");
        verifyNoMoreInteractions(rootSection);
    }

    @Test
    void addContentTestWhenTwoGlobalConfigs() {
        underTest.addContent("setting=something\nproperty=override");

        verify(rootSection).addConfig("setting", "something");
        verify(rootSection).addConfig("property", "override");
        verifyNoMoreInteractions(rootSection);
    }

    @Test
    void addContentTestWhenFourSectionsLevelsOneTwoThreeOneAndConfigs() {
        when(rootSection.getQualifiedName()).thenReturn("root");
        IniFileSection newSection1 = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[foo]")).thenReturn(newSection1);
        when(newSection1.getQualifiedName()).thenReturn("first");
        IniFileSection newSection2 = mock(IniFileSection.class);
        when(newSection1.getChildSectionOrCreateIfAbsent("[[bar]]")).thenReturn(newSection2);
        when(newSection2.getQualifiedName()).thenReturn("second");
        IniFileSection newSection3 = mock(IniFileSection.class);
        when(newSection2.getChildSectionOrCreateIfAbsent("[[[dummy]]]")).thenReturn(newSection3);
        when(newSection3.getQualifiedName()).thenReturn("third");
        IniFileSection newSection4 = mock(IniFileSection.class);
        when(rootSection.getChildSectionOrCreateIfAbsent("[boom]")).thenReturn(newSection4);
        when(newSection4.getQualifiedName()).thenReturn("fourth");

        underTest.addContent("conf0=val0\n[foo]\nconf1=val1\n[[bar]]\nconf2=val2\nconf3=val3\n[[[dummy]]]\nconf4=val4\n[boom]\nconf5=val5\n[foo]\nconf6=val6");

        verify(rootSection).addConfig("conf0", "val0");
        verify(newSection1).addConfig("conf1", "val1");
        verify(newSection2).addConfig("conf2", "val2");
        verify(newSection2).addConfig("conf3", "val3");
        verify(newSection3).addConfig("conf4", "val4");
        verify(newSection4).addConfig("conf5", "val5");
        verify(newSection1).addConfig("conf6", "val6");
        verifyNoMoreInteractions(rootSection, newSection1, newSection2, newSection3, newSection4);
    }

    @ParameterizedTest(name = "content={0}")
    @ValueSource(strings = {"[foo", "[foo[", "[]", "[@]", "[foo@bar]", "[foo]bar", "[foo][bar]", "[foo][[bar]]", "[ [foo] ]", "=", "=foo", "foo", "[foo#]",
            "setting[=something", "setting#=something", "[foo]setting=something"})
    void addContentTestWhenInvalidLine(String content) {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> underTest.addContent(content));

        assertThat(illegalArgumentException).hasMessage("Invalid line: " + content);

        verifyNoInteractions(rootSection);
    }

}