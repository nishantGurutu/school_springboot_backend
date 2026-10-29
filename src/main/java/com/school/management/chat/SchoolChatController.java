package com.school.management.chat;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static com.school.management.chat.ChatPolicy.error;

@RestController @RequestMapping("/api/chat") @RequiredArgsConstructor
public class SchoolChatController {
 private final SchoolChatService service;
 private final ChatPolicy policy;
 private final ChatAttachmentRepository attachments;
 private final ChatFileStorage files;
 @GetMapping("/contacts") @Transactional(readOnly=true) public Object contacts(){return policy.contacts();}
 @GetMapping("/rooms") public Object rooms(){return service.list();}
 @PostMapping("/rooms") public Object create(@RequestBody SchoolChatService.Create request){return service.create(request);}
 @GetMapping("/rooms/{room}/messages") public Object history(@PathVariable String room,@RequestParam(defaultValue="0") long after){return service.history(room,after);}
 @PostMapping("/rooms/{room}/messages") public Object send(@PathVariable String room,@RequestBody SchoolChatService.Send request){return service.send(room,request);}
 public record Receipt(long through,boolean read){}
 @PostMapping("/rooms/{room}/receipts") public void receipt(@PathVariable String room,@RequestBody Receipt receipt){service.receipt(room,receipt.through(),receipt.read());}
 @PostMapping("/rooms/{room}/attachments") @Transactional public Object upload(@PathVariable String room,@RequestParam("file") MultipartFile file) throws Exception {
  service.access(room);if(file.isEmpty()||file.getSize()>25*1024*1024)throw error(HttpStatus.BAD_REQUEST,"Attach a file up to 25 MB");
  byte[] bytes=file.getBytes();String type=detect(bytes,Objects.toString(file.getOriginalFilename(),"file"));if(type==null)throw error(HttpStatus.BAD_REQUEST,"File content does not match its file type");
  String name=Objects.toString(file.getOriginalFilename(),"attachment").replaceAll("[\\/\r\n]","_");if(name.length()>180)name=name.substring(name.length()-180);
  var a=new ChatAttachment();a.setId(UUID.randomUUID().toString());a.setRoomId(room);a.setUploaderId(policy.actor().getId());a.setFileName(name);a.setContentType(type);a.setSize(bytes.length);a.setStorageKey(files.put(a.getId(),bytes,type));attachments.save(a);return Map.of("id",a.getId(),"name",name,"type",type,"size",bytes.length);
 }
 private String detect(byte[] b,String name){
  if(b.length==0)return null;
  String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";
  if(Set.of("exe","com","bat","cmd","ps1","sh","apk","msi","html","htm","js","svg").contains(ext))return null;
  if(b.length>=8&&b[0]==(byte)0x89&&b[1]==80&&b[2]==78&&b[3]==71&&b[4]==13&&b[5]==10&&b[6]==26&&b[7]==10)return "image/png";
  if(b.length>=3&&b[0]==(byte)0xff&&b[1]==(byte)0xd8&&b[2]==(byte)0xff)return "image/jpeg";
  String s=new String(b,0,Math.min(16,b.length),StandardCharsets.ISO_8859_1);
  if(s.startsWith("%PDF-"))return "application/pdf";
  if(s.startsWith("GIF87a")||s.startsWith("GIF89a"))return "image/gif";
  if(s.length()>=12&&s.startsWith("RIFF")&&s.substring(8).startsWith("WEBP"))return "image/webp";
  if(b.length>=12&&s.substring(4).startsWith("ftyp"))return ext.equals("mov")?"video/quicktime":"video/mp4";
  if(b.length>=4&&b[0]==0x1a&&b[1]==0x45&&b[2]==(byte)0xdf&&b[3]==(byte)0xa3)return "video/webm";
  if(s.startsWith("ID3")||(b.length>=2&&b[0]==(byte)0xff&&(b[1]&0xe0)==0xe0))return "audio/mpeg";
  if(s.length()>=12&&s.startsWith("RIFF")&&s.substring(8).startsWith("WAVE"))return "audio/wav";
  if(Set.of("pdf","png","jpg","jpeg","gif","webp","mp4","mov","webm","mp3","wav").contains(ext))return null;
  if(Set.of("docx","xlsx","pptx","zip").contains(ext))return s.startsWith("PK")?"application/zip":null;
  if(Set.of("txt","csv","log").contains(ext)){for(byte value:b)if(value==0)return null;return "text/plain";}
  return "application/octet-stream";
 }

 @GetMapping("/attachments/{id}") @Transactional(readOnly=true) public ResponseEntity<Resource> download(@PathVariable String id) throws Exception {
  var a=attachments.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Attachment not found"));service.access(a.getRoomId());
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(a.getFileName(),StandardCharsets.UTF_8).build().toString()).contentType(MediaType.parseMediaType(a.getContentType())).contentLength(a.getSize()).body(files.read(a.getStorageKey()));
 }
}
