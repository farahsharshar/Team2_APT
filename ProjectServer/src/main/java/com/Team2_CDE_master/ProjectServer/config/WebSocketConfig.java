package com.Team2_CDE_master.ProjectServer.config;

import com.Team2_CDE_master.ProjectServer.handler.CRDTWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.*;

// @EnableScheduling activates the @Scheduled(fixedDelay=...) auto-save in CRDTWebSocketHandler
@Configuration
@EnableWebSocket
@EnableScheduling
public class WebSocketConfig implements WebSocketConfigurer {

    // Spring injects the singleton handler bean — we no longer call "new CRDTWebSocketHandler()"
    // so that @Autowired and @Scheduled inside the handler actually work.
    @Autowired
    private CRDTWebSocketHandler handler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/document/{docId}")
                .setAllowedOrigins("*");
    }
}