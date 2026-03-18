package com.nd.me.filesystem;

public class FileProviderFactory {
    public static IFileProvider create(String uri) throws Exception {
        if (uri.startsWith("file://")) {
            return new LocalFileProvider(uri);
        } else if (uri.startsWith("sftp://")) {
            return new SftpFileProvider(uri);
        } else {
            throw new IllegalArgumentException("Unsupported scheme");
        }
    }
}
