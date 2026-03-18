package com.nd.me.util;

public class SimpleURI {

    private String scheme;
    private String userInfo;
    private String user;
    private String password;
    private String host;
    private int port;
    private String path;

    public SimpleURI(String scheme, String userInfo, String user, String password, String host, int port, String path) {
        this.scheme = scheme;
        this.userInfo = userInfo;
        this.user = user;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
    }

    public SimpleURI(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd > 0) {
            scheme = url.substring(0, schemeEnd);
            url = url.substring(schemeEnd + 3);
        }

        int pathStart = url.indexOf('/');
        String authority;
        if (pathStart >= 0) {
            authority = url.substring(0, pathStart);
            path = url.substring(pathStart);
        } else {
            authority = url;
        }

        int at = authority.indexOf('@');
        if (at >= 0) {
            userInfo = authority.substring(0, at);
            authority = authority.substring(at + 1);

            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                user = userInfo.substring(0, colon);
                password = userInfo.substring(colon + 1);
            } else {
                user = userInfo;
            }
        }

        int colon = authority.lastIndexOf(':');
        if (colon >= 0 && colon < authority.length() - 1) {
            try {
                port = Integer.parseInt(authority.substring(colon + 1));
                host = authority.substring(0, colon);
            } catch (NumberFormatException e) {
                host = authority;
            }
        } else {
            host = authority;
        }
    }

    public String getScheme() {
        return scheme;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "scheme=" + scheme +
                "\nuserInfo=" + userInfo +
                "\nuser=" + user +
                "\npassword=" + password +
                "\nhost=" + host +
                "\nport=" + port +
                "\npath=" + path;
    }
}