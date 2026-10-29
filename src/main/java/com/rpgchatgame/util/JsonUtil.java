package com.rpgchatgame.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(timeModule());

    private static com.fasterxml.jackson.databind.module.SimpleModule timeModule() {
        var module = new com.fasterxml.jackson.databind.module.SimpleModule();
        module.addSerializer(java.time.LocalDateTime.class, new com.fasterxml.jackson.databind.JsonSerializer<java.time.LocalDateTime>() {
            @Override public void serialize(java.time.LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator generator,
                    com.fasterxml.jackson.databind.SerializerProvider provider) throws java.io.IOException {
                generator.writeString(value.toString());
            }
        });
        module.addDeserializer(java.time.LocalDateTime.class, new com.fasterxml.jackson.databind.JsonDeserializer<java.time.LocalDateTime>() {
            @Override public java.time.LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                return java.time.LocalDateTime.parse(parser.getValueAsString());
            }
        });
        return module;
    }

    private JsonUtil() {
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse JSON", e);
        }
    }
}
