package com.sequenceiq.periscope.utils;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.scheduling.support.CronSequenceGenerator;

import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.DateTimeService;

@RunWith(Parameterized.class)
public class CronTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private DateTimeService dateTimeService;

    @InjectMocks
    private final DateService underTest = new DateService();

    private final String input;

    private final Optional<String> expected;

    private final Optional<Class<? extends Exception>> exception;

    public CronTest(String input, String expected, Class<? extends Exception> exception) {
        this.input = input;
        this.expected = Optional.ofNullable(expected);
        this.exception = Optional.ofNullable(exception);
    }

    @Parameters(name = "{index}: cronExpressionTest({0})={1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "* * * * *",              "* * * * * ?",              null },
                { "00 59 23 31 DEC Fri",    "00 59 23 31 DEC Fri",      null },
                { "00 45 17 7 6 *",         "00 45 17 7 6 *",           null },
                { "* * * 1,3,5,7,9,11 * *", "* * * 1,3,5,7,9,11 * *",   null },
                { "0 9 1-7 * 1 *",          "0 9 1-7 * 1 *",            null },
                { "0 0 1 * * *",            "0 0 1 * * *",              null },
                { "* 0-11 * * *",           "* 0-11 * * * ?",           null },
                { "* * * 1,2,3 * *",        "* * * 1,2,3 * *",          null },
                { "0 0 * * * *",            "0 0 * * * *",              null },
                { "0 0 * * 3 *",            "0 0 * * 3 *",              null },
                { "00 59 23 31 12 5",       "00 59 23 31 12 5",         null },
                { "45 17 7 6 * *",          "45 17 7 6 * *",            null },
                { "0 12 * * 1-5 *",         "0 12 * * 1-5 *",           null },
                { "* * * 1,3,5,7,9,11 * *", "* * * 1,3,5,7,9,11 * *",   null },
                { "0 9 1-7 * 1 *",          "0 9 1-7 * 1 *",            null },
                { "0 0 1 * * *",            "0 0 1 * * *",              null },
                { "* 0-11 * * * *",         "* 0-11 * * * *",           null },
                { "* * * 1,2,3 * *",        "* * * 1,2,3 * *",          null },
                { "0 0 * * * *",            "0 0 * * * *",              null },
                { "0 0 * * 3 *",            "0 0 * * 3 *",              null },
                { "0 8 * * 5",              "0 8 * * 5 ?",              null },
                { "0 8 * *",                "0 8 * * * ?",                null },
                { "* * * Jan,Feb,Mar * *",                  null, ParseException.class },
                { "0,15,30,45 0,6,12,18 1,15,31 * * *",     null, ParseException.class },
                { "1,2,3,5,20-25,30-35,59 23 31 12 * *",    null, ParseException.class },
                { "0,15,30,45 0,6,12,18 1,15,31 * 1-5 *",   null, ParseException.class },
                { "*/15 */6 1,15,31 * 1-5 *",               null, ParseException.class },
                { "0 12 * * 1-5 * (0 12 * * Mon-Fri *)",    null, ParseException.class },
                { "0 8 * ",                                 null, ParseException.class },
                { "0 0 ! * * ?",                            null, ParseException.class },
                { "0 8",                                    null, ParseException.class }
        });
    }

    @Test
    public void test() throws ParseException {
        if (exception.isPresent()) {
            thrown.expect(exception.get());
        }
        CronSequenceGenerator cronExpression = underTest.getCronExpression(input);
        if (!exception.isPresent()) {
            Assert.assertEquals(String.format("CronSequenceGenerator: %s", expected.orElse("This should be null")), cronExpression.toString());
        }
    }
}
