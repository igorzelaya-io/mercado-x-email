# Graph Report - .  (2026-08-21)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 319 nodes · 594 edges · 20 communities (19 shown, 1 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 28 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b601ac2e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- WhatsAppNotificationService
- WhatsAppPayloadReaderService
- EmailOrchestratorService
- MercadoXEmailOAuthConfig.java
- NotificationTemplateService
- DltIntegrationTest.java
- WhatsAppEventListenerTest
- WhatsAppApiException
- ReplayableServletInputStream
- LeadWelcomeWhatsAppHandler
- TestKafkaErrorHandlerConfig.java
- .build
- mvnw
- WhatsAppEventListener.java
- KafkaWhatsAppEventPublisher.java
- MercadoXEmailApplication
- WhatsAppWebClientConfig.java
- TemplateVariableValidator
- MercadoXEmailApplicationTests.java
- hn.shadowcore:mercado-x-email

## God Nodes (most connected - your core abstractions)
1. `WhatsAppNotificationService` - 18 edges
2. `WhatsAppNotificationServiceTest` - 17 edges
3. `NotificationTemplateService` - 16 edges
4. `EmailOrchestratorService` - 16 edges
5. `DltIntegrationTest` - 16 edges
6. `WhatsAppEventListenerTest` - 16 edges
7. `WhatsAppPayloadReaderService` - 13 edges
8. `MercadoXEmailOAuthConfig` - 10 edges
9. `WhatsAppSignatureVerificationFilter` - 10 edges
10. `EmailEventListener` - 9 edges

## Surprising Connections (you probably didn't know these)
- `WhatsAppEventListener` --references--> `WhatsAppNotificationService`  [EXTRACTED]
  src/main/java/hn/shadowcore/mercadox/email/listener/WhatsAppEventListener.java → src/main/java/hn/shadowcore/mercadox/email/service/whatsapp/WhatsAppNotificationService.java
- `EmailOrchestratorService` --references--> `NotificationTemplateService`  [EXTRACTED]
  src/main/java/hn/shadowcore/mercadox/email/service/mailer/EmailOrchestratorService.java → src/main/java/hn/shadowcore/mercadox/email/service/NotificationTemplateService.java
- `WhatsAppNotificationService` --references--> `NotificationTemplateService`  [EXTRACTED]
  src/main/java/hn/shadowcore/mercadox/email/service/whatsapp/WhatsAppNotificationService.java → src/main/java/hn/shadowcore/mercadox/email/service/NotificationTemplateService.java
- `WhatsAppNotificationServiceTest` --references--> `NotificationTemplateService`  [EXTRACTED]
  src/test/java/hn/shadowcore/mercadox/email/service/whatsapp/WhatsAppNotificationServiceTest.java → src/main/java/hn/shadowcore/mercadox/email/service/NotificationTemplateService.java
- `EmailOrchestratorService` --references--> `TemplateVariableValidator`  [EXTRACTED]
  src/main/java/hn/shadowcore/mercadox/email/service/mailer/EmailOrchestratorService.java → src/main/java/hn/shadowcore/mercadox/email/util/TemplateVariableValidator.java

## Import Cycles
- None detected.

## Communities (20 total, 1 thin omitted)

### Community 0 - "WhatsAppNotificationService"
Cohesion: 0.11
Nodes (19): ExtendWith, JsonIgnoreProperties, ResponseSpec, AbstractWhatsAppNotificationHandler, NotificationRequest, Message, WhatsAppMessageResponse, Service (+11 more)

### Community 1 - "WhatsAppPayloadReaderService"
Cohesion: 0.11
Nodes (22): GetMapping, OrganizationWhatsAppConfig, OrganizationWhatsAppConfigRepository, PostMapping, RequestMapping, RequiredArgsConstructor, ResponseEntity, RestController (+14 more)

### Community 2 - "EmailOrchestratorService"
Cohesion: 0.15
Nodes (19): EmailRecipient, JavaMailSender, KafkaOrgIdPropagated, OrderEmailEvent, EmailEventListener, Component, ConsumerRecord, KafkaIdempotent (+11 more)

### Community 3 - "MercadoXEmailOAuthConfig.java"
Cohesion: 0.14
Nodes (20): AnonymousTenantValidator, EnableMethodSecurity, EnableWebSecurity, FilterChain, FilterRegistrationBean, HttpSecurity, HttpServletResponse, JwtVerifier (+12 more)

### Community 4 - "NotificationTemplateService"
Cohesion: 0.16
Nodes (18): NotificationTemplateRepository, OrganizationRepository, PreAuthorize, Response, NotificationTemplate, PostMapping, RequestMapping, RequiredArgsConstructor (+10 more)

