# 🛒 E-Commerce Microservices Platform

Plataforma completa de comércio eletrônico baseada em arquitetura de microsserviços orientada a eventos (**Event-Driven Architecture - EDA**), com persistência poliglota, resiliência distribuída, segurança de ponta a ponta via OAuth2/OIDC e **100% de cobertura de testes automatizados**.

---

## 🏛 Arquitetura do Sistema

```mermaid
%%{init: {
  'theme': 'base',
  'themeVariables': {
    'primaryColor': '#1E293B',
    'primaryTextColor': '#FFFFFF',
    'primaryBorderColor': '#3B82F6',
    'lineColor': '#2563EB',
    'textColor': '#000000',
    'edgeLabelBackground': '#FFFFFF',
    'clusterBkg': 'transparent',
    'clusterBorder': '#64748B',
    'fontSize': '15px',
    'fontFamily': 'ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif'
  },
  'themeCSS': '.edgeLabel { color: #000000 !important; font-weight: bold; } .edgeLabel span { color: #000000 !important; } .label-container { fill: #FFFFFF !important; }'
}}%%
flowchart TD
    %% ==========================================
    %% CAMADA DE ACESSO, SEGURANÇA E GOVERNANÇA
    %% ==========================================
    Client["🌐 <big><b>Clientes & Aplicações</b></big><br/>• Web / Mobile / Postman<br/>• Requisições REST com Bearer JWT"]

    subgraph SecurityControl ["🔐 Identidade & Acesso (OAuth2 / OIDC)"]
        Keycloak["<big><b>Keycloak 24.0.1</b></big> (Port: 8080)<br/>• Realm: 'ecommerce-realm'<br/>• Emissor Central de JWT (RS256)<br/>• Roles: ROLE_ADMIN, ROLE_USER"]
        PostgresKC[("<b>PostgreSQL 16</b><br/>keycloak-db: 5433<br/>Tabelas IAM & Sessions")]
    end

    subgraph PlatformControl ["⚙️ Governança de Plataforma"]
        Eureka["<big><b>Netflix Eureka</b></big> (Port: 8761)<br/>• Service Discovery & Registry<br/>• Dynamic Instances & Heartbeat"]
        ConfigServer["<big><b>Spring Cloud Config</b></big> (Port: 8888)<br/>• Governança Centralizada (config-data)<br/>• Suporte a @RefreshScope em runtime"]
    end

    subgraph GatewayLayer ["🚪 API Gateway (Ponto Central de Entrada)"]
        Gateway["<big><b>Spring Cloud Gateway</b></big> (Port: 9001 - WebFlux Reativo)<br/>• Token Relay Filter (Encaminhamento de Bearer JWT aos serviços)<br/>• Roteamento Dinâmico com Spring Cloud LoadBalancer (lb://)<br/>• Validação de Escopos OAuth2, RBAC & Segurança Perimetral"]
    end

    %% ==========================================
    %% MICROSSERVIÇOS DE NEGÓCIO E PERSISTÊNCIA
    %% ==========================================
    subgraph ProductDomain ["📦 Domínio: Catálogo de Produtos"]
        ProductService["<big><b>Product Service</b></big> (Port: 8083)<br/>• Java 21 LTS + Virtual Threads (Loom)<br/>• CRUD Catálogo (GET/POST/PUT/DELETE) & Swagger<br/>• Validação de Unicidade de SKU (HTTP 409)<br/>• Emissor de 'ProductCreatedEvent'"]
        MongoDB[("<b>MongoDB 7.0</b><br/>product-db: 27017<br/>Collection: 'product'")]
    end

    subgraph InventoryDomain ["📊 Domínio: Estoque & Validação"]
        InventoryService["<big><b>Inventory Service</b></big> (Port: 8082)<br/>• Java 21 LTS + Virtual Threads (Loom)<br/>• Projeção Local: t_registered_product (Sync via EDA)<br/>• Validação Obrigatória: Rejeita SKU inexistente (HTTP 400)<br/>• Baixa Atômica com Lock Pessimista (SELECT ... FOR UPDATE)<br/>• Consumidor Idempotente (Tabela t_processed_order)<br/>• Endpoints: POST /api/v1/inventory | GET /api/v1/inventory/{sku}"]
        MySQLInv[("<b>MySQL 8.0</b><br/>inventory-db: 3307<br/>• t_inventory (Saldos em Estoque)<br/>• t_registered_product (Projeção CQRS)<br/>• t_processed_order (Idempotência)")]
    end

    subgraph OrderDomain ["🛒 Domínio: Pedidos & Orquestração"]
        OrderService["<big><b>Order Service</b></big> (Port: 8081)<br/>• Java 21 LTS + Virtual Threads (Loom)<br/>• Projeção Local: t_registered_product (Sync via EDA)<br/>• Validação Fail-Fast: Rejeita SKU não cadastrado (HTTP 400)<br/>• Ciclo de Vida: CREATED ➔ CONFIRMED / CANCELLED<br/>• Persistência do Motivo de Cancelamento (cancellationReason)<br/>• Transactional Outbox (t_outbox) + MessageRelayer<br/>• Resilience4j (Circuit Breaker & Retry com Backoff)<br/>• Proteção contra IDOR / BOLA (Validação de Ownership)<br/>• Endpoints: POST /api/v1/order | GET /api/v1/order/{id}"]
        PostgresOrder[("<b>PostgreSQL 16</b><br/>order-db: 5432<br/>• t_orders / t_order_line_items<br/>• t_registered_product (Projeção CQRS)<br/>• outbox_events (Outbox Pattern)")]
    end

    %% ==========================================
    %% MENSAGERIA ASSÍNCRONA E RESILIÊNCIA (EDA)
    %% ==========================================
    subgraph BrokerLayer ["📬 Mensageria Assíncrona (RabbitMQ 4.2 - AMQP :5672 / Web :15672)"]
        ExProduct["<b>TopicExchange: 'product-events'</b>"]
        ExOrder["<b>TopicExchange: 'order-events'</b>"]

        QProduct["<b>Queue: 'inventory-product-queue'</b><br/>Routing: product.created"]
        QOrderProd["<b>Queue: 'order-product-queue'</b><br/>Routing: product.created"]
        QInv["<b>Queue: 'inventory-queue'</b><br/>Routing: order.created (DLX: inventory-dlx)"]
        QConfirmed["<b>Queue: 'order-confirmed-queue'</b><br/>Routing: order.confirmed (DLX: order-dlx)"]
        QCancelled["<b>Queue: 'order-cancelled-queue'</b><br/>Routing: order.cancelled (DLX: order-dlx)"]
        QNotify["<b>Queue: 'notification-queue'</b><br/>Routing: order.confirmed / order.cancelled (DLX: notification-dlx)"]

        DLQ["<b>Dead Letter Queues (DLQ)</b><br/>• order-dlq | inventory-dlq | notification-dlq<br/>• Retenção e auditoria após esgotamento de retries"]
    end

    %% ==========================================
    %% NOTIFICAÇÕES E OBSERVABILIDADE
    %% ==========================================
    subgraph NotificationDomain ["📧 Notificações Transacionais"]
        NotificationService["<big><b>Notification Service</b></big> (Porta Dinâmica)<br/>• Java 21 LTS + Virtual Threads (Loom)<br/>• Consumidor AMQP com Retry Exponencial (3x)<br/>• Envio de e-mails transacionais (HTML/Text)"]
        MailServer["<b>Servidor SMTP</b><br/>• Mailtrap Sandbox :2525 (Dev)<br/>• Gmail SMTP TLS (Prod)"]
    end

    subgraph ObservabilityStack ["🔭 Observabilidade & Telemetria Unificada"]
        GrafanaLGTM["<big><b>Grafana LGTM Stack & OpenTelemetry</b></big><br/>• Grafana UI (Port: 3000) | OTLP HTTP (Port: 4318) / gRPC (Port: 4317)<br/>• Distributed Tracing (TraceId/SpanId W3C/B3), Logs & Métricas Actuator/Prometheus"]
    end

    %% ==========================================
    %% CONEXÕES E FLUXOS DO SISTEMA
    %% ==========================================
    Client -->|"<b><font color='#000000'>1. Autentica com credenciais (POST /token)</font></b>"| Keycloak
    Keycloak -.-> PostgresKC
    Client -->|"<b><font color='#000000'>2. Requisição REST com Header 'Authorization: Bearer JWT'</font></b>"| Gateway

    Gateway -.->|"<b><font color='#000000'>Resolução de rotas via Service Discovery</font></b>"| Eureka
    ConfigServer -.->|"<b><font color='#000000'>Propriedades centralizadas aos serviços (@RefreshScope)</font></b>"| Gateway

    Gateway -->|"<b><font color='#000000'>Route: /api/v1/product/**</font></b>"| ProductService
    Gateway -->|"<b><font color='#000000'>Route: /api/v1/inventory/**</font></b>"| InventoryService
    Gateway -->|"<b><font color='#000000'>Route: /api/v1/order/**</font></b>"| OrderService

    ProductService --> MongoDB
    InventoryService --> MySQLInv
    OrderService --> PostgresOrder

    ProductService -->|"<b><font color='#000000'>1. Publica 'product.created'</font></b>"| ExProduct
    ExProduct -->|"<b><font color='#000000'>Routing key: product.created</font></b>"| QProduct
    ExProduct -->|"<b><font color='#000000'>Routing key: product.created</font></b>"| QOrderProd
    QProduct -->|"<b><font color='#000000'>2a. Atualiza projeção local em t_registered_product</font></b>"| InventoryService
    QOrderProd -->|"<b><font color='#000000'>2b. Atualiza projeção local em t_registered_product</font></b>"| OrderService

    OrderService -->|"<b><font color='#000000'>1. Transactional Outbox publica 'order.created'</font></b>"| ExOrder
    ExOrder -->|"<b><font color='#000000'>Routing key: order.created</font></b>"| QInv
    QInv -->|"<b><font color='#000000'>2. Valida idempotência, lock pessimista e debita estoque</font></b>"| InventoryService

    InventoryService -->|"<b><font color='#000000'>3. Publica resultado da validação</font></b>"| ExOrder
    ExOrder -->|"<b><font color='#000000'>Routing key: order.confirmed</font></b>"| QConfirmed
    ExOrder -->|"<b><font color='#000000'>Routing key: order.cancelled</font></b>"| QCancelled
    ExOrder -->|"<b><font color='#000000'>Routing keys: order.confirmed / order.cancelled</font></b>"| QNotify

    QConfirmed -->|"<b><font color='#000000'>4a. Atualiza pedido para CONFIRMED</font></b>"| OrderService
    QCancelled -->|"<b><font color='#000000'>4b. Atualiza pedido para CANCELLED</font></b>"| OrderService

    QNotify -->|"<b><font color='#000000'>Consome evento final de pedido</font></b>"| NotificationService
    NotificationService -->|"<b><font color='#000000'>Envia e-mail de confirmação ou cancelamento</font></b>"| MailServer

    ProductService -.->|"<b><font color='#000000'>Traces & Logs OTLP</font></b>"| GrafanaLGTM
    OrderService -.->|"<b><font color='#000000'>Traces & Logs OTLP</font></b>"| GrafanaLGTM
    InventoryService -.->|"<b><font color='#000000'>Traces & Logs OTLP</font></b>"| GrafanaLGTM
    NotificationService -.->|"<b><font color='#000000'>Traces & Logs OTLP</font></b>"| GrafanaLGTM
    BrokerLayer -.->|"<b><font color='#000000'>Mensagens rejeitadas após 3 tentativas</font></b>"| DLQ

    %% ==========================================
    %% SUBGRAFOS: FUNDO TRANSPARENTE E BORDAS COLORIDAS (ADEUS FUNDO CINZA)
    %% ==========================================
    style SecurityControl fill:transparent,stroke:#9333EA,stroke-width:2px,stroke-dasharray: 4 4
    style PlatformControl fill:transparent,stroke:#64748B,stroke-width:2px,stroke-dasharray: 4 4
    style GatewayLayer fill:transparent,stroke:#0284C7,stroke-width:2px,stroke-dasharray: 4 4
    style ProductDomain fill:transparent,stroke:#2563EB,stroke-width:2px,stroke-dasharray: 4 4
    style InventoryDomain fill:transparent,stroke:#059669,stroke-width:2px,stroke-dasharray: 4 4
    style OrderDomain fill:transparent,stroke:#D97706,stroke-width:2px,stroke-dasharray: 4 4
    style BrokerLayer fill:transparent,stroke:#EA580C,stroke-width:2px,stroke-dasharray: 4 4
    style NotificationDomain fill:transparent,stroke:#9333EA,stroke-width:2px,stroke-dasharray: 4 4
    style ObservabilityStack fill:transparent,stroke:#0D9488,stroke-width:2px,stroke-dasharray: 4 4

    %% ==========================================
    %% CARDS COLORIDOS E VIBRANTES (ADEUS QUADRADOS BRANCOS)
    %% ==========================================
    classDef clientNode fill:#1E40AF,stroke:#93C5FD,stroke-width:2px,color:#FFFFFF;
    classDef secNode fill:#6B21A8,stroke:#D8B4FE,stroke-width:2px,color:#FFFFFF;
    classDef infraNode fill:#334155,stroke:#CBD5E1,stroke-width:2px,color:#FFFFFF;
    classDef gwNode fill:#0369A1,stroke:#7DD3FC,stroke-width:2px,color:#FFFFFF;
    classDef prodNode fill:#1D4ED8,stroke:#93C5FD,stroke-width:2px,color:#FFFFFF;
    classDef invNode fill:#047857,stroke:#6EE7B7,stroke-width:2px,color:#FFFFFF;
    classDef orderNode fill:#B45309,stroke:#FDE68A,stroke-width:2px,color:#FFFFFF;
    classDef dbMongo fill:#15803D,stroke:#86EFAC,stroke-width:2px,color:#FFFFFF;
    classDef dbMySQL fill:#0E7490,stroke:#67E8F9,stroke-width:2px,color:#FFFFFF;
    classDef dbPostgres fill:#3730A3,stroke:#C7D2FE,stroke-width:2px,color:#FFFFFF;
    classDef exchNode fill:#C2410C,stroke:#FED7AA,stroke-width:2px,color:#FFFFFF;
    classDef queueNode fill:#9A3412,stroke:#FDBA74,stroke-width:2px,color:#FFFFFF;
    classDef dlqNode fill:#991B1B,stroke:#FECACA,stroke-width:2px,color:#FFFFFF;
    classDef notifNode fill:#7E22CE,stroke:#E9D5FF,stroke-width:2px,color:#FFFFFF;
    classDef mailNode fill:#0F766E,stroke:#99F6E4,stroke-width:2px,color:#FFFFFF;
    classDef obsNode fill:#0F172A,stroke:#38BDF8,stroke-width:2px,color:#F8FAFC;

    class Client clientNode;
    class Keycloak secNode;
    class PostgresKC dbPostgres;
    class Eureka,ConfigServer infraNode;
    class Gateway gwNode;
    class ProductService prodNode;
    class InventoryService invNode;
    class OrderService orderNode;
    class MongoDB dbMongo;
    class MySQLInv dbMySQL;
    class PostgresOrder dbPostgres;
    class ExProduct,ExOrder exchNode;
    class QProduct,QOrderProd,QInv,QConfirmed,QCancelled,QNotify queueNode;
    class DLQ dlqNode;
    class NotificationService notifNode;
    class MailServer mailNode;
    class GrafanaLGTM obsNode;

    %% ==========================================
    %% SETAS EM DESTAQUE (GROSSAS, VIVAS E NÍTIDAS)
    %% ==========================================
    linkStyle default stroke:#2563EB,stroke-width:3px;
```

