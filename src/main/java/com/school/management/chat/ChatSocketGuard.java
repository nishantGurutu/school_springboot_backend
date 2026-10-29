package com.school.management.chat;
import com.school.management.repository.UserRepository;
import com.school.management.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.messaging.*;
import org.springframework.messaging.support.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;

@Component @RequiredArgsConstructor
public class ChatSocketGuard implements ChannelInterceptor {
 private final JwtService jwt;
 private final UserRepository users;
 private final ChatSocketSessions sessions;
 @Override public Message<?> preSend(Message<?> message,MessageChannel channel){
  var h=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);if(h==null)return message;
  if(h.getCommand()==StompCommand.CONNECT){
   String header=h.getFirstNativeHeader("Authorization");if(header==null||!header.startsWith("Bearer "))throw new AccessDeniedException("Access token required");
   String token=header.substring(7);String email=jwt.extractUsername(token);if(email==null)throw new AccessDeniedException("Access token expired");
   var user=users.findByEmail(email).orElseThrow(()->new AccessDeniedException("Unknown user"));
   if(!user.isEnabled()||!jwt.isTokenValid(token,user))throw new AccessDeniedException("Invalid access token");
   h.setUser(new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities()));sessions.expire(h.getSessionId(),jwt.expiresAt(token));
  } else if(h.getCommand()==StompCommand.SUBSCRIBE){
   if(h.getUser()==null||!"/user/queue/chat".equals(h.getDestination()))throw new AccessDeniedException("Subscription denied");
  } else if(h.getCommand()==StompCommand.SEND){if(h.getUser()==null||!"/app/chat.send".equals(h.getDestination()))throw new AccessDeniedException("Message destination denied");}
  return message;
 }
}
