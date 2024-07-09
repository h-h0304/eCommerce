package com.backend.user.application;

import com.backend.domain.config.JwtAuthenticationProvider;
import com.backend.domain.domain.common.UserType;
import com.backend.user.domain.SignInForm;
import com.backend.user.domain.model.Customer;
import com.backend.user.domain.model.Seller;
import com.backend.user.exception.CustomException;
import com.backend.user.exception.ErrorCode;
import com.backend.user.service.customer.CustomerService;
import com.backend.user.service.seller.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignInApplication {

    private final CustomerService customerService;
    private final SellerService sellerService;
    private final JwtAuthenticationProvider provider;

    public String customerLoginToken(SignInForm form) {
        Customer c = customerService.findValidCustomer(form.getEmail(), form.getPassword())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_CHECK_FAIL));

        return provider.createToken(c.getEmail(), c.getId(), UserType.CUSTOMER);
    }

    public String sellerLoginToken(SignInForm form) {
        Seller s = sellerService.findValidSeller(form.getEmail(), form.getPassword())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_CHECK_FAIL));

        return provider.createToken(s.getEmail(), s.getId(), UserType.SELLER);
    }
}