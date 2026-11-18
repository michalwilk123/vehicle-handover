package com.example.dealership.customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmailKey(String emailKey);

    boolean existsByEmailKeyAndIdNot(String emailKey, Long id);
}
