package me.lirx.grpc.common;

import io.grpc.Metadata;

public class MetadataHeaders {
    public static final Metadata.Key<String> USER =
            Metadata.Key.of("user", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> PASSWORD =
            Metadata.Key.of("password", Metadata.ASCII_STRING_MARSHALLER);
}
