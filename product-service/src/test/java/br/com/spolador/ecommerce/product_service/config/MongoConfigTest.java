package br.com.spolador.ecommerce.product_service.config;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for MongoConfig")
class MongoConfigTest {

    @Test
    @DisplayName("getDatabaseName should return 'product-db'")
    void getDatabaseName_shouldReturnProductDb() {
        // Arrange
        MongoConfig mongoConfig = new MongoConfig();

        // Act
        String databaseName = mongoConfig.getDatabaseName();

        // Assert
        assertThat(databaseName).isEqualTo("product-db");
    }

    @Test
    @DisplayName("mongoClient should build and return a valid MongoClient instance")
    void mongoClient_shouldCreateMongoClientInstance() {
        // Arrange
        MongoConfig mongoConfig = new MongoConfig();
        ReflectionTestUtils.setField(mongoConfig, "host", "localhost");
        ReflectionTestUtils.setField(mongoConfig, "port", 27017);
        ReflectionTestUtils.setField(mongoConfig, "database", "product-db");
        ReflectionTestUtils.setField(mongoConfig, "username", "root");
        ReflectionTestUtils.setField(mongoConfig, "password", "password");
        ReflectionTestUtils.setField(mongoConfig, "authDatabase", "admin");

        // Act
        MongoClient client = mongoConfig.mongoClient();

        // Assert
        assertThat(client).isNotNull();

        // Cleanup
        client.close();
    }
}
