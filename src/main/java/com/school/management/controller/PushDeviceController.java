package com.school.management.controller;

import com.school.management.entity.PushDeviceEntity;
import com.school.management.repository.PushDeviceRepository;
import com.school.management.service.PortalAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

@RestController @RequestMapping("/api/mobile/push-devices") @RequiredArgsConstructor @Transactional
public class PushDeviceController {
 private final PushDeviceRepository devices;
 private final PortalAccessService access;
 public record Registration(String token){}
 private String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 @PostMapping public void register(@RequestBody Registration request){
  if(request.token()==null || request.token().length()<20 || request.token().length()>4096)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid device token");
  devices.save(PushDeviceEntity.builder().id(hash(request.token())).token(request.token()).userId(access.actor().getId()).build());
 }
 @PostMapping("/remove") public void remove(@RequestBody Registration request){if(request.token()==null)return;devices.findById(hash(request.token())).filter(d->Objects.equals(d.getUserId(),access.actor().getId())).ifPresent(devices::delete);}
}
