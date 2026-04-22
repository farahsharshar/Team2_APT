package com.Team2_CDE_master.ProjectServer.network;
public interface OperationListener {

    // called when a jason arrives
    void onOperationReceived(String jsonPayload);
    // when websocket connection is successfull
    void onConnected();
    // when connection drops or is closed
    void onDisconnected();
    // when somethings goes wring it is called
    void onError(String errorMessage);
}
