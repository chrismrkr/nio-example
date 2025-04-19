package org.example.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class ConfigLoader {
    private static ObjectMapper objectMapper = new ObjectMapper();
    public static <T> T load(String filePath, Class<T> clazz) throws IOException {
        try {
            InputStream inputStream = ClassLoader.getSystemResourceAsStream(filePath);
            return objectMapper.readValue(inputStream, clazz);
        } catch (Exception e) {
            throw e;
        }
    }
}
