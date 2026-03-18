package com.nd.me.filesystem;

import com.nd.me.util.SimpleURI;
import com.nd.me.util.StorageUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LocalFileProvider implements IFileProvider {
    private String currentDir;
    private String url;

    public LocalFileProvider(String url) throws Exception {
        this.url = url;
        SimpleURI uri = new SimpleURI(url);
        currentDir = uri.getPath();
    }

    @Override
    public List<IFileItem> listFiles(String path) {
        if (path != null) {
            currentDir = path;
        }
        File dir = new File(path);
        List<IFileItem> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                result.add(new IFileItem() {
                    @Override
                    public String getName() {
                        return f.getName();
                    }

                    @Override
                    public long getModTime() {
                        return f.lastModified();
                    }

                    @Override
                    public long getSize() {
                        return f.length();
                    }

                    @Override
                    public boolean isDirectory() {
                        return f.isDirectory();
                    }

                    @Override
                    public String getPath() {
                        return f.getPath();
                    }

                    @Override
                    public String getUrl() {
                        return "file://" + f.getPath();
                    }
                });
            }
        }
        return result;
    }

    @Override
    public InputStream getInputStream(String path) throws Exception {
        return new FileInputStream(path);
    }

    @Override
    public InputStream getInputStreamOffset(String path, long offset) throws Exception {
        FileInputStream randomAccessFile = new FileInputStream(path);
        randomAccessFile.skip(offset);
        return randomAccessFile;
    }

    @Override
    public byte[] readAllBytes(String path) throws Exception {
        byte[] data;
        try (FileInputStream fileInputStream = new FileInputStream(path)) {
            data = StorageUtils.readAllBytes(fileInputStream);
        }
        return data;
    }

    @Override
    public void close() {

    }

    @Override
    public void delete(IFileItem fileItem) throws Exception {
        if (fileItem.isDirectory()) {
            File[] files = new File(fileItem.getPath()).listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        delete(new IFileItem() {
                            @Override
                            public String getName() {
                                return f.getName();
                            }

                            @Override
                            public long getModTime() {
                                return f.lastModified();
                            }

                            @Override
                            public long getSize() {
                                return f.length();
                            }

                            @Override
                            public boolean isDirectory() {
                                return f.isDirectory();
                            }

                            @Override
                            public String getPath() {
                                return f.getPath();
                            }

                            @Override
                            public String getUrl() {
                                return "file://" + f.getPath();
                            }
                        });
                    } else {
                        if (!new File(f.getPath()).delete()) {
                            throw new Exception("delete fail");
                        }
                    }
                }
            }
        }
        if (!new File(fileItem.getPath()).delete()) {
            throw new Exception("delete fail");
        }
    }

    @Override
    public void rename(String oldPath, String newPath) throws Exception {
        Files.move(Paths.get(oldPath), Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void mkdir(String dir) throws Exception {
        Files.createDirectory(Paths.get(dir));
    }

    public void move(String sourcePath, String targetPath) throws Exception {
        Path src = Paths.get(sourcePath);
        Path dst = Paths.get(targetPath);
        Path targetRoot = dst.resolve(src.getFileName());
        Files.createDirectories(targetRoot);
        Files.walk(src)
                .sorted(Comparator.reverseOrder())
                .forEach(source -> {
                    try {
                        Path target = targetRoot.resolve(src.relativize(source));
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target);
                        } else {
                            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        try {
            Files.deleteIfExists(src);
        } catch (Exception e) {
        }
    }

    public void copy(String sourcePath, String targetPath) throws Exception {
        Path src = Paths.get(sourcePath);
        Path dst = Paths.get(targetPath);
        Path targetRoot = dst.resolve(src.getFileName());
        Files.walk(src).forEach(source -> {
            try {
                Path target = targetRoot.resolve(src.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public OutputStream getOutputstream(String path) throws Exception {
        return new FileOutputStream(path);
    }
}