### Community 5 - "DltIntegrationTest.java"
Cohesion: 0.18
Nodes (13): DltIntegrationTest, ActiveProfiles, BeforeEach, DynamicPropertyRegistry, DynamicPropertySource, EmbeddedKafka, GenericContainer, KafkaTemplate (+5 more)

### Community 6 - "WhatsAppEventListenerTest"
Cohesion: 0.19
Nodes (13): ActiveProfiles, BeforeEach, DynamicPropertyRegistry, DynamicPropertySource, EmbeddedKafka, GenericContainer, KafkaTemplate, LeadCreatedEvent (+5 more)

### Community 7 - "WhatsAppApiException"
Cohesion: 0.13
Nodes (8): KafkaErrorHandlerCustomizer, Component, DefaultErrorHandler, Override, WhatsAppErrorHandlerCustomizer, WhatsAppApiException, WhatsAppClientException, WhatsAppServerException

### Community 8 - "ReplayableServletInputStream"
Cohesion: 0.22
Nodes (7): HttpServletRequestWrapper, ReadListener, ServletInputStream, CachedBodyHttpServletRequest, HttpServletRequest, Override, ReplayableServletInputStream

### Community 9 - "LeadWelcomeWhatsAppHandler"
Cohesion: 0.27
Nodes (7): Component, LeadCreatedEvent, NotificationRequest, Override, LeadWelcomeWhatsAppHandler, Test, LeadWelcomeWhatsAppHandlerTest

### Community 10 - "TestKafkaErrorHandlerConfig.java"
Cohesion: 0.33
Nodes (9): ConcurrentKafkaListenerContainerFactory, ConsumerFactory, Primary, Bean, DefaultErrorHandler, KafkaErrorHandlerCustomizer, KafkaTemplate, TestKafkaErrorHandlerConfig (+1 more)

### Community 11 - ".build"
Cohesion: 0.24
Nodes (6): NotificationRequest, NotificationTemplate, WhatsAppPayloadBuilder, SuppressWarnings, Test, WhatsAppPayloadBuilderTest

### Community 12 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 13 - "WhatsAppEventListener.java"
Cohesion: 0.39
Nodes (7): Component, ConsumerRecord, KafkaIdempotent, KafkaListener, LeadCreatedEvent, RequiredArgsConstructor, WhatsAppEventListener

### Community 14 - "KafkaWhatsAppEventPublisher.java"
Cohesion: 0.36
Nodes (7): Component, KafkaTemplate, Override, RequiredArgsConstructor, KafkaWhatsAppEventPublisher, WhatsAppEventPublisher, WhatsAppMessageReceivedEvent

### Community 15 - "MercadoXEmailApplication"
Cohesion: 0.43
Nodes (6): EnableAsync, EnableCaching, EntityScan, Import, SpringBootApplication, MercadoXEmailApplication

### Community 16 - "WhatsAppWebClientConfig.java"
Cohesion: 0.43
Nodes (5): Builder, Bean, Configuration, WebClient, WhatsAppWebClientConfig

### Community 17 - "TemplateVariableValidator"
Cohesion: 0.43
Nodes (3): SpecificRecord, Component, TemplateVariableValidator

### Community 18 - "MercadoXEmailApplicationTests.java"
Cohesion: 0.60
Nodes (3): SpringBootTest, Test, MercadoXEmailApplicationTests

## Knowledge Gaps
- **1 isolated node(s):** `hn.shadowcore:mercado-x-email`
  These have ≤1 connection - possible missing edges or undocumented components.
- **1 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Message` connect `WhatsAppNotificationService` to `WhatsAppPayloadReaderService`?**
  _High betweenness centrality (0.346) - this node is a cross-community bridge._
- **Why does `WhatsAppNotificationServiceTest` connect `WhatsAppNotificationService` to `NotificationTemplateService`?**
  _High betweenness centrality (0.253) - this node is a cross-community bridge._
- **Why does `WhatsAppNotificationService` connect `WhatsAppNotificationService` to `DltIntegrationTest.java`, `NotificationTemplateService`, `WhatsAppEventListener.java`, `WhatsAppEventListenerTest`?**
  _High betweenness centrality (0.218) - this node is a cross-community bridge._
- **What connects `hn.shadowcore:mercado-x-email` to the rest of the system?**
  _1 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `WhatsAppNotificationService` be split into smaller, more focused modules?**
  _Cohesion score 0.1126984126984127 - nodes in this community are weakly interconnected._
- **Should `WhatsAppPayloadReaderService` be split into smaller, more focused modules?**
  _Cohesion score 0.11491935483870967 - nodes in this community are weakly interconnected._
- **Should `MercadoXEmailOAuthConfig.java` be split into smaller, more focused modules?**
  _Cohesion score 0.13675213675213677 - nodes in this community are weakly interconnected._