package com.viaversion.viaversion.api.protocol;

public interface ProtocolPipeline {
    boolean contains(Class<? extends Protocol> protocolClass);
}
