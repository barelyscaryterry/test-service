package com.trafficreplay.testservice.beer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateBeerRequest(
        @NotBlank String name,
        @NotBlank String brewery,
        @NotBlank String style,
        @NotNull @PositiveOrZero BigDecimal abv) {
}
