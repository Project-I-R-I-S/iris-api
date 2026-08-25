package com.iris.common.util;

import java.math.BigDecimal;

/**
 * Small null-safe numeric helpers shared across features. Optional nutritional
 * and quantity fields (protein, caffeine, amounts, ...) are frequently omitted
 * on the request and should default to zero rather than be persisted as null.
 */
public final class Numbers {

    private Numbers() { }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
