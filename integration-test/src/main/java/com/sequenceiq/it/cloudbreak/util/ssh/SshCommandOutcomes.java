package com.sequenceiq.it.cloudbreak.util.ssh;

import static java.lang.String.format;
import static java.util.function.Predicate.not;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

/**
 * The outcomes of an SSH command executed on multiple instances, in the order the instances were given.
 */
public record SshCommandOutcomes(List<SshCommandOutcome> outcomes) implements Iterable<SshCommandOutcome> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshCommandOutcomes.class);

    private static final String DEFAULT_OPERATION = "SSH command";

    public SshCommandOutcomes {
        outcomes = List.copyOf(outcomes);
    }

    /**
     * Fails the test if the command could not be executed on any of the instances because of an SSH connection or authentication failure, or
     * because it timed out.
     *
     * @return these outcomes
     */
    public SshCommandOutcomes requireAllExecuted() {
        return requireAllExecuted(DEFAULT_OPERATION);
    }

    /**
     * Fails the test if the command could not be executed on any of the instances because of an SSH connection or authentication failure, or
     * because it timed out.
     *
     * @param operation the description of what the command was meant to do, used as the subject of the failure message
     * @return these outcomes
     */
    public SshCommandOutcomes requireAllExecuted(String operation) {
        List<SshCommandOutcome> notExecuted = outcomes.stream().filter(not(SshCommandOutcome::executed)).toList();
        if (!notExecuted.isEmpty()) {
            String failures = notExecuted.stream()
                    .map(outcome -> format("'%s' (%s: %s)", outcome.instanceIp(), outcome.result(), outcome.exceptionMessage().orElse(null)))
                    .collect(Collectors.joining(", "));
            String message = format("%s could not be executed on %d instance(s): %s", operation, notExecuted.size(), failures);
            Log.error(LOGGER, message);
            throw new TestFailException(message, notExecuted.getFirst().throwable());
        }
        return this;
    }

    /**
     * Fails the test if the command could not be executed on any of the instances, or returned a non-zero (or missing) exit code on any of them.
     *
     * @return these outcomes
     */
    public SshCommandOutcomes requireAllSucceeded() {
        return requireAllSucceeded(DEFAULT_OPERATION);
    }

    /**
     * Fails the test if the command could not be executed on any of the instances, or returned a non-zero (or missing) exit code on any of them.
     *
     * @param operation the description of what the command was meant to do, used as the subject of the failure message
     * @return these outcomes
     */
    public SshCommandOutcomes requireAllSucceeded(String operation) {
        requireAllExecuted(operation);
        List<SshCommandOutcome> failed = outcomes.stream().filter(not(SshCommandOutcome::returnCodeSuccess)).toList();
        if (!failed.isEmpty()) {
            String failures = failed.stream()
                    .map(outcome -> format("'%s' (exit code: %d, output: %s)", outcome.instanceIp(), outcome.returnCode(), outcome.commandOutput()))
                    .collect(Collectors.joining(", "));
            String message = format("%s failed on %d instance(s): %s", operation, failed.size(), failures);
            Log.error(LOGGER, message);
            throw new TestFailException(message);
        }
        return this;
    }

    public Map<String, SshCommandOutcome> toMapByInstanceIp() {
        return toMapByInstanceIp(Function.identity());
    }

    /**
     * Maps each outcome by the IP of its instance, keeping the order of the outcomes.
     *
     * @param valueMapper creates the map value from an outcome, it may return {@code null}
     * @throws IllegalArgumentException if multiple outcomes belong to the same instance
     */
    public <V> Map<String, V> toMapByInstanceIp(Function<SshCommandOutcome, V> valueMapper) {
        return toMapByInstanceIp(outcome -> true, valueMapper);
    }

    /**
     * Maps the outcomes matching the filter by the IP of their instance, keeping the order of the outcomes.
     *
     * @param filter      selects the outcomes to put into the map
     * @param valueMapper creates the map value from an outcome, it may return {@code null}
     * @throws IllegalArgumentException if multiple selected outcomes belong to the same instance
     */
    public <V> Map<String, V> toMapByInstanceIp(Predicate<SshCommandOutcome> filter, Function<SshCommandOutcome, V> valueMapper) {
        Map<String, V> outcomesByInstanceIp = new LinkedHashMap<>();
        outcomes.stream()
                .filter(filter)
                .forEach(outcome -> {
                    if (outcomesByInstanceIp.containsKey(outcome.instanceIp())) {
                        throw new IllegalArgumentException(format("Multiple SSH command outcomes belong to '%s' instance", outcome.instanceIp()));
                    }
                    outcomesByInstanceIp.put(outcome.instanceIp(), valueMapper.apply(outcome));
                });
        return outcomesByInstanceIp;
    }

    public Stream<SshCommandOutcome> stream() {
        return outcomes.stream();
    }

    @Override
    public Iterator<SshCommandOutcome> iterator() {
        return outcomes.iterator();
    }
}
