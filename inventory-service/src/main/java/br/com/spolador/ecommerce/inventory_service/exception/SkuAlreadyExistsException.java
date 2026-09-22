package br.com.spolador.ecommerce.inventory_service.exception;

import lombok.Getter;

@Getter
public class SkuAlreadyExistsException extends RuntimeException {
    private final String sku;

    public SkuAlreadyExistsException(String sku) {
        super(String.format("The inventory for SKU '%s' already exists", sku));
        this.sku = sku;
    }
}
