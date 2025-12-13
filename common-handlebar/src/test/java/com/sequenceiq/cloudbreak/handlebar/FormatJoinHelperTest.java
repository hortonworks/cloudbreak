package com.sequenceiq.cloudbreak.handlebar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.jknack.handlebars.Options;

@ExtendWith(MockitoExtension.class)
class FormatJoinHelperTest {

    @Mock
    private Options options;

    @Test
    void formatsEachItem() {
        Collection<?> items = Arrays.asList("A", "B");
        separatorIs(";");
        formatStringIs("prefix:%s:suffix");

        assertEquals("prefix:A:suffix;prefix:B:suffix", FormatJoinHelper.INSTANCE.apply(items, options));
    }

    @Test
    void noSeparatorForSingleItem() {
        Collection<?> items = Collections.singleton("host");
        separatorIs(",");
        formatStringIs("http://%s:8080");

        assertEquals("http://host:8080", FormatJoinHelper.INSTANCE.apply(items, options));
    }

    @Test
    void emptyResultForNoItems() {
        assertEquals("", FormatJoinHelper.INSTANCE.apply(Collections.emptySet(), options));
    }

    private void formatStringIs(String format) {
        when(options.hash("format", null)).thenReturn(format);
    }

    private void separatorIs(String separator) {
        when(options.hash("sep", ",")).thenReturn(separator);
    }

}
