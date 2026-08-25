package com.iris.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NumbersTest {

    @Test
    void nzBigDecimal_returnsZeroForNull() {
        assertThat(Numbers.nz((BigDecimal) null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void nzBigDecimal_returnsValueUnchangedWhenPresent() {
        assertThat(Numbers.nz(new BigDecimal("12.5"))).isEqualByComparingTo("12.5");
    }

    @Test
    void nzInteger_returnsZeroForNull() {
        assertThat(Numbers.nz((Integer) null)).isZero();
    }

    @Test
    void nzInteger_returnsValueUnchangedWhenPresent() {
        assertThat(Numbers.nz(7)).isEqualTo(7);
    }
}
