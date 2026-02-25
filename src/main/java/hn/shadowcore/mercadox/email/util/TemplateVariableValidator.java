package hn.shadowcore.mercadox.email.util;

import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TemplateVariableValidator {

    public void validate(Object payload, List<String> expectedFields) {
        if(payload == null) {
            throw new IllegalArgumentException("Payload is null");
        }

        Class<?> clazz = payload.getClass();
        Set<String> availableFields = Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        List<String> missing = expectedFields.stream()
                .filter(val -> !availableFields.contains(val))
                .toList();

        if(!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing fields in payload: " + missing);
        }
    }
}
