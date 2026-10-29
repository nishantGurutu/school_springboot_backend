package com.school.management.chat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.config.annotation.*;
import java.util.*;

@Configuration @EnableWebSocketMessageBroker @RequiredArgsConstructor
public class ChatSocketConfig implements WebSocketMessageBrokerConfigurer {
 private final ChatSocketGuard guard;
 private final ChatSocketSessions sessions;
 @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,https://school-dashboard-one-sepia.vercel.app}") private String origins;
 @org.springframework.context.annotation.Bean(name="taskScheduler")
 public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler chatBrokerScheduler(){var scheduler=new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();scheduler.setPoolSize(4);scheduler.setThreadNamePrefix("chat-heartbeat-");return scheduler;}
 @Override public void registerStompEndpoints(StompEndpointRegistry registry){registry.addEndpoint("/api/chat/ws").setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new));}
 @Override public void configureMessageBroker(MessageBrokerRegistry registry){registry.enableSimpleBroker("/queue").setTaskScheduler(chatBrokerScheduler()).setHeartbeatValue(new long[]{10000,10000});registry.setUserDestinationPrefix("/user");registry.setApplicationDestinationPrefixes("/app");}
 @Override public void configureClientInboundChannel(ChannelRegistration registration){registration.interceptors(guard);}
 @Override public void configureWebSocketTransport(WebSocketTransportRegistration registration){
  registration.setMessageSizeLimit(16384).setSendBufferSizeLimit(65536).setSendTimeLimit(10000);
  registration.addDecoratorFactory(handler->new WebSocketHandlerDecorator(handler){
   @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {sessions.opened(session);super.afterConnectionEstablished(session);}
   @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status) throws Exception {sessions.closed(session.getId());super.afterConnectionClosed(session,status);}
  });
 }
}
