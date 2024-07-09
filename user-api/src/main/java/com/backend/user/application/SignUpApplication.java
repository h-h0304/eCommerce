package com.backend.user.application;

import com.backend.user.client.MailgunClient;
import com.backend.user.client.mailgun.SendMailForm;
import com.backend.user.domain.SignUpForm;
import com.backend.user.domain.model.Customer;
import com.backend.user.domain.model.Seller;
import com.backend.user.exception.CustomException;
import com.backend.user.exception.ErrorCode;
import com.backend.user.service.customer.SignUpCustomerService;
import com.backend.user.service.seller.SellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SignUpApplication {

    private final MailgunClient mailgunClient;
    private final SignUpCustomerService signUpCustomerService;
    private final SellerService sellerService;

    public void customerVerify(String email, String code) {
        signUpCustomerService.verifyEmail(email, code);
    }

    public void sellerVerify(String email, String code) {
        sellerService.verifyEmail(email, code);
    }

    public String customerSignUp(SignUpForm form) {
        if (signUpCustomerService.isEmailExist(form.getEmail())) {
            throw new CustomException(ErrorCode.ALREADY_REGISTERED_USER);
        } else {
            Customer c = signUpCustomerService.signUp(form);

            String code = getRandomCode();
            SendMailForm sendMailForm = SendMailForm.builder()
                    .from("test@gmail.com")
                    .to(form.getEmail())
                    .subject("Verification Email!")
                    .text(getVerificationEmailBody(form.getEmail(), form.getName(), "customer", code))
                    .build();
            log.info(mailgunClient.sendEmail(sendMailForm).getBody());

            signUpCustomerService.changeCustomerValidateEmail(c.getId(), code);
            return "회원 가입에 성공하였습니다.";
        }
    }

    public String sellerSignUp(SignUpForm form) {
        if (sellerService.isEmailExist(form.getEmail())) {
            throw new CustomException(ErrorCode.ALREADY_REGISTERED_USER);
        } else {
            Seller c = sellerService.signUp(form);

            String code = getRandomCode();
            SendMailForm sendMailForm = SendMailForm.builder()
                    .from("test@gmail.com")
                    .to(form.getEmail())
                    .subject("Verification Email!")
                    .text(getVerificationEmailBody(form.getEmail(), form.getName(), "seller", code))
                    .build();
            log.info(mailgunClient.sendEmail(sendMailForm).getBody());

            sellerService.changeSellerValidateEmail(c.getId(), code);
            return "회원 가입에 성공하였습니다.";
        }
    }

    private String getRandomCode() {
        return RandomStringUtils.random(10, true, true);
    }

    private String getVerificationEmailBody(String email, String name, String type, String code) {
        StringBuilder builder = new StringBuilder();
        return builder.append("Hello ").append(name)
                .append("! Please Click Link for verification.\n\n")
                .append("http://localhost:8081/signup/" + type + "/verify?email=")
                .append(email)
                .append("&code=")
                .append(code).toString();
    }
}