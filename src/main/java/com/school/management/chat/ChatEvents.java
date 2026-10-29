package com.school.management.chat;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Component @RequiredArgsConstructor
public class ChatEvents {
 private final ApplicationEventPublisher publisher;
 private final SimpMessagingTemplate socket;
 public record Changed(List<String> emails,String roomId,String type,Map<String,Object> details){}
 public void changed(List<String> emails,String room,String type){publisher.publishEvent(new Changed(emails,room,type,Map.of()));}
 public void detailed(List<String> emails,String room,String type,Map<String,Object> details){publisher.publishEvent(new Changed(emails,room,type,Map.copyOf(details)));}
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 public void publish(Changed event){for(String email:event.emails())socket.convertAndSendToUser(email,"/queue/chat",payload(event));}
 private Map<String,Object> payload(Changed event){var payload=new HashMap<String,Object>(event.details());payload.put("roomId",event.roomId());payload.put("type",event.type());return payload;}
}
