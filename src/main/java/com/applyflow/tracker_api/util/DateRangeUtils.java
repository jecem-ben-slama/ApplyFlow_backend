package com.applyflow.tracker_api.util;

import java.time.LocalDateTime;

public final class DateRangeUtils {

    private static final LocalDateTime MIN_BOUND = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX_BOUND = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private DateRangeUtils() {
    }

    public static LocalDateTime effectiveFrom(LocalDateTime from) {
        return from != null ? from : MIN_BOUND;
    }

    public static LocalDateTime effectiveTo(LocalDateTime to) {
        return to != null ? to : MAX_BOUND;
    }
}