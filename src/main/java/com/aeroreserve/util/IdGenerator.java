package com.aeroreserve.util;

import java.util.concurrent.atomic.AtomicLong;

/** Generates short, human-readable, monotonically unique identifiers. */
public final class IdGenerator {

    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis() % 100_000);

    private IdGenerator() {
    }

    public static String generate(String prefix) {
        long sequence = COUNTER.incrementAndGet();
        return prefix + "-" + Long.toString(sequence, 36).toUpperCase();
    }
}
