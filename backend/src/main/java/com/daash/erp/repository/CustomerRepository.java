package com.daash.erp.repository;

import com.daash.erp.model.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByDui(String dui);

    Optional<Customer> findByNit(String nit);
}
