package com.Team2_CDE_master.ProjectServer.config;

import com.Team2_CDE_master.ProjectServer.handler.CRDTWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private CRDTWebSocketHandler crdtWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(crdtWebSocketHandler, "/document/**")
                .setAllowedOriginPatterns("*");
        // Do NOT call .withSockJS() — the Java-WebSocket client
        // speaks native WebSocket, not SockJS protocol.
    }
}