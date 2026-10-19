package com.school.management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class RazorpayGateway {
 private final String key,secret;
 private final ObjectMapper json;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 public RazorpayGateway(@Value("${school.payments.razorpay.key-id:}") String key,@Value("${school.payments.razorpay.key-secret:}") String secret,ObjectMapper json){this.key=key;this.secret=secret;this.json=json;}
 public boolean configured(){return !key.isBlank() && !secret.isBlank();}
 public JsonNode create(long amount,String reference){return request("POST","",Map.of("amount",amount,"currency","INR","accept_partial",false,"reference_id",reference,"description","School fee payment","expire_by",Instant.now().plusSeconds(1800).getEpochSecond(),"notify",Map.of("sms",false,"email",false),"reminder_enable",false));}
 public JsonNode fetch(String id){if(id==null || !id.matches("plink_[A-Za-z0-9]+"))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid provider payment reference");return request("GET",id,null);}
 private JsonNode request(String method,String path,Object body){
  if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Online payments need school Razorpay configuration");
  try{
   var builder=HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/payment_links/"+path)).timeout(Duration.ofSeconds(20)).header("Authorization","Basic "+Base64.getEncoder().encodeToString((key+":"+secret).getBytes(StandardCharsets.UTF_8))).header("Content-Type","application/json");
   builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
   var response=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()<200 || response.statusCode()>=300)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Payment provider is unavailable; please retry later");
   return json.readTree(response.body());
  }catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Payment verification interrupted");}
  catch(ResponseStatusException e){throw e;}
  catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Could not contact payment provider");}
 }
}
