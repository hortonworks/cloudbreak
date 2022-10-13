package com.sequenceiq.freeipa.service.stack;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class MultimapCollector<T, A> implements Collector<T, A, A> {

    static final Set<Collector.Characteristics> CH_ID
            = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));

    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, A> finisher;
    private final Set<Characteristics> characteristics = CH_ID;

    MultimapCollector(Supplier<A> supplier,
                      BiConsumer<A, T> accumulator,
                      BinaryOperator<A> combiner) {
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.finisher = Function.identity();
    }

    public static <T, K, U, M extends Multimap<K, U>>
    MultimapCollector<T, M> toMultiMap(Function<? super T, ? extends K> keyMapper,
                                       Function<? super T, ? extends U> valueMapper,
                                       Supplier<M> mapSupplier) {
        BiConsumer<M, T> accumulator = (map, element) -> map.put(keyMapper.apply(element), valueMapper.apply(element));
        return new MultimapCollector<>(mapSupplier, accumulator,
                (kvMultimap, kvMultimap2) -> {
                    kvMultimap.putAll(kvMultimap2);
                    return kvMultimap;
                });
    }

    public static <T, K, U>
    MultimapCollector<T, HashMultimap<K, U>> toHashMultiMap(Function<? super T, ? extends K> keyMapper,
                                                            Function<? super T, ? extends U> valueMapper) {
        return toMultiMap(keyMapper, valueMapper, HashMultimap::create);
    }

    public static <T, K, U>
    MultimapCollector<T, LinkedHashMultimap<K, U>> toLinkedHashMultiMap(Function<? super T, ? extends K> keyMapper,
                                                                        Function<? super T, ? extends U> valueMapper) {
        return toMultiMap(keyMapper, valueMapper, LinkedHashMultimap::create);
    }

    public static <T, K, U>
    MultimapCollector<T, ArrayListMultimap<K, U>> toArrayListMultimap(Function<? super T, ? extends K> keyMapper,
                                                                      Function<? super T, ? extends U> valueMapper) {
        return toMultiMap(keyMapper, valueMapper, ArrayListMultimap::create);
    }

    public static <T, K extends Comparable<K>, U extends Comparable<U>>
    MultimapCollector<T, TreeMultimap<K, U>> toTreeMultiMap(Function<? super T, ? extends K> keyMapper,
                                                            Function<? super T, ? extends U> valueMapper) {
        return toMultiMap(keyMapper, valueMapper, TreeMultimap::create);
    }

    @Override
    public Supplier<A> supplier() {
        return supplier;
    }

    @Override
    public BiConsumer<A, T> accumulator() {
        return accumulator;
    }

    @Override
    public BinaryOperator<A> combiner() {
        return combiner;
    }

    @Override
    public Function<A, A> finisher() {
        return finisher;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return characteristics;
    }
}