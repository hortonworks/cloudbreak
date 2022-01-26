package com.sequenceiq.cloudbreak.util.password;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.prng.SP800SecureRandomBuilder;

public class PasswordGenerator {

    private static final int PREFIX_LENGTH = 3;

    private static final int PART_LENGTH = 8;

    private static final PasswordGenerator DEFAULT_PASSWORD_GENERATOR = builder()
            .prefix(CharSet.ALPHABETIC_LOWER_CASE, PREFIX_LENGTH)
            .mandatoryCharacters(CharSet.SAFE_SPECIAL_CHARACTERS)
            .bodyPart(CharSet.ALPHABETIC_LOWER_CASE, PART_LENGTH)
            .bodyPart(CharSet.ALPHABETIC_UPPER_CASE, PART_LENGTH)
            .bodyPart(CharSet.NUMERIC, PART_LENGTH)
            .randomSupplier(() -> new SP800SecureRandomBuilder(new SecureRandom(), false)
                    .setPersonalizationString(Long.toString(System.nanoTime()).getBytes())
                    .buildHash(new SHA512Digest(), null, false))
            .build();

    private final Optional<PasswordPart> prefixPart;

    private final List<PasswordPart> bodyParts;

    private final Optional<CharSet> mandatoryCharacters;

    private final Supplier<SecureRandom> randomSupplier;

    private PasswordGenerator(
            Optional<PasswordPart> prefixPart,
            List<PasswordPart> bodyParts,
            Optional<CharSet> mandatoryCharacters,
            Supplier<SecureRandom> randomSupplier) {
        this.prefixPart = prefixPart;
        this.bodyParts = bodyParts;
        this.mandatoryCharacters = mandatoryCharacters;
        this.randomSupplier = randomSupplier;
    }

    public static PasswordGenerator getDefault() {
        return DEFAULT_PASSWORD_GENERATOR;
    }

    public String generate() {
        SecureRandom random = randomSupplier.get();
        StringBuilder password = new StringBuilder();
        if (prefixPart.isPresent()) {
            prefixPart.get().generateRandomCharacters(random).forEach(password::append);
        }
        List<Character> body = new ArrayList<>();
        if (mandatoryCharacters.isPresent()) {
            body.addAll(mandatoryCharacters.get().getValues());
        }
        for (PasswordPart passwordPart : bodyParts) {
            body.addAll(passwordPart.generateRandomCharacters(random));
        }

        Collections.shuffle(body, random);
        body.forEach(password::append);
        return password.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Optional<PasswordPart> prefixPart = Optional.empty();

        private List<PasswordPart> bodyParts = new ArrayList<>();

        private Optional<CharSet> mandatoryCharacters = Optional.empty();

        private Supplier<SecureRandom> randomSupplier;

        public Builder prefix(CharSet charSet, int length) {
            prefixPart = Optional.of(new PasswordPart(charSet, length));
            return this;
        }

        public Builder mandatoryCharacters(CharSet charSet) {
            mandatoryCharacters = Optional.of(charSet);
            return this;
        }

        public Builder bodyPart(CharSet charSet, int length) {
            bodyParts.add(new PasswordPart(charSet, length));
            return this;
        }

        public Builder randomSupplier(Supplier<SecureRandom> randomSupplier) {
            this.randomSupplier = randomSupplier;
            return this;
        }

        public PasswordGenerator build() {
            return new PasswordGenerator(
                    prefixPart,
                    bodyParts,
                    mandatoryCharacters,
                    randomSupplier
            );
        }
    }
}
