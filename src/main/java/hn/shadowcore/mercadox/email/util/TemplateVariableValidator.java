package hn.shadowcore.mercadox.email.util;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

@Component
public class TemplateVariableValidator {

    public void validate(Object payload, List<String> expectedFields) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload is null");
        }
        if (expectedFields == null || expectedFields.isEmpty()) {
            return;
        }
        // String payloads are passed as-is to Thymeleaf via ${data} — no field-level access
        if (payload instanceof String) {
            return;
        }

        List<String> missingOrNull = expectedFields.stream()
                .filter(field -> !hasNonNullValue(payload, field))
                .toList();

        if (!missingOrNull.isEmpty()) {
            throw new IllegalArgumentException(
                    "Template requires fields that are missing or null in payload: " + missingOrNull);
        }
    }

    private boolean hasNonNullValue(Object payload, String fieldName) {
        if (payload instanceof SpecificRecord record) {
            Schema.Field schemaField = record.getSchema().getField(fieldName);
            return schemaField != null && record.get(schemaField.pos()) != null;
        }
        return hasNonNullValueViaReflection(payload, fieldName);
    }

    private boolean hasNonNullValueViaReflection(Object payload, String fieldName) {
        Class<?> clazz = payload.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(payload) != null;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                return false;
            }
        }
        return false;
    }
}
