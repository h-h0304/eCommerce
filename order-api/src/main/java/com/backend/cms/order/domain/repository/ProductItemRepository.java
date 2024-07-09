package com.backend.cms.order.domain.repository;

import com.backend.cms.order.domain.model.ProductItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductItemRepository extends JpaRepository<ProductItem, Long> {

}
