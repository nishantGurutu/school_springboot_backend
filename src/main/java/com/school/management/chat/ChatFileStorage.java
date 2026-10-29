package com.school.management.chat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.sync.RequestBody;
import java.nio.file.*;
import java.net.URI;

@Service
public class ChatFileStorage {
 private final Path root;
 private final String bucket;
 private final S3Client s3;
 public ChatFileStorage(@Value("${chat.storage.path:./private-chat-files}") String path,@Value("${chat.storage.bucket:}") String bucket,@Value("${chat.storage.region:ap-south-1}") String region,@Value("${chat.storage.endpoint:}") String endpoint){
  this.root=Path.of(path).toAbsolutePath().normalize();this.bucket=bucket;
  if(!bucket.isBlank()){var builder=S3Client.builder().region(Region.of(region)).forcePathStyle(!endpoint.isBlank());if(!endpoint.isBlank())builder.endpointOverride(URI.create(endpoint));s3=builder.build();}else s3=null;
 }
 public String put(String key,byte[] bytes,String type) throws Exception {
  if(s3!=null){s3.putObject(b->b.bucket(bucket).key(key).contentType(type),RequestBody.fromBytes(bytes));return "s3:"+key;}
  Files.createDirectories(root);Files.write(root.resolve(key),bytes,StandardOpenOption.CREATE_NEW);return "local:"+key;
 }
 public Resource read(String stored) throws Exception {
  if(stored.startsWith("s3:")){if(s3==null)throw new IllegalStateException("S3 storage not configured");return new InputStreamResource(s3.getObject(b->b.bucket(bucket).key(stored.substring(3))));}
  Path path=root.resolve(stored.substring(6)).normalize();if(!path.startsWith(root))throw new IllegalArgumentException("Invalid file key");return new FileSystemResource(path);
 }
}