---

## 🛠 Tecnologias Utilizadas

| Categoria | Tecnologia | Versão | Aplicação / Finalidade |
| :--- | :--- | :--- | :--- |
| **Linguagem** | Java (OpenJDK) | **21 (LTS)** | Plataforma principal com suporte a **Virtual Threads (Project Loom)** |
| **Framework Base** | Spring Boot | **4.0.8** | Estrutura de injeção de dependências, REST APIs e ciclo de vida |
| **Service Discovery** | Spring Cloud Netflix Eureka | **2025.1.3** | Registro dinâmico e resolução de nomes das instâncias com load balancing |
| **API Gateway** | Spring Cloud Gateway (WebFlux) | **2025.1.3** | Roteamento reativo, validação de tokens JWT e *Token Relay* |
| **Configuração Central** | Spring Cloud Config Server | **2025.1.3** | Gestão externalizada de propriedades e perfis com suporte a `@RefreshScope` |
| **Identity & IAM** | Keycloak | **24.0.1** | Provedor de identidade OAuth2 / OpenID Connect com RBAC |
| **Mensageria** | RabbitMQ | **4.2-management** | Broker AMQP para orquestração de eventos assíncronos, DLQ e sincronização |
| **Banco NoSQL** | MongoDB | **7.0** | Armazenamento de catálogo de produtos com esquema flexível |
| **Banco Relacional** | PostgreSQL | **16-alpine** | Persistência de pedidos, eventos outbox e dados do Keycloak |
| **Banco Relacional** | MySQL | **8.0** | Persistência de estoque, projeção de catálogo e pedidos processados |
| **Resiliência** | Resilience4j | **2.3.0** | Circuit Breaker, Retry com backoff exponencial e Fallbacks |
| **Documentação API** | SpringDoc OpenAPI 3 / Swagger UI | **2.8.5** | Especificação e interface interativa dos endpoints REST |
| **Observabilidade** | OpenTelemetry & Spring Boot Actuator | - | Rastreamento distribuído, métricas e endpoints de saúde |
| **Mapeamento & Utilitários**| MapStruct & Lombok | - | Mapeamento DTO de alta performance e redução de boilerplate |
| **Containers** | Docker & Docker Compose | - | Orquestração unificada de toda a infraestrutura local |
| **Testes Automatizados** | JUnit 5, Mockito & JaCoCo | - | Testes unitários e de integração com **100% de cobertura** |

