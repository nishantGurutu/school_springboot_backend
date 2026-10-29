package com.school.management.controller;

import com.school.management.service.PushDeviceService;
import com.school.management.service.PortalAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/mobile/push-devices") @RequiredArgsConstructor
public class PushDeviceController {
    private final PushDeviceService devices;
    private final PortalAccessService access;
    public record Registration(String token) {}
    @PostMapping public void register(@RequestBody Registration request) { devices.register(access.actor().getId(), request.token()); }
    @PostMapping("/remove") public void remove(@RequestBody Registration request) { devices.remove(access.actor().getId(), request.token()); }
}
