package br.com.spolador.ecommerce.product_service.service.impl;

import br.com.spolador.ecommerce.product_service.dto.ProductRequestDTO;
import br.com.spolador.ecommerce.product_service.dto.ProductResponseDTO;
import br.com.spolador.ecommerce.product_service.event.ProductCreatedEvent;
import br.com.spolador.ecommerce.product_service.exception.ResourceNotFoundException;
import br.com.spolador.ecommerce.product_service.mapper.ProductMapper;
import br.com.spolador.ecommerce.product_service.model.Product;
import br.com.spolador.ecommerce.product_service.repository.ProductRepository;
import br.com.spolador.ecommerce.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO requestDTO) {
        Product product = productMapper.toProduct(requestDTO);
        Product createdProduct = productRepository.save(product);
        log.info("product {} created with sku: {}", createdProduct.getName(), createdProduct.getSku());

        ProductCreatedEvent event = new ProductCreatedEvent(
                createdProduct.getId(),
                createdProduct.getSku(),
                createdProduct.getName(),
                createdProduct.getPrice()
        );
        rabbitTemplate.convertAndSend("product-events", "product.created", event);
        log.info("ProductCreatedEvent sent to RabbitMQ for sku: {}", createdProduct.getSku());

        return productMapper.toProductResponseDTO(createdProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream().map(productMapper::toProductResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(String id) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Product", "id", id)
        );
        return productMapper.toProductResponseDTO(product);
    }

    @Override
    @Transactional
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
    @Transactional
    public void deleteProductById(String id) {
        if(!productRepository.existsById(id)){
            throw new ResourceNotFoundException("Product", "id", id);
        }
        productRepository.deleteById(id);
        log.info("product with id:{} deleted", id);
    }
}
