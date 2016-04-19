package com.irislabs.stream;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.irislabs.sheet.SheetEntry;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
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

    public static <T, K> Collector<T, ?, Map<K, Long>> toHistogram(Function<? super T, ? extends K> keyMapper) {
        return new Collector<T, Map<K, LongAdder>, Map<K, Long>>() {
            @Override public Supplier<Map<K, LongAdder>> supplier() {
                return ConcurrentHashMap::new;
            }

            @Override public BiConsumer<Map<K, LongAdder>, T> accumulator() {
                return (map, entry) -> {
                    K key = keyMapper.apply(entry);
                    // If the key is not there
                    map.computeIfAbsent(key, (k -> {
                        return new LongAdder();
                    }));

                    // If/when the key is there
                    map.computeIfPresent(key, (k, count) -> {
                        count.increment();
                        return count;
                    });

                };
            }

            @Override public BinaryOperator<Map<K, LongAdder>> combiner() {
                return (map, secondMap) -> {
                    secondMap.forEach((key, value) -> {
                        // If the key is not there
                        map.computeIfAbsent(key, (k -> {
                            final LongAdder count = new LongAdder();
                            count.increment();
                            return count;
                        }));

                        // If the key is there
                        map.computeIfPresent(key, (k, count) -> {
                            count.add(value.sum());
                            return count;
                        });
                    });
                    return map;
                };
            }

            @Override public Function<Map<K, LongAdder>, Map<K, Long>> finisher() {
                return (map -> map.entrySet()
                                  .stream()
                                  .collect(Collectors.toMap(Map.Entry::getKey,
                                                            entry -> entry.getValue().sum())));

            }

            @Override public Set<Characteristics> characteristics() {
                return EnumSet.of(Collector.Characteristics.UNORDERED, Characteristics.CONCURRENT);
            }
        };
    }

    public static <T, K> Collector<T, Map<K, DoubleAdder>, Map<K, Double>> toHistogram(Function<? super T, ? extends K> keyMapper,
                                                                                       ToDoubleFunction valueMapper) {
        return new Collector<T, Map<K, DoubleAdder>, Map<K, Double>>() {
            @Override public Supplier<Map<K, DoubleAdder>> supplier() {
                return ConcurrentHashMap::new;
            }

            @Override public BiConsumer<Map<K, DoubleAdder>, T> accumulator() {
                return (map, entry) -> {
                    K key = keyMapper.apply(entry);
                    // If the key is not there
                    map.computeIfAbsent(key, (k -> new DoubleAdder()));

                    // If/when the key is there
                    map.computeIfPresent(key, (k, count) -> {
                        count.add(valueMapper.applyAsDouble(entry));
                        return count;
                    });

                };
            }

            @Override public BinaryOperator<Map<K, DoubleAdder>> combiner() {
                return (map, secondMap) -> {
                    secondMap.forEach((key, value) -> {
                        // If the key is not there
                        map.computeIfAbsent(key, (k -> new DoubleAdder()));

                        // If the key is there
                        map.computeIfPresent(key, (k, count) -> {
                            count.add(value.sum());
                            return count;
                        });
                    });
                    return map;
                };
            }

            @Override public Function<Map<K, DoubleAdder>, Map<K, Double>> finisher() {
                return (map -> map.entrySet()
                                  .stream()
                                  .collect(Collectors.toMap(Map.Entry::getKey,
                                                            entry -> entry.getValue().sum())));

            }

            @Override public Set<Characteristics> characteristics() {
                return EnumSet.of(Collector.Characteristics.UNORDERED, Characteristics.CONCURRENT);
            }
        };
    }

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
                return EnumSet.of(Characteristics.UNORDERED, Characteristics.CONCURRENT);
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
                return EnumSet.of(Characteristics.UNORDERED, Characteristics.CONCURRENT);
            }
        };
    }

    public static <K, V> Map<K, Long> toHistogram(Multimap<K, V> target) {
        return target.asMap()
                     .entrySet()
                     .stream()
                     .collect(Collectors.toMap(Map.Entry::getKey,
                                               entry -> (long) entry.getValue().size()));
    }
}
