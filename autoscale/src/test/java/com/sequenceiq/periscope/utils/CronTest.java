package com.sequenceiq.periscope.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;
import java.util.Arrays;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.support.CronSequenceGenerator;

import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.DateTimeService;

@ExtendWith(MockitoExtension.class)
public class CronTest {

    @Mock
    private DateTimeService dateTimeService;

    @InjectMocks
    private final DateService underTest = new DateService();

    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"* * * * * ?", "* * * * * ?", null},
                {"00 59 23 31 DEC Fri", "00 59 23 31 DEC Fri", null},
                {"00 45 17 7 6 *", "00 45 17 7 6 *", null},
                {"* * * 1,3,5,7,9,11 * *", "* * * 1,3,5,7,9,11 * *", null},
                {"0 9 1-7 * 1 *", "0 9 1-7 * 1 *", null},
                {"0 0 1 * * *", "0 0 1 * * *", null},
                {"* 0-11 * * * ?", "* 0-11 * * * ?", null},
                {"* * * 1,2,3 * *", "* * * 1,2,3 * *", null},
                {"0 0 * * * *", "0 0 * * * *", null},
                {"0 0 * * 3 *", "0 0 * * 3 *", null},
                {"00 59 23 31 12 5", "00 59 23 31 12 5", null},
                {"45 17 7 6 * *", "45 17 7 6 * *", null},
                {"0 12 * * 1-5 *", "0 12 * * 1-5 *", null},
                {"* * * 1,3,5,7,9,11 * *", "* * * 1,3,5,7,9,11 * *", null},
                {"0 9 1-7 * 1 *", "0 9 1-7 * 1 *", null},
                {"0 0 1 * * *", "0 0 1 * * *", null},
                {"* 0-11 * * * *", "* 0-11 * * * *", null},
                {"* * * 1,2,3 * *", "* * * 1,2,3 * *", null},
                {"0 0 * * * *", "0 0 * * * *", null},
                {"0 0 * * 3 *", "0 0 * * 3 *", null},
                {"0 8 * * 5 ?", "0 8 * * 5 ?", null},
                {"0 8 * * * ?", "0 8 * * * ?", null},
                {"* * * Jan,Feb,Mar * *", null, ParseException.class},
                {"0,15,30,45 0,6,12,18 1,15,31 * * *", null, ParseException.class},
                {"1,2,3,5,20-25,30-35,59 23 31 12 * *", null, ParseException.class},
                {"0,15,30,45 0,6,12,18 1,15,31 * 1-5 *", null, ParseException.class},
                {"*/15 */6 1,15,31 * 1-5 *", null, ParseException.class},
                {"0 12 * * 1-5 * (0 12 * * Mon-Fri *)", null, ParseException.class},
                {"0 8 * ", null, ParseException.class},
                {"0 0 ! * * ?", null, ParseException.class},
                {"0 8", null, ParseException.class}
        });
    }

    @ParameterizedTest(name = "state = {0} internalTenant = {1} expectedShouldPopulate = {2}")
    @MethodSource("data")
    public void test(String input, String expected, Class<? extends Exception> exception) throws ParseException {

        if (expected != null) {
            CronSequenceGenerator cronExpression = underTest.getCronExpression(input, "UTC");
            assertEquals(String.format("CronSequenceGenerator: %s", expected), cronExpression.toString());
        }

        if (exception != null) {
            assertThrows(exception, () -> underTest.getCronExpression(input, "UTC"));
        }
    }
}
