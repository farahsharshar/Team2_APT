package com.Team2_CDE_master.ProjectServer.config;

import com.Team2_CDE_master.ProjectServer.handler.CRDTWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new CRDTWebSocketHandler(), "/document/{docId}")
                .setAllowedOrigins("*");
    }
}