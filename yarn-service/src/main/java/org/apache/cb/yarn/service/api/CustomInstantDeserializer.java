package org.apache.cb.yarn.service.api;

import java.io.IOException;
import java.math.BigDecimal;

import org.threeten.bp.DateTimeException;
import org.threeten.bp.Instant;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalAccessor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.threetenbp.DateTimeUtils;
import com.fasterxml.jackson.datatype.threetenbp.DecimalUtils;
import com.fasterxml.jackson.datatype.threetenbp.deser.ThreeTenDateTimeDeserializerBase;
import com.fasterxml.jackson.datatype.threetenbp.function.BiFunction;
import com.fasterxml.jackson.datatype.threetenbp.function.Function;

/**
 * Deserializer for ThreeTen temporal {@link Instant}s, {@link OffsetDateTime}, and {@link ZonedDateTime}s.
 * Adapted from the jackson threetenbp InstantDeserializer to add support for deserializing rfc822 format.
 *
 * @author Nick Williams
 */
public class CustomInstantDeserializer<T extends Temporal> extends ThreeTenDateTimeDeserializerBase<T> {
    public static final CustomInstantDeserializer<Instant> INSTANT = new CustomInstantDeserializer<>(
            Instant.class, DateTimeFormatter.ISO_INSTANT,
            new Function<TemporalAccessor, Instant>() {
                @Override
                public Instant apply(TemporalAccessor temporalAccessor) {
                    return Instant.from(temporalAccessor);
                }
            },
            new Function<FromIntegerArguments, Instant>() {
                @Override
                public Instant apply(FromIntegerArguments a) {
                    return Instant.ofEpochMilli(a.value);
                }
            },
            new Function<FromDecimalArguments, Instant>() {
                @Override
                public Instant apply(FromDecimalArguments a) {
                    return Instant.ofEpochSecond(a.integer, a.fraction);
                }
            },
            null
    );

