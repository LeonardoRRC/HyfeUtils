package com.viaversion.viaversion.api.type;

public class Type<T> {
    private final Class<? super T> outputClass;

    public Type(Class<? super T> outputClass) {
        this.outputClass = outputClass;
    }

    public Class<? super T> getOutputClass() {
        return outputClass;
    }
}
