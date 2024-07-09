package com.backend.user.service;

import com.backend.user.domain.SignUpForm;
import com.backend.user.domain.model.Customer;
import com.backend.user.service.customer.SignUpCustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SignUpCustomerServiceTest {

    @Autowired
    private SignUpCustomerService service;

    @Test
    void signUp() {
        SignUpForm form = SignUpForm.builder()
                .name("name")
                .birth(LocalDate.now())
                .email("abc@naver.com")
                .password("1")
                .phone("01012345678")
                .build();

        Customer c= service.signUp(form);

        assertNotNull(c.getId());
        assertNotNull(c.getCreatedAt());

    }
}