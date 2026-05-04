package com.Team2_CDE_master.ProjectServer.network;

public interface OperationListener {
    void onOperationReceived(String jsonPayload);
    void onConnected();
    void onDisconnected();
    void onError(String errorMessage);
}