---

## 📦 Serviços da Aplicação

### 1. Discovery Server (`discovery-server`)
* **Porta**: `8761`
* **Função**: Catálogo e registro de serviços dinâmico baseado no Netflix Eureka. Permite escalabilidade horizontal com portas dinâmicas (`server.port=0`) e balanceamento de carga automático via *Spring Cloud LoadBalancer*.

### 2. Config Server (`config-server`)
* **Porta**: `8888`
* **Função**: Servidor centralizado de configurações externas integradas à pasta `config-data`. Fornece propriedades para os microsserviços via perfis (`dev`, `prod`) e suporta atualização em tempo de execução via `@RefreshScope` sem necessidade de reinício dos serviços.

### 3. API Gateway (`api-gateway`)
* **Porta**: `9001`
* **Função**: Ponto de entrada unificado para clientes. Atua como OAuth2 Resource Server validando tokens JWT emitidos pelo Keycloak, converte roles (`ADMIN`, `USER`), aplica segurança perimetral e repassa as credenciais via *Token Relay* aos serviços internos.

### 4. Product Service (`product-service`)
* **Porta**: `8083` | **Banco**: MongoDB (`product-db:27017` / Collection: `product`)
* **Função**: Gerencia o catálogo de produtos da plataforma (operações completas de CRUD).
  * **Validação de Unicidade de SKU**: O SKU é a chave identificadora universal do produto no ecossistema. O serviço garante unicidade com índice exclusivo no MongoDB (`@Indexed(unique = true)`) e validação prévia na criação (`POST`) e atualização (`PUT`), retornando `SkuAlreadyExistsException` (**HTTP 409 Conflict**) caso haja tentativa de duplicação.
  * **Sincronização EDA**: Ao criar um novo produto com sucesso, publica o evento `ProductCreatedEvent` na exchange `product-events` (`routing key: product.created`) para replicação assíncrona nas projeções dos demais serviços.
  * **Endpoints Disponíveis**:
    * `POST /api/v1/product` — Cadastro de novo produto (`ROLE_ADMIN`, valida SKU único)
    * `GET /api/v1/product` — Listagem paginada de todos os produtos (público)
    * `GET /api/v1/product/{id}` — Busca de produto por ID (público)
    * `PUT /api/v1/product/{id}` — Atualização cadastral (`ROLE_ADMIN`, valida se SKU pertence a outro produto)
    * `DELETE /api/v1/product/{id}` — Remoção de produto do catálogo (`ROLE_ADMIN`)