    public static final CustomInstantDeserializer<OffsetDateTime> OFFSET_DATE_TIME = new CustomInstantDeserializer<>(
            OffsetDateTime.class, DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            new Function<TemporalAccessor, OffsetDateTime>() {
                @Override
                public OffsetDateTime apply(TemporalAccessor temporalAccessor) {
                    return OffsetDateTime.from(temporalAccessor);
                }
            },
            new Function<FromIntegerArguments, OffsetDateTime>() {
                @Override
                public OffsetDateTime apply(FromIntegerArguments a) {
                    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(a.value), a.zoneId);
                }
            },
            new Function<FromDecimalArguments, OffsetDateTime>() {
                @Override
                public OffsetDateTime apply(FromDecimalArguments a) {
                    return OffsetDateTime.ofInstant(Instant.ofEpochSecond(a.integer, a.fraction), a.zoneId);
                }
            },
            new BiFunction<OffsetDateTime, ZoneId, OffsetDateTime>() {
                @Override
                public OffsetDateTime apply(OffsetDateTime d, ZoneId z) {
                    return d.withOffsetSameInstant(z.getRules().getOffset(d.toLocalDateTime()));
                }
            }
    );

    public static final CustomInstantDeserializer<ZonedDateTime> ZONED_DATE_TIME = new CustomInstantDeserializer<>(
            ZonedDateTime.class, DateTimeFormatter.ISO_ZONED_DATE_TIME,
            new Function<TemporalAccessor, ZonedDateTime>() {
                @Override
                public ZonedDateTime apply(TemporalAccessor temporalAccessor) {
                    return ZonedDateTime.from(temporalAccessor);
                }
            },
            new Function<FromIntegerArguments, ZonedDateTime>() {
                @Override
                public ZonedDateTime apply(FromIntegerArguments a) {
                    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(a.value), a.zoneId);
                }
            },
            new Function<FromDecimalArguments, ZonedDateTime>() {
                @Override
                public ZonedDateTime apply(FromDecimalArguments a) {
                    return ZonedDateTime.ofInstant(Instant.ofEpochSecond(a.integer, a.fraction), a.zoneId);
                }
            },
            new BiFunction<ZonedDateTime, ZoneId, ZonedDateTime>() {
                @Override
                public ZonedDateTime apply(ZonedDateTime zonedDateTime, ZoneId zoneId) {
                    return zonedDateTime.withZoneSameInstant(zoneId);
                }
            }
    );

    private static final long serialVersionUID = 1L;

    private final Function<FromIntegerArguments, T> fromMilliseconds;

    private final Function<FromDecimalArguments, T> fromNanoseconds;

    private final Function<TemporalAccessor, T> parsedToValue;

    private final BiFunction<T, ZoneId, T> adjust;

    protected CustomInstantDeserializer(Class<T> supportedType,
            DateTimeFormatter parser,
            Function<TemporalAccessor, T> parsedToValue,
            Function<FromIntegerArguments, T> fromMilliseconds,
            Function<FromDecimalArguments, T> fromNanoseconds,
            BiFunction<T, ZoneId, T> adjust) {
        super(supportedType, parser);
        this.parsedToValue = parsedToValue;
        this.fromMilliseconds = fromMilliseconds;
        this.fromNanoseconds = fromNanoseconds;
        this.adjust = adjust == null ? new BiFunction<T, ZoneId, T>() {
            @Override
            public T apply(T t, ZoneId zoneId) {
                return t;
            }
        } : adjust;
    }

    @SuppressWarnings("unchecked")
    protected CustomInstantDeserializer(CustomInstantDeserializer<T> base, DateTimeFormatter f) {
        super((Class<T>) base.handledType(), f);
        parsedToValue = base.parsedToValue;
        fromMilliseconds = base.fromMilliseconds;
        fromNanoseconds = base.fromNanoseconds;
        adjust = base.adjust;
    }

    @Override
    protected JsonDeserializer<T> withDateFormat(DateTimeFormatter dtf) {
        if (dtf == _formatter) {
            return this;
        }
        return new CustomInstantDeserializer<>(this, dtf);
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        //NOTE: Timestamps contain no timezone info, and are always in configured TZ. Only
        //string values have to be adjusted to the configured TZ.
        int i = parser.getCurrentTokenId();
        if (i == JsonTokenId.ID_NUMBER_FLOAT) {
            BigDecimal decimalValue = parser.getDecimalValue();
            long seconds = decimalValue.longValue();
            int nanoseconds = DecimalUtils.extractNanosecondDecimal(decimalValue, seconds);
            return fromNanoseconds.apply(new FromDecimalArguments(
                    seconds, nanoseconds, getZone(context)));
        } else if (i == JsonTokenId.ID_NUMBER_INT) {
            long timestamp = parser.getLongValue();
            if (context.isEnabled(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)) {
                return fromNanoseconds.apply(new FromDecimalArguments(
                        timestamp, 0, getZone(context)
                ));
            }
            return fromMilliseconds.apply(new FromIntegerArguments(
                    timestamp, getZone(context)
            ));
        } else if (i == JsonTokenId.ID_STRING) {
            String trimmedText = parser.getText().trim();
            if (trimmedText.isEmpty()) {
                return null;
            }
            String end = "+0000";
            if (trimmedText.endsWith(end)) {
                trimmedText = trimmedText.substring(0, trimmedText.length() - end.length()) + 'Z';
            }
            T value;
            try {
                TemporalAccessor acc = _formatter.parse(trimmedText);
                value = parsedToValue.apply(acc);
                if (context.isEnabled(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)) {
                    return adjust.apply(value, getZone(context));
                }
            } catch (DateTimeException e) {
                throw _peelDTE(e);
            }
            return value;
        }
        throw context.mappingException("Expected type float, integer, or string.");
    }

    private ZoneId getZone(DeserializationContext context) {
        // Instants are always in UTC, so don't waste compute cycles
        return (_valueClass == Instant.class) ? null : DateTimeUtils.timeZoneToZoneId(context.getTimeZone());
    }

    private static class FromIntegerArguments {
        private final long value;

        private final ZoneId zoneId;

        private FromIntegerArguments(long value, ZoneId zoneId) {
            this.value = value;
            this.zoneId = zoneId;
        }
    }

    private static class FromDecimalArguments {
        private final long integer;

        private final int fraction;

        private final ZoneId zoneId;

        private FromDecimalArguments(long integer, int fraction, ZoneId zoneId) {
            this.integer = integer;
            this.fraction = fraction;
            this.zoneId = zoneId;
        }
    }
}
