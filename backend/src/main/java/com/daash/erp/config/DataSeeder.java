package com.daash.erp.config;

import com.daash.erp.model.CashRegister;
import com.daash.erp.model.Customer;
import com.daash.erp.model.DteDocument;
import com.daash.erp.model.KardexEntry;
import com.daash.erp.model.Product;
import com.daash.erp.model.Role;
import com.daash.erp.model.Sale;
import com.daash.erp.model.SaleItem;
import com.daash.erp.model.Supplier;
import com.daash.erp.model.User;
import com.daash.erp.repository.CashRegisterRepository;
import com.daash.erp.repository.CustomerRepository;
import com.daash.erp.repository.DteDocumentRepository;
import com.daash.erp.repository.KardexRepository;
import com.daash.erp.repository.ProductRepository;
import com.daash.erp.repository.SaleRepository;
import com.daash.erp.repository.SupplierRepository;
import com.daash.erp.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final SaleRepository saleRepository;
    private final KardexRepository kardexRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final DteDocumentRepository dteDocumentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, ProductRepository productRepository,
                      CustomerRepository customerRepository, SupplierRepository supplierRepository,
                      SaleRepository saleRepository, KardexRepository kardexRepository,
                      CashRegisterRepository cashRegisterRepository, DteDocumentRepository dteDocumentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.saleRepository = saleRepository;
        this.kardexRepository = kardexRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.dteDocumentRepository = dteDocumentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        String passwordHash = passwordEncoder.encode("Demo1234!");
        User cajero = user("Maria", "maria@adventureworks.com", Role.CAJERO, passwordHash);
        User bodega = user("Carlos", "carlos@adventureworks.com", Role.ADMIN_BODEGA, passwordHash);
        User gerente = user("Ana", "ana@adventureworks.com", Role.GERENTE, passwordHash);
        user("Roberto", "roberto@adventureworks.com", Role.AUDITOR, passwordHash);

        List<Product> products = List.of(
                product("AW-001", "Bicicleta Montaña X1", "Bicicleta rodada 29", "Deportes", "459.99", "310.00", 12, 4),
                product("AW-002", "Casco Deportivo Pro", "Casco con certificación", "Seguridad", "89.99", "52.00", 30, 8),
                product("AW-003", "Guantes de Ciclismo", "Guantes talla M", "Accesorios", "24.50", "12.00", 40, 10),
                product("AW-004", "Luces LED Recargables", "Juego de luces frontales y traseras", "Accesorios", "34.99", "18.00", 25, 6),
                product("AW-005", "Candado Seguridad U-Lock", "Candado antirrobo", "Seguridad", "39.99", "21.00", 18, 5),
                product("AW-006", "Botella Agua 750ml", "Botella térmica", "Accesorios", "12.99", "6.50", 60, 15),
                product("AW-007", "Kit Herramientas Bici", "Set de herramientas", "Herramientas", "74.95", "41.00", 14, 3),
                product("AW-008", "Silla de Montaña Ergo", "Sillín ergonómico", "Accesorios", "29.99", "15.00", 22, 5));
        products = productRepository.saveAll(products);

        Customer consumidor = customer("Consumidor Final", null, null, null, "consumidor_final");
        Customer juan = customer("Juan Carlos Pérez", "0614-123456-100-0", "12345678-9", "juan@example.com", "contribuyente");
        Customer distribuidora = customer("Distribuidora Los Pinos", "0614-987654-321-0", null, "ventas@lospinos.com", "contribuyente");
        customerRepository.saveAll(List.of(consumidor, juan, distribuidora));

        supplier("Distribuidora Adventure", "0614-111111-111-1", "Proveedor mayorista de bicicletas");
        supplier("Seguridad Deportiva S.A.", "0614-222222-222-2", "Cascos y candados");

        Instant now = Instant.now();
        CashRegister register = new CashRegister();
        register.userId = cajero.id;
        register.fechaApertura = now.minus(3, ChronoUnit.HOURS);
        register.fondoInicial = new BigDecimal("100.00");
        register.totalEfectivo = new BigDecimal("1169.48");
        register.totalTarjeta = new BigDecimal("1069.14");
        register.totalTransferencia = BigDecimal.ZERO;
        register.totalVentas = register.totalEfectivo.add(register.totalTarjeta);
        register.abierta = true;
        cashRegisterRepository.save(register);

        seedSale("FE001-0001", now.minus(2, ChronoUnit.HOURS), juan, products.get(0), 1, "efectivo", cajero.id);
        seedSale("FE001-0002", now.minus(90, ChronoUnit.MINUTES), consumidor, products.get(1), 1, "tarjeta", cajero.id);
        seedSale("FE001-0003", now.minus(45, ChronoUnit.MINUTES), distribuidora, products.get(4), 5, "efectivo", bodega.id);
        seedSale("FE001-0004", now.minus(20, ChronoUnit.MINUTES), consumidor, products.get(5), 3, "tarjeta", cajero.id);

        for (Product product : products.subList(0, 5)) {
            KardexEntry entry = new KardexEntry();
            entry.fecha = now.minus(4, ChronoUnit.HOURS);
            entry.productoId = product.id;
            entry.productoNombre = product.nombre;
            entry.tipo = "compra";
            entry.cantidad = 20;
            entry.stockAnterior = product.stock - 20;
            entry.stockPosterior = product.stock;
            entry.justificacion = "Carga inicial de inventario";
            entry.userId = bodega.id;
            entry.referencia = "SEED";
            kardexRepository.save(entry);
        }
    }

    private User user(String nombre, String email, Role role, String passwordHash) {
        User user = new User();
        user.nombre = nombre;
        user.email = email;
        user.role = role;
        user.passwordHash = passwordHash;
        return userRepository.save(user);
    }

    private Product product(String codigo, String nombre, String descripcion, String categoria,
                            String precio, String costo, int stock, int stockMinimo) {
        Product product = new Product();
        product.codigo = codigo;
        product.nombre = nombre;
        product.descripcion = descripcion;
        product.categoria = categoria;
        product.precio = new BigDecimal(precio);
        product.costo = new BigDecimal(costo);
        product.stock = stock;
        product.stockMinimo = stockMinimo;
        product.activo = true;
        return product;
    }

    private Customer customer(String nombre, String nit, String dui, String email, String tipo) {
        Customer customer = new Customer();
        customer.nombre = nombre;
        customer.nit = nit;
        customer.dui = dui;
        customer.email = email;
        customer.tipoContribuyente = tipo;
        return customer;
    }

    private void supplier(String nombre, String nit, String contacto) {
        Supplier supplier = new Supplier();
        supplier.nombre = nombre;
        supplier.nit = nit;
        supplier.contacto = contacto;
        supplierRepository.save(supplier);
    }

    private void seedSale(String numero, Instant fecha, Customer customer, Product product, int quantity,
                          String metodoPago, Long userId) {
        Sale sale = new Sale();
        sale.numeroFactura = numero;
        sale.fecha = fecha;
        sale.clienteId = customer.id;
        sale.clienteNombre = customer.nombre;
        sale.metodoPago = metodoPago;
        sale.userId = userId;
        sale.estadoDTE = "aceptado";
        sale.uuid = UUID.randomUUID().toString();
        sale.anulada = false;

        SaleItem item = new SaleItem();
        item.sale = sale;
        item.productoId = product.id;
        item.productoCodigo = product.codigo;
        item.productoNombre = product.nombre;
        item.cantidad = quantity;
        item.precioUnitario = product.precio;
        item.descuento = BigDecimal.ZERO;
        item.montoLinea = product.precio.multiply(BigDecimal.valueOf(quantity));
        sale.items.add(item);

        sale.subtotal = item.montoLinea;
        sale.iva = sale.subtotal.multiply(new BigDecimal("0.13"));
        sale.total = sale.subtotal.add(sale.iva);
        sale.dteJson = "{\"version\":\"1.4\",\"codigoGeneracion\":\"" + sale.uuid + "\"}";
        sale.dteFirma = "seed-signature";
        Sale saved = saleRepository.save(sale);

        DteDocument document = new DteDocument();
        document.saleId = saved.id;
        document.codigoGeneracion = sale.uuid.toUpperCase();
        document.fechaEmision = fecha;
        document.json = sale.dteJson;
        document.firma = sale.dteFirma;
        document.estado = "aceptado";
        document.sello = sale.uuid;
        document.createdAt = fecha;
        dteDocumentRepository.save(document);
    }
}
