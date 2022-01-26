package com.sequenceiq.cloudbreak.util.password;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

public class CharSet {

    public static final CharSet ALPHABETIC_LOWER_CASE = fromString("abcdefghijklmnopqrstuvwxyz");

    public static final CharSet ALPHABETIC_UPPER_CASE = fromString("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    public static final CharSet NUMERIC = fromString("0123456789");

    public static final CharSet SAFE_SPECIAL_CHARACTERS = fromString("?.-_+");

    private final List<Character> values;

    public CharSet(Collection<Character> values) {
        Preconditions.checkNotNull(values);
        Preconditions.checkArgument(!values.isEmpty(), "Character set must contain at least one value.");
        this.values = List.copyOf(new LinkedHashSet<>(values));
    }

    public static CharSet fromString(String characters) {
        return new CharSet(characters.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList()));
    }

    public List<Character> getValues() {
        return values;
    }
}
