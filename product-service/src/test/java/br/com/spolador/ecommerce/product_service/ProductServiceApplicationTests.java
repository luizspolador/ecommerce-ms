package br.com.spolador.ecommerce.product_service;

import br.com.spolador.ecommerce.product_service.repository.ProductRepository;
import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ProductServiceApplicationTests {

	@MockitoBean
	private ProductRepository productRepository;

	@MockitoBean
	private MongoClient mongoClient;

	@MockitoBean
	private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
		assertNotNull(productRepository);
		assertNotNull(mongoClient);
	}

}

