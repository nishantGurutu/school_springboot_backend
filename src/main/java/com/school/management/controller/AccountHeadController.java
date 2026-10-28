package com.school.management.controller;
import com.school.management.entity.AccountHeadEntity;
import com.school.management.repository.AccountHeadRepository;
import com.school.management.exceptions.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/accounts/heads") @RequiredArgsConstructor
public class AccountHeadController {
 private final AccountHeadRepository heads;
 public record HeadRequest(@NotBlank @Pattern(regexp="INCOME|EXPENSE") String transactionType,
   @NotBlank @Size(max=64) String name,@Size(max=255) String description,@Size(max=64) String section,@Pattern(regexp="Active|Inactive") String status) {}
 @GetMapping public List<AccountHeadEntity> list(){return heads.findAll();}
 @PostMapping @Transactional public ResponseEntity<AccountHeadEntity> create(@Valid @RequestBody HeadRequest request){
  return ResponseEntity.status(HttpStatus.CREATED).body(heads.save(apply(new AccountHeadEntity(),request)));
 }
 @PutMapping("/{id}") @Transactional public AccountHeadEntity update(@PathVariable Long id,@Valid @RequestBody HeadRequest request){
  return heads.save(apply(heads.findById(id).orElseThrow(()->new ResourceNotFoundException("Account head not found")),request));
 }
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id){
  heads.delete(heads.findById(id).orElseThrow(()->new ResourceNotFoundException("Account head not found")));return ResponseEntity.noContent().build();
 }
 private AccountHeadEntity apply(AccountHeadEntity entity,HeadRequest request){
  String name=request.name().trim();
  if ((entity.getId()==null || !name.equalsIgnoreCase(entity.getName()) || !request.transactionType().equals(entity.getTransactionType()))
     && heads.existsByTransactionTypeAndNameIgnoreCase(request.transactionType(),name)) throw new DuplicateResourceException("This account head already exists");
  entity.setName(name);entity.setTransactionType(request.transactionType());entity.setDescription(request.description());entity.setSection(request.section());entity.setStatus(request.status()==null?"Active":request.status());return entity;
 }
}
