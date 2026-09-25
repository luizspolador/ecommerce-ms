package br.com.spolador.ecommerce.order_service.exception;

import lombok.Getter;

@Getter
public class ProductNotRegisteredException extends RuntimeException {
    private final String sku;

    public ProductNotRegisteredException(String sku) {
        super(String.format("Product with SKU '%s' is not registered in catalog. You can only create orders for registered products.", sku));
        this.sku = sku;
    }
}
