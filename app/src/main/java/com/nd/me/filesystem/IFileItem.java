package com.nd.me.filesystem;

public interface IFileItem {
    String getName();

    long getModTime();

    long getSize();

    boolean isDirectory();

    String getPath();

    String getUrl();
}

