package com.cleardocs.integration;

import com.cleardocs.ClearDocsApplication;
import com.cleardocs.dto.LoginRequest;
import com.cleardocs.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ClearDocsApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Document Approval Workflow — Integration Tests")
class DocumentApprovalIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("cleardocs_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7-jammy");

    @SuppressWarnings("rawtypes")
    @Container
    static GenericContainer redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static String userToken;
    private static String adminToken;
    private static Long documentId;

    @Test
    @Order(1)
    @DisplayName("Register a new user successfully")
    void registerUser_validRequest_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("integrationuser");
        req.setEmail("integration@test.com");
        req.setPassword("Integration@123");
        req.setFullName("Integration User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("integrationuser"));
    }

    @Test
    @Order(2)
    @DisplayName("Admin login returns JWT token")
    void adminLogin_validCredentials_returnsToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("admin");
        req.setPassword("Admin@1234");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(body).get("accessToken").asText();
        Assertions.assertNotNull(adminToken);
    }

    @Test
    @Order(3)
    @DisplayName("User login returns JWT token")
    void userLogin_validCredentials_returnsToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("integrationuser");
        req.setPassword("Integration@123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        userToken = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("accessToken").asText();
        Assertions.assertNotNull(userToken);
    }

    @Test
    @Order(4)
    @DisplayName("Unauthenticated request to /documents returns 401")
    void getDocuments_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("Authenticated user can access document list")
    void getDocuments_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/documents")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("State-count endpoint requires ADMIN or APPROVER role")
    void getStateCounts_asAdmin_returnsAllStates() throws Exception {
        mockMvc.perform(get("/api/v1/documents/stats/state-counts")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.DRAFT").exists())
            .andExpect(jsonPath("$.APPROVED").exists());
    }
}
