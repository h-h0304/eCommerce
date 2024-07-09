package com.backend.user.service.seller;

import com.backend.user.domain.SignUpForm;
import com.backend.user.domain.model.Seller;
import com.backend.user.domain.repository.SellerRepository;
import com.backend.user.exception.CustomException;
import com.backend.user.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;

    public Optional<Seller> findByIdAndEmail(Long id, String email) {
        return sellerRepository.findByIdAndEmail(id, email);
    }

    public Optional<Seller> findValidSeller(String email, String password) {
        return sellerRepository.findByEmailAndPasswordAndVerifyIsTrue(email, password);
    }

    public Seller signUp(SignUpForm form) {
        return sellerRepository.save(Seller.from(form));
    }

    public boolean isEmailExist(String email) {
        return sellerRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public void verifyEmail(String email, String code) {
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        if (seller.isVerify()) {
            throw new CustomException(ErrorCode.ALREADY_VERIFIED);
        }
        if (!seller.getVerificationCode().equals(code)) {
            throw new CustomException(ErrorCode.WRONG_VERIFICATION);
        }
        if (seller.getVerifyExpiredAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.CODE_EXPIRED);
        }
        seller.setVerify(true);
    }

    @Transactional
    public LocalDateTime changeSellerValidateEmail(Long customerId, String verificationCode) {
        Optional<Seller> sellerOptional = sellerRepository.findById(customerId);

        if (sellerOptional.isPresent()) {
            Seller seller = sellerOptional.get();
            seller.setVerificationCode(verificationCode);
            seller.setVerifyExpiredAt(LocalDateTime.now().plusDays(1));
            return seller.getVerifyExpiredAt();
        }

        throw new CustomException(ErrorCode.NOT_FOUND_USER);
    }
}