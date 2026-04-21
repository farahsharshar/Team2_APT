package com.Team2_CDE_master.ProjectServer.network;
// This is the "contract" part of the project, yo receive any operations from server it needs to pas here
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
