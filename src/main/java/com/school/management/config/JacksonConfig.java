package com.school.management.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            builder.featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE) {
                @Override
                public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                    String value = parser.getValueAsString();
                    if (value == null || value.trim().isEmpty()) {
                        return null;
                    }
                    return super.deserialize(parser, context);
                }
            });
        };
    }
}
