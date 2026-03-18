package com.nd.me.util;

import org.json.JSONObject;

public class NetworkLink {
    public long id;
    public String name;
    public String protocol;
    public String host;
    public String port;
    public String path;
    public String username;
    public String password;

    public NetworkLink() {
    }

    public NetworkLink(long id, String name, String protocol, String host, String port, String path, String username, String password) {
        this.id = id;
        this.name = name;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.username = username;
        this.password = password;
    }

    public String toJsonString() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", id);
            jsonObject.put("name", name);
            jsonObject.put("protocol", protocol);
            jsonObject.put("host", host);
            jsonObject.put("port", port);
            jsonObject.put("path", path);
            jsonObject.put("username", username);
            jsonObject.put("password", password);
            return jsonObject.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static NetworkLink fromJsonString(String s) {
        try {
            JSONObject jsonObject = new JSONObject(s);
            NetworkLink networkLink = new NetworkLink();
            networkLink.id = jsonObject.getLong("id");
            networkLink.name = jsonObject.getString("name");
            networkLink.protocol = jsonObject.getString("protocol");
            networkLink.host = jsonObject.getString("host");
            networkLink.port = jsonObject.getString("port");
            networkLink.path = jsonObject.getString("path");
            networkLink.username = jsonObject.getString("username");
            networkLink.password = jsonObject.getString("password");
            return networkLink;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String buildUrl() {
        return protocol + "://" + username + ":" + password + "@" + host + ":" + port + path;
    }
}
