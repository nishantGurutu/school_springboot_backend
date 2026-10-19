package com.school.management.controller;

import com.school.management.service.FeePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController @RequestMapping("/api/mobile/payments") @RequiredArgsConstructor
public class FeePaymentController {
 private final FeePaymentService payments;
 @PostMapping("/fees/{feeId}/checkout") public Map<String,Object> checkout(@PathVariable Long feeId){return payments.checkout(feeId);}
 @PostMapping("/{id}/verify") public Map<String,Object> verify(@PathVariable Long id){return payments.verify(id);}
}
