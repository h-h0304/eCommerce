package com.backend.cms.order.application;

import com.backend.cms.order.domain.model.Product;
import com.backend.cms.order.domain.model.ProductItem;
import com.backend.cms.order.domain.product.AddProductCartForm;
import com.backend.cms.order.domain.redis.Cart;
import com.backend.cms.order.exception.CustomException;
import com.backend.cms.order.exception.ErrorCode;
import com.backend.cms.order.service.CartService;
import com.backend.cms.order.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartApplication {

    private final ProductSearchService productSearchService;
    private final CartService cartService;

    public Cart addCart(Long customerId, AddProductCartForm form) {
        Product product = productSearchService.getByProductId(form.getId());

        if (product == null) {
            throw new CustomException(ErrorCode.NOT_FOUND_PRODUCT);
        }

        Cart cart = cartService.getCart(customerId);

        if (!addAble(cart, product, form)) {
            throw new CustomException(ErrorCode.ITEM_COUNT_NOT_ENOUGH);
        }

        return cartService.addCart(customerId, form);
    }

    /**
     * 엣지 케이스
     *
     * @param customerId
     * @param cart
     * @return
     */
    public Cart updateCart(Long customerId, Cart cart) {
        // 실질적으로 변하는 데이터
        // 상품의 삭제, 수량 변경
        cartService.putCart(customerId, cart);
        return getCart(customerId);
    }

    // 1. 장바구니에 상품을 추가했다
    // 2. 상품의 가격이나 수량이 변동 된다.
    public Cart getCart(Long customerId) {

        Cart cart = refreshCart(cartService.getCart(customerId));
        cartService.putCart(cart.getCustomerId(), cart);

        Cart returnCart = new Cart();
        returnCart.setCustomerId(customerId);
        returnCart.setProducts(cart.getProducts());
        returnCart.setMessages(cart.getMessages());
        cart.setMessages(new ArrayList<>());
        return returnCart;

        // 3. 메세지를 보고 난 다음에는, 이미 본 메세지는 스팸이 되기 때문에 삭제한다.

    }

    public void clearCart(Long customerId) {
        cartService.putCart(customerId, null);
    }

    protected Cart refreshCart(Cart cart) {
        // 1. 상품이나 상품의 아이템의 정보, 가격 수량이 변경되었는지 체크

        // 2. 변경되었다면 그에 맞는 알람 제공해준다,

        // 4. 상품의 수량, 가격을 우리가 임의로 변경한다


        Map<Long, Product> productMap = productSearchService.getListByProductIds(
                        cart.getProducts().stream().map(Cart.Product::getId).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        for (int i = 0; i < cart.getProducts().size(); i++) {
            Cart.Product cartProduct = cart.getProducts().get(i);

            Product p = productMap.get(cartProduct.getId());

            if (p == null) {
                cart.getProducts().remove(cartProduct);
                i--;
                cart.addMessage(cartProduct.getName() + " 상품이 삭제되었습니다.");
                continue;
            }

            Map<Long, ProductItem> productItemMap = p.getProductItems().stream()
                    .collect(Collectors.toMap(ProductItem::getId, productItem -> productItem));


            List<String> tmpMessages = new ArrayList<>();
            for (int j = 0; j < cartProduct.getItems().size(); j++) {
                Cart.ProductItem cartProductItem = cartProduct.getItems().get(j);

                ProductItem pi = productItemMap.get(cartProductItem.getId());

                if (pi == null) {
                    cartProduct.getItems().remove(cartProductItem);
                    j--;
                    tmpMessages.add(cartProductItem.getName() + " 옵션이 삭제되었습니다.");
                    continue;
                }

                boolean isPriceChanged = false;
                boolean isCountNotEnough = false;

                if (!cartProductItem.getPrice().equals(pi.getPrice())) {
                    isPriceChanged = true;
                    cartProductItem.setPrice(pi.getPrice());
                }

                if (cartProductItem.getCount() > (pi.getCount())) {
                    isCountNotEnough = true;
                    cartProductItem.setCount(pi.getCount());
                }

                if (isCountNotEnough && isPriceChanged) {
                    // message 1
                    tmpMessages.add(cartProductItem.getName() + " 가격변동, 수량이 부족하여 구매 가능한 최대치로 변경되었습니다.");
                } else if (isPriceChanged) {
                    // message 2
                    tmpMessages.add(cartProductItem.getName() + " 가격이 변동되었습니다.");
                } else if (isCountNotEnough) {
                    tmpMessages.add(cartProductItem.getName() + " 수량이 부족하여 구매 가능한 최대치로 변경되었습니다.");
                }
            }

            if (cartProduct.getItems().isEmpty()) {
                cart.getProducts().remove(cartProduct);
                i--;
                cart.addMessage(cartProduct.getName() + " 상품의 옵션이 모두 없어져 구매가 불가능합니다.");
            } else if (!tmpMessages.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                builder.append(cartProduct.getName() + " 상품의 변동 사항 : ");
                for (String message : tmpMessages) {
                    builder.append(message);
                    builder.append(", ");
                }
                cart.addMessage(builder.toString());
            }
        }
        return cart;
    }

    /**
     * cart에 cartItem이 추가될 수 있는지 검사
     *
     * @param cart
     * @param product
     * @param form
     * @return
     */
    private boolean addAble(Cart cart, Product product, AddProductCartForm form) {
        Cart.Product cartProduct = cart.getProducts().stream().filter(p -> p.getId().equals(form.getId()))
                .findFirst().orElse(
                        Cart.Product.builder()
                                .id(product.getId())
                                .items(Collections.emptyList()).build()
                );

        Map<Long, Integer> cartItemCountMap = cartProduct.getItems().stream()
                .collect(Collectors.toMap(Cart.ProductItem::getId, Cart.ProductItem::getCount));

        Map<Long, Integer> currentItemCountMap = product.getProductItems().stream()
                .collect(Collectors.toMap(ProductItem::getId, ProductItem::getCount));

        return form.getItems().stream().noneMatch(
                formItem -> {
                    Integer cartCount = cartItemCountMap.get(formItem.getId());
                    if (cartCount == 0) {
                        cartCount = 0;
                    }
                    Integer currentCount = currentItemCountMap.get(formItem.getId());
                    return formItem.getCount() + cartCount > currentCount;
                });
    }
}
