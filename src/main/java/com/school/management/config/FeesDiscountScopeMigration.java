package com.school.management.config;
import com.school.management.repository.FeesDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// The old discount form never offered a fee scope; it silently submitted this placeholder.
@Component @Order(0) @RequiredArgsConstructor
public class FeesDiscountScopeMigration implements ApplicationRunner {
    private final FeesDiscountRepository discounts;
    @Override @Transactional public void run(ApplicationArguments args) {
        discounts.findAll().stream()
            .filter(d -> "May month fees, Admission fees, Exam fees".equals(d.getFeesType()))
            .forEach(d -> { d.setFeesType("All"); discounts.save(d); });
    }
}
