package com.school.management.config;
import com.school.management.entity.FeePaymentMethodEntity;
import com.school.management.repository.FeePaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Component @RequiredArgsConstructor
public class FeePaymentMethodInitializer implements ApplicationRunner {
    private final FeePaymentMethodRepository methods;
    @Override @Transactional public void run(ApplicationArguments args) {
        if (methods.count() == 0) {
            methods.saveAll(List.of("Cash", "Bank Transfer", "Card", "UPI", "Cheque").stream()
                .map(name -> FeePaymentMethodEntity.builder().name(name).build()).toList());
        }
    }
}
