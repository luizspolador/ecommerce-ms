package br.com.spolador.ecommerce.product_service.service.impl;

import br.com.spolador.ecommerce.product_service.dto.ProductRequestDTO;
import br.com.spolador.ecommerce.product_service.dto.ProductResponseDTO;
import br.com.spolador.ecommerce.product_service.exception.ResourceNotFoundException;
import br.com.spolador.ecommerce.product_service.mapper.ProductMapper;
import br.com.spolador.ecommerce.product_service.model.Product;
import br.com.spolador.ecommerce.product_service.repository.ProductRepository;
import br.com.spolador.ecommerce.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO requestDTO) {
        Product product = productMapper.toProduct(requestDTO);
        Product createdProduct = productRepository.save(product);
        log.info("product {} created", createdProduct.getName());
        return productMapper.toProductResponseDTO(createdProduct);
    }

    @Override
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream().map(productMapper::toProductResponseDTO).toList();
    }

    @Override
    public ProductResponseDTO getProductById(String id) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Product", "id", id)
        );
        return productMapper.toProductResponseDTO(product);
    }

    @Override
    public ProductResponseDTO updateProduct(String id, ProductRequestDTO productRequest) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Product", "id", id)
        );
        productMapper.updateProductFromRequest(productRequest, product);
        Product updatedProduct = productRepository.save(product);
        log.info("product {} updated", updatedProduct.getName());
        return productMapper.toProductResponseDTO(updatedProduct);
    }

    @Override
    public void deleteProductById(String id) {
        if(!productRepository.existsById(id)){
            throw new ResourceNotFoundException("Product", "id", id);
        }
        productRepository.deleteById(id);
        log.info("product with id:{} deleted", id);
    }
}
