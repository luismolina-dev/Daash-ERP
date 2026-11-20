package com.daash.erp;

import com.daash.erp.model.CashRegister;
import com.daash.erp.model.Product;
import com.daash.erp.model.Role;
import com.daash.erp.model.User;
import com.daash.erp.repository.AuditLogRepository;
import com.daash.erp.repository.CashRegisterRepository;
import com.daash.erp.repository.CustomerRepository;
import com.daash.erp.repository.DteDocumentRepository;
import com.daash.erp.repository.KardexRepository;
import com.daash.erp.repository.ProductRepository;
import com.daash.erp.repository.PurchaseRepository;
import com.daash.erp.repository.SaleRepository;
import com.daash.erp.repository.SupplierRepository;
import com.daash.erp.repository.ContingencyQueueRepository;
import com.daash.erp.repository.UserRepository;
import com.daash.erp.security.JwtService;
import com.daash.erp.security.LoginRateLimiter;
import com.daash.erp.service.MockHaciendaClient;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class AbstractIntegrationTest {
    @Autowired protected WebApplicationContext webApplicationContext;
    protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected ProductRepository productRepository;
    @Autowired protected CustomerRepository customerRepository;
    @Autowired protected SupplierRepository supplierRepository;
    @Autowired protected SaleRepository saleRepository;
    @Autowired protected KardexRepository kardexRepository;
    @Autowired protected CashRegisterRepository cashRegisterRepository;
    @Autowired protected DteDocumentRepository dteDocumentRepository;
    @Autowired protected ContingencyQueueRepository contingencyQueueRepository;
    @Autowired protected AuditLogRepository auditLogRepository;
    @Autowired protected PurchaseRepository purchaseRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;
    @Autowired protected LoginRateLimiter loginRateLimiter;
    @Autowired protected MockHaciendaClient haciendaClient;

    @BeforeEach
    void resetState() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        loginRateLimiter.reset();
        haciendaClient.setDown(false);
        contingencyQueueRepository.deleteAll();
        dteDocumentRepository.deleteAll();
        saleRepository.deleteAll();
        kardexRepository.deleteAll();
        cashRegisterRepository.deleteAll();
        purchaseRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
        supplierRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User createUser(String email, Role role) {
        User user = new User();
        user.nombre = email.substring(0, email.indexOf('@'));
        user.email = email;
        user.role = role;
        user.passwordHash = passwordEncoder.encode("Demo1234!");
        return userRepository.save(user);
    }

    protected String token(User user) {
        return jwtService.generateAccessToken(user);
    }

    protected Product createProduct(String codigo, String nombre, String precio, int stock) {
        Product product = new Product();
        product.codigo = codigo;
        product.nombre = nombre;
        product.categoria = "Pruebas";
        product.precio = new BigDecimal(precio);
        product.costo = new BigDecimal("10.00");
        product.stock = stock;
        product.stockMinimo = 2;
        product.activo = true;
        return productRepository.save(product);
    }

    protected CashRegister openRegister(User user) {
        CashRegister register = new CashRegister();
        register.userId = user.id;
        register.fechaApertura = Instant.now();
        register.fondoInicial = new BigDecimal("50.00");
        register.abierta = true;
        return cashRegisterRepository.save(register);
    }
}
