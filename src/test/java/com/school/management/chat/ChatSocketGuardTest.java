package com.school.management.chat;
import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.repository.UserRepository;
import com.school.management.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ChatSocketGuardTest {
 final JwtService jwt=new JwtService("unit-test-chat-secret-at-least-32-characters");
 final UserRepository users=mock(UserRepository.class);
 final ChatSocketSessions sessions=mock(ChatSocketSessions.class);
 final ChatSocketGuard guard=new ChatSocketGuard(jwt,users,sessions);
 StompHeaderAccessor header(StompCommand command){var h=StompHeaderAccessor.create(command);h.setSessionId("session");h.setLeaveMutable(true);return h;}
 void send(StompHeaderAccessor h){guard.preSend(MessageBuilder.createMessage(new byte[0],h.getMessageHeaders()),null);}
 @Test void connectAuthenticatesExistingJwtAndSchedulesExpiry(){
  var user=UserEntity.builder().id(1L).email("chat@test.local").role(Role.TEACHER).build();when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
  var h=header(StompCommand.CONNECT);h.setNativeHeader("Authorization","Bearer "+jwt.generateAccessToken(user.getEmail()));send(h);assertEquals(user.getEmail(),h.getUser().getName());verify(sessions).expire(eq("session"),any());
 }
 @Test void anonymousRefreshTokenAndDisabledUserCannotConnect(){
  assertThrows(AccessDeniedException.class,()->send(header(StompCommand.CONNECT)));
  var user=UserEntity.builder().id(1L).email("chat@test.local").role(Role.TEACHER).build();when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
  var h=header(StompCommand.CONNECT);h.setNativeHeader("Authorization","Bearer "+jwt.generateRefreshToken(user.getEmail()));assertThrows(AccessDeniedException.class,()->send(h));
  user.setEnabled(false);h.setNativeHeader("Authorization","Bearer "+jwt.generateAccessToken(user.getEmail()));assertThrows(AccessDeniedException.class,()->send(h));
 }
 @Test void onlyOwnUserQueueIsSubscribableAndClientBroadcastIsDenied(){
  var h=header(StompCommand.SUBSCRIBE);h.setUser(()->"chat@test.local");h.setDestination("/topic/all");assertThrows(AccessDeniedException.class,()->send(h));
  h.setDestination("/queue/chat");assertThrows(AccessDeniedException.class,()->send(h));h.setDestination("/user/queue/chat");assertDoesNotThrow(()->send(h));
  assertThrows(AccessDeniedException.class,()->send(header(StompCommand.SEND)));
  var allowed=header(StompCommand.SEND);allowed.setUser(()->"chat@test.local");allowed.setDestination("/app/chat.send");assertDoesNotThrow(()->send(allowed));allowed.setDestination("/topic/all");assertThrows(AccessDeniedException.class,()->send(allowed));
 }
}
