package com.nd.me.filesystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface IFileProvider {
    List<IFileItem> listFiles(String path) throws Exception;

    InputStream getInputStream(String path) throws Exception;

    InputStream getInputStreamOffset(String path, long offset) throws Exception;

    byte[] readAllBytes(String path) throws Exception;

    void close();

    void delete(IFileItem fileItem) throws Exception;

    void rename(String oldPath, String newPath) throws Exception;

    void mkdir(String dir) throws Exception;

    void move(String sourcePath, String targetPath) throws Exception;

    void copy(String sourcePath, String targetPath) throws Exception;

    OutputStream getOutputstream(String path) throws Exception;
}
