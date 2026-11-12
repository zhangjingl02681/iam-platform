package com.enterprise.iam.common.jpa;

import jakarta.persistence.AttributeConverter;
import java.util.Objects;

public abstract class CodedEnumConverter<E extends Enum<E> & CodedEnum>
        implements AttributeConverter<E, String> {

    private final Class<E> enumType;

    protected CodedEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        for (E constant : enumType.getEnumConstants()) {
            if (constant.code().equalsIgnoreCase(dbData)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown " + enumType.getSimpleName() + " value: " + dbData);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CodedEnumConverter<?> c && Objects.equals(enumType, c.enumType);
    }

    @Override
    public int hashCode() {
        return enumType.hashCode();
    }
}
