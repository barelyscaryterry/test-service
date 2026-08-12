package com.trafficreplay.testservice.beer;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * All fields optional — only non-null fields are applied to the existing item.
 */
public record PatchBeerRequest(
        String name,
        String brewery,
        String style,
        @PositiveOrZero BigDecimal abv) {
}
