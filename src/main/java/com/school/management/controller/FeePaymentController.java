package com.school.management.controller;
import com.school.management.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/mobile/payments") @RequiredArgsConstructor
public class FeePaymentController {
 private final FeePaymentService payments;
 @PostMapping("/fees/{feeId}/checkout") public Map<String,Object> checkout(@PathVariable Long feeId){return payments.checkout(feeId);}
 @PostMapping("/{id}/verify") public Map<String,Object> verify(@PathVariable Long id){return payments.verify(id);}
}