### 5. Inventory Service (`inventory-service`)
* **Porta**: `8082` | **Banco**: MySQL (`inventory-db:3307`)
* **Função**: Gerencia o estoque de produtos por código SKU.
  * **Projeção Local de Produtos**: Consome eventos de produtos criados e mantém a tabela `t_registered_product`.
  * **Validação de Catálogo**: Só permite cadastro ou alteração de estoque para produtos devidamente registrados no catálogo, rejeitando SKUs inexistentes com `ProductNotRegisteredException` (HTTP 400).
  * **Validação de Unicidade de SKU**: Cada produto possui um único registro de estoque. Tentativas de cadastrar ou atualizar para um SKU já existente em outro registro de inventário são rejeitadas com `SkuAlreadyExistsException` (HTTP 409 Conflict).
  * **Baixa de Estoque Atômica com Lock Pessimista**: `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) com ordenação alfabética de SKUs para prevenção de deadlocks.
  * **Consumidor Idempotente**: Controle via tabela `ProcessedOrder`, evitando duplicação de baixas por reprocessamento de mensagens.
  * **Endpoints de Baixa Direta**: Suporta operações manuais/administrativas de ajuste de estoque via `PUT /api/v1/inventory/reduce/{sku}`.

### 6. Order Service (`order-service`)
* **Porta**: `8081` | **Banco**: PostgreSQL (`order-db:5432`)
* **Função**: Responsável pelo ciclo de vida das ordens de compra.
  * **Projeção Local de Produtos**: Consome eventos `product.created` via fila `order-product-queue` e mantém a tabela `t_registered_product`.
  * **Validação Fail-Fast de Catálogo**: Valida imediatamente se todos os SKUs informados existem no catálogo registrado, rejeitando requisições com SKUs incorretos com `ProductNotRegisteredException` (HTTP 400 Bad Request) antes de abrir a transação do pedido.
  * **Rastreabilidade e Motivo de Cancelamento**: Persiste a justificativa detalhada de cancelamento (`cancellationReason`) na tabela `t_orders`, expondo-a no `OrderResponseDTO` para consulta transparente via `GET /api/v1/order/{id}`.
  * **Transactional Outbox**: Persiste pedido e evento na mesma transação atômica relacional, garantindo entrega confiável de mensagens mesmo em falhas do broker.
  * **Scheduler de Reenvio**: Processa eventos outbox pendentes em caso de indisponibilidade temporária do RabbitMQ.
  * **Proteção contra BOLA/IDOR**: Valida que clientes comuns só acessem seus próprios pedidos (`jwt.getSubject()`), mantendo visão global apenas para `ROLE_ADMIN`.
  * **Tolerância a Falhas**: Circuit Breaker e Retry com Resilience4j.

### 7. Notification Service (`notification-service`)
* **Porta**: Dinâmica | **Tipo**: Orientado puramente a eventos (AMQP)
* **Função**: Escuta eventos de status de pedidos no RabbitMQ (`order.confirmed` e `order.cancelled`) e envia e-mails transacionais formatados aos clientes via SMTP (Mailtrap em desenvolvimento / Gmail em produção). Possui Dead Letter Queue (`notification-dlq`) para retenção de mensagens com falha.

---

## 🔄 Fluxos de Negócio Assíncronos

### Fluxo 1: Sincronização de Catálogo (EDA - Event-Carried State Transfer)
1. Um administrador cadastra um novo produto via `POST /api/v1/product`.
2. O **Product Service** grava no MongoDB e publica o evento `ProductCreatedEvent` no RabbitMQ (`product.created`).
3. O **Inventory Service** e o **Order Service** consomem a mensagem em suas respectivas filas (`inventory-product-queue` e `order-product-queue`) e sincronizam suas projeções locais (`t_registered_product`).
4. Ao cadastrar estoque (`POST /api/v1/inventory`) ou criar pedidos (`POST /api/v1/order`), os sistemas validam o SKU em suas projeções locais com altíssima performance e sem chamadas HTTP bloqueantes.

```
POST /api/v1/product ──► [Product Service] ──► MongoDB (product-db)
                               │
                      Publica 'product.created'
                               │
                               ▼
                         [RabbitMQ Broker]
                        /                 \
     Queue: inventory-product-queue     Queue: order-product-queue
                      /                     \
                     ▼                       ▼
    [Inventory Service] ──► MySQL        [Order Service] ──► PostgreSQL
   (t_registered_product)               (t_registered_product)
