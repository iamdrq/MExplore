package com.nd.me.filesystem;

import com.jcraft.jsch.*;
import com.nd.me.util.SimpleURI;
import com.nd.me.util.StorageUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class SftpFileProvider implements IFileProvider {

    private Session session;
    private ChannelSftp sftp;
    private String currentDir;

    private String url;
    private String baseUrl;

    private boolean connected = false;

    public SftpFileProvider(String url) throws Exception {
        this.url = url;
        SimpleURI uri = new SimpleURI(url);
        currentDir = uri.getPath();
        baseUrl = url.replace(uri.getPath(), "");
    }

    public Session createSession() throws Exception {
        SimpleURI uri = new SimpleURI(url);

        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        String username = uri.getUser();
        String password = uri.getPassword();

        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect(5000);
        return session;
    }

    public ChannelSftp createSftp(Session session) throws Exception {
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        return sftp;
    }

    public void connect() throws Exception {
        if (connected) {
            return;
        }
        session = createSession();
        sftp = createSftp(session);
        connected = true;
    }

    @Override
    public List<IFileItem> listFiles(String path) throws Exception {
        this.connect();
        if (path != null) {
            currentDir = path;
        }

        Vector<ChannelSftp.LsEntry> list = sftp.ls(currentDir);
        String dir = currentDir;
        List<IFileItem> result = new ArrayList<>();
        for (ChannelSftp.LsEntry entry : list) {
            if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) continue;
            result.add(new IFileItem() {
                @Override
                public String getName() {
                    return entry.getFilename();
                }

                @Override
                public boolean isDirectory() {
                    return entry.getAttrs().isDir();
                }

                @Override
                public String getPath() {
                    return dir + "/" + entry.getFilename();
                }

                @Override
                public String getUrl() {
                    return baseUrl + dir + "/" + entry.getFilename();
                }

                @Override
                public long getModTime() {
                    return entry.getAttrs().getMTime() * 1000L;
                }

                @Override
                public long getSize() {
                    return entry.getAttrs().getSize();
                }
            });
        }
        return result;
    }

    @Override
    public InputStream getInputStream(String path) throws Exception {
        return getInputStreamOffset(path, 0L);
    }

    @Override
    public InputStream getInputStreamOffset(String path, long offset) throws Exception {
        this.connect();
        ChannelSftp sftp = createSftp(session);
        InputStream inputStream = sftp.get(path, null, offset);
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return inputStream.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return inputStream.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return inputStream.read(b, off, len);
            }

            @Override
            public void close() throws IOException {
                inputStream.close();
                sftp.exit();
            }
        };
    }

    @Override
    public byte[] readAllBytes(String path) throws Exception {
        this.connect();
        ChannelSftp sftp = createSftp(session);
        byte[] data;
        try (InputStream inputStream = sftp.get(path)) {
            data = StorageUtils.readAllBytes(inputStream);
        }
        sftp.exit();
        return data;
    }

    public void close() {
        if (sftp != null) sftp.exit();
        if (session != null) session.disconnect();
    }

    @Override
    public void delete(IFileItem fileItem) throws Exception {
        this.connect();
        ChannelSftp sftp = createSftp(session);
        if (fileItem.isDirectory()) {
            Vector<ChannelSftp.LsEntry> list = sftp.ls(fileItem.getPath());
            for (ChannelSftp.LsEntry entry : list) {
                String name = entry.getFilename();
                if (".".equals(name) || "..".equals(name)) continue;
                if (entry.getAttrs().isDir()) {
                    delete(new IFileItem() {
                        @Override
                        public String getName() {
                            return entry.getFilename();
                        }

                        @Override
                        public boolean isDirectory() {
                            return entry.getAttrs().isDir();
                        }

                        @Override
                        public String getPath() {
                            return fileItem.getPath() + "/" + entry.getFilename();
                        }

                        @Override
                        public String getUrl() {
                            return baseUrl + fileItem.getPath() + "/" + entry.getFilename();
                        }

                        @Override
                        public long getModTime() {
                            return entry.getAttrs().getMTime() * 1000L;
                        }

                        @Override
                        public long getSize() {
                            return entry.getAttrs().getSize();
                        }
                    });
                } else {
                    sftp.rm(fileItem.getPath() + "/" + entry.getFilename());
                }
            }
            sftp.rmdir(fileItem.getPath());
        } else {
            sftp.rm(fileItem.getPath());
        }
        sftp.exit();
    }

    @Override
    public void rename(String oldPath, String newPath) throws Exception {
        this.connect();
        ChannelSftp sftp = createSftp(session);
        sftp.rename(oldPath, newPath);
        sftp.exit();
    }

    public void move(String sourcePath, String targetPath) throws Exception {
        String cmd = String.format("mv '%s' '%s'", sourcePath, targetPath);
        cmd(cmd);
    }

    public void copy(String sourcePath, String targetPath) throws Exception {
        String cmd = String.format("cp -r '%s' '%s'", sourcePath, targetPath);
        cmd(cmd);
    }

    @Override
    public void mkdir(String dir) throws Exception {
        this.connect();
        ChannelSftp sftp = createSftp(session);
        sftp.mkdir(dir);
        sftp.exit();
    }

    @Override
    public OutputStream getOutputstream(String path) throws Exception {
        this.connect();
        ChannelSftp sftp = createSftp(session);
        OutputStream outputStream = sftp.put(path);
        return new OutputStream() {
            @Override
            public void write(byte[] b) throws IOException {
                outputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                outputStream.write(b, off, len);
            }

            @Override
            public void write(int b) throws IOException {
                outputStream.write(b);
            }

            @Override
            public void close() throws IOException {
                outputStream.close();
                sftp.exit();
            }

            @Override
            public void flush() throws IOException {
                outputStream.flush();
            }
        };
    }

    public void cmd(String cmd) throws Exception {
        this.connect();
        ChannelExec exec = (ChannelExec) session.openChannel("exec");
        exec.setCommand(cmd);
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(exec.getErrStream()));
        exec.connect();

        String line;
        StringBuilder errorMsg = new StringBuilder();
        while ((line = errorReader.readLine()) != null) {
            errorMsg.append(line).append("\n");
        }
        exec.disconnect();
        if (exec.getExitStatus() != 0) {
            throw new Exception(errorMsg.toString());
        }
    }
}
