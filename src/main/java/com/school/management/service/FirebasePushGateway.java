package com.school.management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class FirebasePushGateway {
 private final String credentialsPath;
 private final ObjectMapper json;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 private String bearer;private Instant expires=Instant.EPOCH;private JsonNode credentials;
 public FirebasePushGateway(@Value("${school.push.firebase.service-account:}") String path,ObjectMapper json){credentialsPath=path;this.json=json;}
 public boolean configured(){return !credentialsPath.isBlank();}
 private String base64(byte[] value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}
 private synchronized String token() throws Exception {
  if(bearer!=null && Instant.now().isBefore(expires))return bearer;
  credentials=json.readTree(Files.readString(Path.of(credentialsPath)));
  long now=Instant.now().getEpochSecond();
  String header=base64(json.writeValueAsBytes(Map.of("alg","RS256","typ","JWT")));
  String claims=base64(json.writeValueAsBytes(Map.of("iss",credentials.path("client_email").asText(),"scope","https://www.googleapis.com/auth/firebase.messaging","aud","https://oauth2.googleapis.com/token","iat",now,"exp",now+3600)));
  String unsigned=header+"."+claims;
  String pem=credentials.path("private_key").asText().replace("-----BEGIN PRIVATE KEY-----","").replace("-----END PRIVATE KEY-----","").replaceAll("\\s","");
  var key=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
  var signature=Signature.getInstance("SHA256withRSA");signature.initSign(key);signature.update(unsigned.getBytes(StandardCharsets.UTF_8));
  String assertion=unsigned+"."+base64(signature.sign());
  var request=HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token")).timeout(Duration.ofSeconds(20)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion="+URLEncoder.encode(assertion,StandardCharsets.UTF_8))).build();
  var response=http.send(request,HttpResponse.BodyHandlers.ofString());
  if(response.statusCode()!=200)throw new IllegalStateException("Firebase authorization failed");
  var result=json.readTree(response.body());bearer=result.path("access_token").asText();if(bearer.isBlank())throw new IllegalStateException("Firebase authorization missing");expires=Instant.now().plusSeconds(Math.max(1,result.path("expires_in").asLong(3600)-60));return bearer;
 }
 /** 200=delivered, 404=unregistered token, other status=retryable configuration/network failure. */
 public int send(String deviceToken,String title,String body,String notificationId) throws Exception {
  title=title==null?"School notification":title.substring(0,Math.min(120,title.length()));
  body=body==null?"":body.substring(0,Math.min(600,body.length()));
  String authorization=token();String project=credentials.path("project_id").asText();
  if(!project.matches("[a-zA-Z0-9-]+"))throw new IllegalStateException("Invalid Firebase project");
  var payload=Map.of("message",Map.of("token",deviceToken,"notification",Map.of("title",title,"body",body),"data",Map.of("notificationId",notificationId),"android",Map.of("collapse_key","school-"+notificationId,"notification",Map.of("tag","school-"+notificationId))));
  var request=HttpRequest.newBuilder(URI.create("https://fcm.googleapis.com/v1/projects/"+project+"/messages:send")).timeout(Duration.ofSeconds(20)).header("Authorization","Bearer "+authorization).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
  var response=http.send(request,HttpResponse.BodyHandlers.ofString());
  if(response.statusCode()==401){bearer=null;expires=Instant.EPOCH;}
  if(response.statusCode()==404 && !response.body().contains("UNREGISTERED"))return 503;
  return response.statusCode();
 }
}
