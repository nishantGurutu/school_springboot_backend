package com.school.management.chat;

import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.repository.UserRepository;
import com.school.management.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketIntegrationTest {
 @LocalServerPort int port;
 @Autowired TestRestTemplate rest;
 @Autowired UserRepository users;
 @Autowired JwtService jwt;
 HttpEntity<Object> body(Object payload,String token){var h=new HttpHeaders();h.setBearerAuth(token);h.setContentType(MediaType.APPLICATION_JSON);return new HttpEntity<>(payload,h);}
 @Test void committedRestMessageReachesOnlyAuthenticatedUserQueue() throws Exception {
  var a=users.save(UserEntity.builder().name("Socket Sender").email("socket.sender@test.local").password("unused").role(Role.ADMIN).schoolId("socket-school").build());
  var b=users.save(UserEntity.builder().name("Socket Recipient").email("socket.recipient@test.local").password("unused").role(Role.STAFF).schoolId("socket-school").build());
  var stomp=new WebSocketStompClient(new StandardWebSocketClient());stomp.setMessageConverter(new MappingJackson2MessageConverter());stomp.setDefaultHeartbeat(new long[]{0,0});
  var headers=new StompHeaders();headers.set("Authorization","Bearer "+jwt.generateAccessToken(b.getEmail()));
  StompSession session=null;
  try {
   session=stomp.connectAsync("ws://localhost:"+port+"/api/chat/ws",new WebSocketHttpHeaders(),headers,new StompSessionHandlerAdapter(){}).get(10,TimeUnit.SECONDS);
   var events=new LinkedBlockingQueue<Map<?,?>>();
   session.subscribe("/user/queue/chat",new StompFrameHandler(){public Type getPayloadType(StompHeaders h){return Map.class;}public void handleFrame(StompHeaders h,Object payload){events.add((Map<?,?>)payload);}});
   String access=jwt.generateAccessToken(a.getEmail());
   var created=rest.exchange("/api/chat/rooms",HttpMethod.POST,body(Map.of("name","","groupChat",false,"memberIds",List.of(b.getId())),access),Map.class);
   assertEquals(200,created.getStatusCode().value());String room=created.getBody().get("id").toString();
   var sent=rest.exchange("/api/chat/rooms/"+room+"/messages",HttpMethod.POST,body(Map.of("clientId","socket-message","text","Realtime test"),access),Map.class);
   assertEquals(200,sent.getStatusCode().value());
   Map<?,?> event; long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);boolean message=false;
   while(System.nanoTime()<deadline){event=events.poll(1,TimeUnit.SECONDS);if(event!=null&&event.get("type").equals("message")){assertEquals(room,event.get("roomId"));assertEquals("Realtime test",((Map<?,?>)event.get("message")).get("text"));assertEquals(a.getId().longValue(),((Number)((Map<?,?>)event.get("message")).get("senderId")).longValue());message=true;break;}}
   assertTrue(message,"Committed message must publish a STOMP user-queue event");
   session.send("/app/chat.send",Map.of("roomId",room,"clientId","mobile-ws-send","text","Hello via websocket 😀","senderId",a.getId()));
   Map<?,?> ack=null;deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
   while(System.nanoTime()<deadline){var next=events.poll(1,TimeUnit.SECONDS);if(next!=null&&"ack".equals(next.get("type"))){ack=next;break;}}
   assertNotNull(ack,"WebSocket send must acknowledge committed message");assertEquals(b.getId().longValue(),((Number)((Map<?,?>)ack.get("message")).get("senderId")).longValue());
   var history=rest.exchange("/api/chat/rooms/"+room+"/messages",HttpMethod.GET,body(null,jwt.generateAccessToken(b.getEmail())),Map.class);
   assertEquals(200,history.getStatusCode().value());assertEquals(2,((List<?>)history.getBody().get("messages")).size());
  } finally {if(session!=null&&session.isConnected())session.disconnect();stomp.stop();}
 }
}
