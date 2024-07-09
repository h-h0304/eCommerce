package com.backend.cms.order.application;

import com.backend.cms.order.client.UserClient;
import com.backend.cms.order.client.user.ChangeBalanceForm;
import com.backend.cms.order.client.user.CustomerDto;
import com.backend.cms.order.domain.model.ProductItem;
import com.backend.cms.order.domain.redis.Cart;
import com.backend.cms.order.exception.CustomException;
import com.backend.cms.order.exception.ErrorCode;
import com.backend.cms.order.service.ProductItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class OrderApplication {

    private final CartApplication cartApplication;
    private final UserClient userClient;
    private final ProductItemService productItemService;


    // 결제를 위해 필요한 것
    // 1번 : 물건들이 전부 주문 가능한 상태인지 확인
    // 2번 : 가격 변동이 있었는지에 대해 확인
    // 3번 : 고객의 돈이 충분한지
    // 4번 : 결제 & 상품의 재고 관리
    @Transactional
    public void order(String token, Cart cart) {
        // 1번 케이스 : 주문 시 기존 카트 버림
        // 2번 케이스 : (선택주문) 주문하지 않은 아이템은 삭제되면 안됨

        // 1번 케이스로 진행
        Cart orderCart = cartApplication.refreshCart(cart);
        if (!orderCart.getMessages().isEmpty()) {
            // 문제가 있음
            throw new CustomException(ErrorCode.ORDER_FAIL_CHECK_CART);
        }

        // 3번 : 고객의 돈이 충분한지
        CustomerDto customerDto = userClient.getCustomerInfo(token).getBody();

        int totalPrice = getTotalPrice(cart);
        if (customerDto.getBalance() < totalPrice) {
            throw new CustomException(ErrorCode.ORDER_FAIL_NO_MONEY);
        }

        // RollBack 계획에 대해 생각해야 함.
        userClient.changeBalance(token,
                ChangeBalanceForm.builder()
                        .from("USER")
                        .message("Order")
                        .money(-totalPrice)
                        .build()
        );

        // 4번 : 결제 & 상품의 재고 관리
        for(Cart.Product product : orderCart.getProducts()) {
            for(Cart.ProductItem cartItem: product.getItems()) {
                ProductItem productItem = productItemService.getProductItem(cartItem.getId());
                productItem.setCount(productItem.getCount() - cartItem.getCount());
            }
        }
    }


    /**
     * 장바구니에 담긴 상품의 총합 금액 구하기
     *
     * @param cart 장바구니
     * @return 장바구니에 담긴 물품 총합계액
     */
    private Integer getTotalPrice(Cart cart) {
        return cart.getProducts().stream().flatMapToInt(
                product -> product.getItems().stream().flatMapToInt(
                        productItem ->
                                IntStream.of(productItem.getPrice() * productItem.getCount())
                )
        ).sum();
    }


}
