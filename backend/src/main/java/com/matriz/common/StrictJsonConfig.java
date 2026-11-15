package com.matriz.common;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * Makes the API refuse a value of the wrong JSON type instead of converting it (NFR-04, S-05).
 * The contract says {@code string}, so {@code 42}, {@code 1.5} or {@code true} sent for a text field
 * is a {@code 400} and not the text "42". The opposite case, a text sent for a number, is refused
 * by {@code allow-coercion-of-scalars: false} in {@code application.yml}.
 */
@Configuration
class StrictJsonConfig {

    @Bean
    JsonMapperBuilderCustomizer refuseNumbersAndBooleansForText() {
        return builder -> builder.withCoercionConfig(LogicalType.Textual, config -> config
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