```

---

### Fluxo 2: Pedidos, Orquestração e Compensação (Saga Coreografada & Outbox)
1. O cliente autenticado cria um pedido via `POST /api/v1/order` no Gateway.
2. O **Order Service** valida os SKUs na projeção local (rejeita imediatamente com **HTTP 400** se algum SKU não existir).
3. Se válido, persiste o pedido como `CREATED`, salva o evento no Transactional Outbox e retorna `201 Created`.
4. O evento `order.created` é publicado no RabbitMQ.
5. O **Inventory Service** consome a mensagem, valida a idempotência (`ProcessedOrder`) e executa o lock pessimista dos itens:
   * **Se houver estoque suficiente:** debita o saldo, registra o pedido como processado e publica `order.confirmed`.
   * **Se faltar estoque para qualquer item:** executa rollback transacional e publica `order.cancelled` com o motivo específico (`"Insufficient stock for SKU '...'"` ou `"Inventory not found with sku: '...'"`).
6. O **Order Service** consome a resposta:
   * Se confirmado: atualiza o pedido para `CONFIRMED`.
   * Se cancelado: atualiza o pedido para `CANCELLED` e salva o `cancellationReason`.
7. O **Notification Service** consome o evento final e dispara o e-mail transacional correspondente para o cliente.

---

## 🔒 Segurança e Controle de Acesso (RBAC)

* **Servidor de Identidade**: Keycloak 24.0.1 em container dedicado com PostgreSQL.
* **Autenticação**: OAuth2 / OpenID Connect com tokens JWT assinados digitalmente.
* **Defesa em Profundidade**: O Gateway aplica segurança perimetral e repassa as credenciais (*Token Relay*). Cada microsserviço atua de forma independente como OAuth2 Resource Server.
* **Mapeamento de Roles**: O `JwtAuthenticationConverter` extrai as roles de `realm_access.roles` do Keycloak e as injeta no contexto de segurança como `ROLE_ADMIN` e `ROLE_USER`.

---

## 📚 Documentação das APIs (Swagger UI) & Portais do Ecossistema

### 📑 Documentação Swagger / OpenAPI Direta dos Serviços

Cada microsserviço de negócio disponibiliza sua documentação OpenAPI interativa (Swagger UI) em sua respectiva porta HTTP:

| Microsserviço | Swagger UI Direto | OpenAPI Spec (JSON) | Porta Padrão |
| :--- | :--- | :--- | :---: |
| **Product Service** | [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html) | [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs) | `8083` |
| **Order Service** | [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html) | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) | `8081` |
| **Inventory Service** | [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html) | [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs) | `8082` |

> [!TIP]
> **Atenção às Portas no IntelliJ IDEA (`server.port=0`)**:
> - Se você estiver rodando os serviços via IntelliJ com a VM Option `-Dserver.port=0`, o Spring Boot alocará uma porta dinâmica e aleatória (ex: `58801`, `58720`).
> - Para que os serviços utilizem as **portas fixas oficiais** (`8081`, `8082`, `8083`), certifique-se de que a opção `-Dserver.port=0` **não** esteja presente em **Run ➔ Edit Configurations ➔ VM Options** ou no template Spring Boot do IntelliJ.
> - Caso use portas dinâmicas, basta verificar a porta alocada no console do serviço (`Tomcat started on port(s): XXXXX`) ou no dashboard do **Eureka** ([http://localhost:8761](http://localhost:8761)).

### Portais de Infraestrutura e Governança

| Plataforma / Serviço | URL de Acesso | Credenciais Padrão | Finalidade |
| :--- | :--- | :--- | :--- |
| **Netflix Eureka** | [http://localhost:8761](http://localhost:8761) | *Acesso Livre* | Service Discovery & catálogo de instâncias ativas |
| **Spring Cloud Config** | [http://localhost:8888](http://localhost:8888) | *Acesso Livre* | Servidor central de propriedades (`config-data`) |
| **API Gateway** | [http://localhost:9001](http://localhost:9001) | *Bearer JWT* | Ponto único de entrada e roteamento reativo |
| **Keycloak IAM** | [http://localhost:8080](http://localhost:8080) | `admin` / `admin` | Gestão de identidade, usuários, roles e tokens |
| **RabbitMQ Management** | [http://localhost:15672](http://localhost:15672) | `guest` / `guest` | Monitoramento de exchanges, filas e dead-letters |
| **Grafana Dashboard** | [http://localhost:3000](http://localhost:3000) | `admin` / `admin` | Telemetria, métricas Prometheus e rastreamento OTLP |

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
* **Java 21 JDK** instalado e configurado.
* **Maven 3.9+** instalado *(opcional caso utilize o **Maven Wrapper** incluso em cada microsserviço)*.
* **Docker & Docker Compose** em execução.

---

### Passo 1: Iniciar os Containers de Infraestrutura
Na raiz do projeto, inicie os bancos de dados, RabbitMQ e Keycloak:
```bash
docker compose up -d
```

---

### Passo 2: Configuração de Variáveis de Ambiente (Opcional)
Para garantir máxima segurança, nenhuma credencial sensível está gravada nos arquivos versionados. O projeto disponibiliza o arquivo [`/.env.example`](.env.example) com todas as variáveis suportadas:

* **API Gateway (`KEYCLOAK_CLIENT_SECRET`)**: Secret do client `api-gateway-client` configurado no Keycloak. Se não informado, adota o fallback `ecommerce-client-secret`.
* **Notification Service (Ambiente de Testes / Sandbox - Mailtrap)**:
  * Por padrão (perfil default), o serviço utiliza o **[Mailtrap](https://mailtrap.io)** para captura segura de e-mails transacionais (pedidos confirmados ou cancelados) sem envio a caixas de e-mail reais.
  * Basta criar uma conta gratuita no Mailtrap e exportar `MAILTRAP_USERNAME` e `MAILTRAP_PASSWORD` no terminal ou nas Run Configurations da IDE.
* **Notification Service (Ambiente Real - Gmail SMTP Opcional)**:
  * Caso queira enviar e-mails reais via Gmail, execute com o perfil `prod` (`--spring.profiles.active=prod`) definindo `GMAIL_USERNAME` e `GMAIL_APP_PASSWORD` (gerada em [Google Senhas de App](https://myaccount.google.com/apppasswords)).

---

### Passo 3: Ordem de Inicialização dos Microsserviços
Execute os serviços na seguinte sequência para garantir a resolução correta de configurações e descoberta:

```bash
# 1. Discovery Server (Porta 8761 - Eureka)
cd discovery-server && mvn spring-boot:run

