package com.Team2_CDE_master.ProjectServer.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserPresenceManager {

    private final LinkedHashMap<Integer, String> users = new LinkedHashMap<>();
    private Runnable onChanged;

    public void setOnChanged(Runnable r) {
        this.onChanged = r;
    }

    public void addUser(int siteId, String username) {
        users.put(siteId, username);
        fire();
    }

    public void removeUser(int siteId) {
        users.remove(siteId);
        fire();
    }

    public List<String> getActiveUsernames() {
        return new ArrayList<>(users.values());
    }

    public Map<Integer, String> getUsers() {
        return users;
    }

    public int getCount() {
        return users.size();
    }

    public void clear() {
        users.clear();
        fire();
    }

    private void fire() {
        if (onChanged != null) onChanged.run();
    }
}