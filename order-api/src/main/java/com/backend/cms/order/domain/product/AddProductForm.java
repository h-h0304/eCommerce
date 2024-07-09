package com.backend.cms.order.domain.product;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddProductForm {

    private String name;
    private String description;
    List<AddProductItemForm> items;
}