# 2. Config Server (Porta 8888)
cd config-server && mvn spring-boot:run

# 3. API Gateway (Porta 9001)
cd api-gateway && mvn spring-boot:run

# 4. Microsserviços de Negócio (em terminais separados ou via Run Dashboard do IntelliJ)
cd product-service && mvn spring-boot:run
cd inventory-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
```

---

## 🧪 Qualidade e Testes Automatizados

O ecossistema conta com uma suíte abrangente de testes unitários e de integração utilizando **JUnit 5**, **Mockito**, **Spring Security Test** e **JaCoCo**:

* **Cobertura de Código**: **100%** de cobertura aferida via JaCoCo em todas as camadas de negócio, controllers, listeners e repositórios.
* **Total de Testes**: **325+ testes automatizados** (329 testes no total) com **0 falhas**.

### ⚙️ Como Executar os Testes

Como cada microsserviço é um projeto independente, navegue até a pasta do serviço desejado e execute os testes:

#### 1. Com Maven Global Instalado (`mvn`)
Caso possua o Maven configurado nas variáveis de ambiente (`PATH`):
```bash
# Exemplo no order-service:
cd order-service
mvn clean test jacoco:report

# Ou no inventory-service:
cd ../inventory-service
mvn clean test jacoco:report
```

#### 2. Sem Maven Instalado (Usando o Maven Wrapper Incluso)
Caso **não** tenha o Maven instalado no computador, utilize o **Maven Wrapper** pré-configurado na raiz de cada microsserviço:

* **No PowerShell / Prompt de Comando (Windows)**:
  ```powershell
  # Exemplo no inventory-service:
  cd inventory-service
  .\mvnw.cmd clean test jacoco:report

  # Exemplo no order-service:
  cd ../order-service
  .\mvnw.cmd clean test jacoco:report
  ```

* **No Git Bash / Linux / macOS**:
  ```bash
  # Exemplo no inventory-service:
  cd inventory-service
  ./mvnw clean test jacoco:report

  # Exemplo no order-service:
  cd ../order-service
  ./mvnw clean test jacoco:report
  ```

---

### 📊 Como Visualizar o Relatório de Cobertura JaCoCo (HTML)

Após a execução do comando `jacoco:report`, o relatório interativo e detalhado é gerado no diretório `target/site/jacoco/index.html` do respectivo serviço. Você pode abri-lo de três formas:

#### A. Diretamente no Navegador (Chrome, Edge, Firefox, etc.)
Copie e cole a URI direta com o protocolo `file:///` na barra de endereços do seu navegador:
* **Inventory Service**:
  ```text
  file:///C:/microservices-ecommerce/inventory-service/target/site/jacoco/index.html
  ```
* **Order Service**:
  ```text
  file:///C:/microservices-ecommerce/order-service/target/site/jacoco/index.html
  ```
* **Product Service**:
  ```text
  file:///C:/microservices-ecommerce/product-service/target/site/jacoco/index.html
  ```
* **API Gateway**:
  ```text
  file:///C:/microservices-ecommerce/api-gateway/target/site/jacoco/index.html
  ```

#### B. Pelo PowerShell
Estando no diretório do microsserviço:
```powershell
# Abre automaticamente o relatório no navegador padrão:
Start-Process "target\site\jacoco\index.html"

# Ou informando a URI direta completa:
Start-Process "file:///C:/microservices-ecommerce/inventory-service/target/site/jacoco/index.html"
```

#### C. Pelo Git Bash
Estando no diretório do microsserviço:
```bash
# Abre automaticamente no navegador padrão:
explorer "target/site/jacoco/index.html"

# Ou via comando start:
start "target/site/jacoco/index.html"
```