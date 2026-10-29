package com.school.management.chat;
import com.school.management.entity.UserEntity;
import com.school.management.service.PortalNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.*;
import static com.school.management.chat.ChatPolicy.error;

@Service @RequiredArgsConstructor @Transactional
public class SchoolChatService {
 private final ChatPolicy policy;
 private final ChatRoomRepository rooms;
 private final ChatMemberRepository members;
 private final SchoolMessageRepository messages;
 private final ChatAttachmentRepository attachments;
 private final ChatEvents events;
 private final PortalNotificationService notifications;
 private final com.school.management.repository.ChatMessageRepository legacy;
 private final com.school.management.repository.PortalNotificationRepository inbox;
 public record Create(String name,boolean groupChat,List<Long> memberIds){}
 public record Send(String clientId,String text,String attachmentId){}
 public ChatRoom access(String id){var a=policy.actor();var r=rooms.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Chat not found"));
  if(!r.getSchoolId().equals(policy.school(a))||!members.existsById(id+":"+a.getId()))throw error(HttpStatus.FORBIDDEN,"Chat access denied");
  if(!r.isGroupChat()){var peer=members.findByRoomId(id).stream().filter(m->!m.getUserId().equals(a.getId())).findFirst().orElseThrow();if(!policy.contact(a,policy.user(peer.getUserId())))throw error(HttpStatus.FORBIDDEN,"Contact authorization changed");}
  return r;
 }
 private void event(ChatRoom r,String type){event(r,type,Map.of());}
 private void event(ChatRoom r,String type,Map<String,Object> details){events.detailed(members.findByRoomId(r.getId()).stream().map(m->policy.user(m.getUserId())).filter(u->u.isEnabled()&&r.getSchoolId().equals(policy.school(u))).map(UserEntity::getEmail).toList(),r.getId(),type,details);}
 public Map<String,Object> create(Create c){
  var actor=policy.actor();if(c.memberIds()==null||c.memberIds().isEmpty()||c.memberIds().size()>100)throw error(HttpStatus.BAD_REQUEST,"Choose 1 to 100 contacts");
  var ids=new LinkedHashSet<>(c.memberIds());ids.remove(actor.getId());if(ids.isEmpty())throw error(HttpStatus.BAD_REQUEST,"Choose a contact");
  if(!c.groupChat()&&ids.size()!=1)throw error(HttpStatus.BAD_REQUEST,"Direct chat needs one contact");
  if(c.groupChat()&&!(policy.leader(actor)||actor.getRole()==com.school.management.domain.user.Role.TEACHER))throw error(HttpStatus.FORBIDDEN,"Only teachers and school leaders create groups");
  for(Long id:ids)if(!policy.contact(actor,policy.user(id)))throw error(HttpStatus.FORBIDDEN,"Contact not authorized");
  String direct=null;
  if(!c.groupChat()){Long peer=ids.iterator().next();direct=Math.min(actor.getId(),peer)+":"+Math.max(actor.getId(),peer);var existing=rooms.findBySchoolIdAndDirectKey(policy.school(actor),direct);if(existing.isPresent())return summary(existing.get());}
  String name=Objects.toString(c.name(),"").trim();if(c.groupChat()&&(name.isEmpty()||name.length()>120))throw error(HttpStatus.BAD_REQUEST,"Group name must be 1 to 120 characters");
  var r=new ChatRoom();r.setId(UUID.randomUUID().toString());r.setSchoolId(policy.school(actor));r.setDirectKey(direct);r.setName(name);r.setGroupChat(c.groupChat());r.setOwnerId(actor.getId());r.setCreatedAt(Instant.now());rooms.save(r);
  ids.add(actor.getId());for(Long id:ids){var m=new ChatMember();m.setId(r.getId()+":"+id);m.setRoomId(r.getId());m.setUserId(id);members.save(m);}if(!r.isGroupChat())for(var oldMessage:legacy.conversation(actor.getId(),ids.stream().filter(id->!id.equals(actor.getId())).findFirst().orElseThrow())){var m=new SchoolMessage();m.setRoomId(r.getId());m.setSenderId(oldMessage.getSenderId());m.setSenderName(policy.user(oldMessage.getSenderId()).getName());m.setClientId("legacy-"+oldMessage.getId());m.setText(oldMessage.getContent());m.setSentAt(oldMessage.getSentAt().atZone(java.time.ZoneId.systemDefault()).toInstant());messages.save(m);}
  event(r,"room");return summary(r);
 }
 public List<Map<String,Object>> list(){var a=policy.actor();return members.findByUserId(a.getId()).stream().map(m->rooms.findById(m.getRoomId()).orElse(null)).filter(Objects::nonNull).filter(r->r.getSchoolId().equals(policy.school(a))).filter(r->r.isGroupChat()||members.findByRoomId(r.getId()).stream().filter(m->!m.getUserId().equals(a.getId())).anyMatch(m->policy.contact(a,policy.user(m.getUserId())))).map(this::summary).toList();}
 private Map<String,Object> summary(ChatRoom r){var a=policy.actor();var member=members.findById(r.getId()+":"+a.getId()).orElseThrow();var peers=members.findByRoomId(r.getId()).stream().map(m->policy.user(m.getUserId())).filter(u->r.getSchoolId().equals(u.getSchoolId())).toList();
  var result=new HashMap<String,Object>();result.put("id",r.getId());result.put("groupChat",r.isGroupChat());result.put("name",r.isGroupChat()?r.getName():peers.stream().filter(u->!u.getId().equals(a.getId())).map(UserEntity::getName).findFirst().orElse("Chat"));
  result.put("members",peers.stream().map(u->Map.of("id",u.getId(),"name",u.getName(),"role",u.getRole().name(),"deliveredThrough",members.findById(r.getId()+":"+u.getId()).orElseThrow().getDeliveredThrough(),"readThrough",members.findById(r.getId()+":"+u.getId()).orElseThrow().getReadThrough())).toList());result.put("unreadCount",messages.countByRoomIdAndIdGreaterThanAndSenderIdNot(r.getId(),member.getReadThrough(),a.getId()));
  result.put("lastMessage",messages.findFirstByRoomIdOrderByIdDesc(r.getId()).map(m->Objects.toString(m.getText(),"").isBlank()?"Attachment":m.getText()).orElse(""));return result;
 }
 public Map<String,Object> history(String room,long after){access(room);var batch=messages.findByRoomIdAndIdGreaterThanOrderByIdAsc(room,Math.max(0,after),PageRequest.of(0,100));return Map.of("messages",batch.stream().map(this::view).toList(),"hasMore",batch.size()==100);}
 private Map<String,Object> view(SchoolMessage m){var result=new HashMap<String,Object>();result.put("id",m.getId());result.put("senderId",m.getSenderId());result.put("senderName",Objects.toString(m.getSenderName(),"User"));result.put("clientId",m.getClientId());result.put("text",Objects.toString(m.getText(),""));result.put("sentAt",m.getSentAt());
  var recipients=members.findByRoomId(m.getRoomId()).stream().filter(p->!p.getUserId().equals(m.getSenderId())).toList();result.put("status",!recipients.isEmpty()&&recipients.stream().allMatch(p->p.getReadThrough()>=m.getId())?"read":!recipients.isEmpty()&&recipients.stream().allMatch(p->p.getDeliveredThrough()>=m.getId())?"delivered":"sent");
  if(m.getAttachmentId()!=null){var f=attachments.findById(m.getAttachmentId()).orElseThrow();result.put("attachment",Map.of("id",f.getId(),"name",f.getFileName(),"type",f.getContentType(),"size",f.getSize()));}return result;
 }
 public Map<String,Object> send(String room,Send s){var r=access(room);rooms.locked(room).orElseThrow();var a=policy.actor();
  if(s.clientId()==null||s.clientId().length()>80||s.clientId().isBlank())throw error(HttpStatus.BAD_REQUEST,"Message clientId required");
  var old=messages.findByRoomIdAndSenderIdAndClientId(room,a.getId(),s.clientId());if(old.isPresent())return view(old.get());
  String text=Objects.toString(s.text(),"").trim();if(text.length()>4000||(text.isEmpty()&&s.attachmentId()==null))throw error(HttpStatus.BAD_REQUEST,"Enter text up to 4000 characters or attach a file");
  if(s.attachmentId()!=null){var f=attachments.findById(s.attachmentId()).orElseThrow(()->error(HttpStatus.BAD_REQUEST,"Attachment not found"));if(!f.getRoomId().equals(room)||!f.getUploaderId().equals(a.getId()))throw error(HttpStatus.FORBIDDEN,"Attachment access denied");}
  var m=new SchoolMessage();m.setRoomId(room);m.setSenderId(a.getId());m.setSenderName(a.getName());m.setClientId(s.clientId());m.setText(text);m.setAttachmentId(s.attachmentId());m.setSentAt(Instant.now());messages.saveAndFlush(m);
  for(var recipient:members.findByRoomId(room))if(!recipient.getUserId().equals(a.getId())&&policy.user(recipient.getUserId()).isEnabled()&&r.getSchoolId().equals(policy.school(policy.user(recipient.getUserId()))))notifications.sendChat(recipient.getUserId(),"New message from "+a.getName(),text.isEmpty()?"Attachment":text.substring(0,Math.min(text.length(),180)),room,m.getId());
  var rendered=view(m);event(r,"message",Map.of("message",rendered));return rendered;
 }
 public void receipt(String room,long through,boolean read){var r=access(room);if(through<1)return;var message=messages.findById(through).orElseThrow(()->error(HttpStatus.BAD_REQUEST,"Message not found"));if(!message.getRoomId().equals(room))throw error(HttpStatus.FORBIDDEN,"Message belongs to another chat");
  var m=members.locked(room+":"+policy.actor().getId()).orElseThrow();boolean changed=m.getDeliveredThrough()<through||(read&&m.getReadThrough()<through);m.setDeliveredThrough(Math.max(m.getDeliveredThrough(),through));if(read){m.setReadThrough(Math.max(m.getReadThrough(),through));inbox.markChatRead(m.getUserId(),room,through,java.time.LocalDateTime.now());}if(changed){members.save(m);event(r,"receipt",Map.of("userId",m.getUserId(),"readThrough",m.getReadThrough(),"deliveredThrough",m.getDeliveredThrough()));}
 }
}
