package br.com.spolador.ecommerce.product_service.service;

import br.com.spolador.ecommerce.product_service.dto.ProductRequestDTO;
import br.com.spolador.ecommerce.product_service.dto.ProductResponseDTO;

import java.util.List;

public interface ProductService {
    ProductResponseDTO createProduct(ProductRequestDTO requestDTO);
    List<ProductResponseDTO> getAllProducts();
    ProductResponseDTO getProductById(String id);
    ProductResponseDTO updateProduct(String id, ProductRequestDTO productRequestDTO);
    void deleteProductById(String id);
}
