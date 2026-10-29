package com.school.management.chat;
import com.school.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.*;
@Controller @RequiredArgsConstructor
public class ChatSocketController {
 private final SchoolChatService chat;
 private final UserRepository users;
 public record Command(String roomId,String clientId,String text,String attachmentId){}
 @MessageMapping("/chat.send") @SendToUser(value="/queue/chat",broadcast=false)
 public Map<String,Object> send(Command command,Principal principal){
  var previous=SecurityContextHolder.getContext();
  try {
   if(principal==null)throw new IllegalArgumentException("Sign in required");
   var user=users.findByEmail(principal.getName()).filter(u->u.isEnabled()).orElseThrow(()->new IllegalArgumentException("Account unavailable"));
   var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities()));SecurityContextHolder.setContext(context);
   if(command.roomId()==null||command.clientId()==null)throw new IllegalArgumentException("Room and clientId required");
   var message=chat.send(command.roomId(),new SchoolChatService.Send(command.clientId(),command.text(),command.attachmentId()));
   return Map.of("type","ack","roomId",command.roomId(),"clientId",command.clientId(),"message",message);
  } catch(ResponseStatusException e){return rejected(command,Objects.toString(e.getReason(),"Message rejected"));}
    catch(IllegalArgumentException e){return rejected(command,e.getMessage());}
    catch(Exception e){return rejected(command,"Unable to send message. Retry with the same draft.");}
  finally {SecurityContextHolder.setContext(previous);}
 }
 private Map<String,Object> rejected(Command command,String message){return Map.of("type","send-error","clientId",Objects.toString(command.clientId(),""),"error",message);}
}
