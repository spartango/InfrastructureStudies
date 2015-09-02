package com.irislabs.stream;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.irislabs.sheet.SheetEntry;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Author: spartango
 * Date: 5/2/14
 * Time: 5:30 PM.
 */
public class MultiCollectors {
    private static Set<Collector.Characteristics> characteristics = EnumSet.of(Collector.Characteristics.UNORDERED,
                                                                               Collector.Characteristics.IDENTITY_FINISH);

    public static <A, D>
    Collector<SheetEntry, ?, Map<String, D>> groupingByKey(String key,
                                                           Collector<SheetEntry, A, D> downstream) {
        return Collectors.groupingBy(entry -> entry.get(key), downstream);
    }

    public static <T, K> Collector<T, ?, Multimap<K, T>> toMultimap(Function<? super T, ? extends K> keyMapper) {
        return toMultimap(keyMapper, Function.identity());
    }

    public static <T>
    Collector<T, ?, DescriptiveStatistics> descriptiveSummarizing(ToDoubleFunction<? super T> mapper) {
        return new Collector<T, DescriptiveStatistics, DescriptiveStatistics>() {
            @Override public Supplier<DescriptiveStatistics> supplier() {
                return DescriptiveStatistics::new;
            }

            @Override public BiConsumer<DescriptiveStatistics, T> accumulator() {
                return (stats, target) -> stats.addValue(mapper.applyAsDouble(target));
            }

            @Override public BinaryOperator<DescriptiveStatistics> combiner() {
                return (stats, secondStats) -> {
                    DoubleStream.of(secondStats.getValues()).forEach(stats::addValue);
                    return stats;
                };
            }

            @Override public Function<DescriptiveStatistics, DescriptiveStatistics> finisher() {
                return Function.identity();
            }

            @Override public Set<Characteristics> characteristics() {
                return characteristics;
            }
        };
    }


    public static <T, K, U> Collector<T, ?, Multimap<K, U>> toMultimap(Function<? super T, ? extends K> keyMapper,
                                                                       Function<? super T, ? extends U> valueMapper) {
        return new Collector<T, Multimap<K, U>, Multimap<K, U>>() {
            @Override public Supplier<Multimap<K, U>> supplier() {
                return LinkedListMultimap::create;
            }

            @Override public BiConsumer<Multimap<K, U>, T> accumulator() {
                return (map, entry) -> map.put(keyMapper.apply(entry), valueMapper.apply(entry));
            }

            @Override public BinaryOperator<Multimap<K, U>> combiner() {
                return (firstMap, secondMap) -> {
                    firstMap.putAll(secondMap);
                    return firstMap;
                };
            }

            @Override public Function<Multimap<K, U>, Multimap<K, U>> finisher() {
                return Function.identity();
            }

            @Override public Set<Characteristics> characteristics() {
                return characteristics;
            }
        };
    }

    public static Collector<Boolean, AtomicBoolean, Boolean> andCollector() {
        return new Collector<Boolean, AtomicBoolean, Boolean>() {
            @Override public Supplier<AtomicBoolean> supplier() {
                return () -> new AtomicBoolean(true);
            }

            @Override public BiConsumer<AtomicBoolean, Boolean> accumulator() {
                return (first, second) -> first.set(first.get() && second);
            }

            @Override public BinaryOperator<AtomicBoolean> combiner() {
                return (first, second) -> new AtomicBoolean(first.get() && second.get());
            }

            @Override public Function<AtomicBoolean, Boolean> finisher() {
                return AtomicBoolean::get;
            }

            @Override public Set<Characteristics> characteristics() {
                return EnumSet.of(Collector.Characteristics.UNORDERED, Characteristics.CONCURRENT);
            }
        };
    }

    public static Collector<Boolean, AtomicBoolean, Boolean> orCollector() {
        return new Collector<Boolean, AtomicBoolean, Boolean>() {
            @Override public Supplier<AtomicBoolean> supplier() {
                return () -> new AtomicBoolean(false);
            }

            @Override public BiConsumer<AtomicBoolean, Boolean> accumulator() {
                return (first, second) -> first.set(first.get() || second);
            }

            @Override public BinaryOperator<AtomicBoolean> combiner() {
                return (first, second) -> new AtomicBoolean(first.get() || second.get());
            }

            @Override public Function<AtomicBoolean, Boolean> finisher() {
                return AtomicBoolean::get;
            }

            @Override public Set<Characteristics> characteristics() {
                return EnumSet.of(Collector.Characteristics.UNORDERED, Characteristics.CONCURRENT);
            }
        };
    }

}
