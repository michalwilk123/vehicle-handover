package com.example.dealership.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveStoresCustomerThatCanBeReadBack() {
        Customer saved = customerRepository.save(
                new Customer("Jan", "Kowalski", "Jan.Kowalski@Example.com", "+48 600 100 200", "1234567890"));
        entityManager.flush();
        entityManager.clear();

        Customer found = customerRepository.findById(saved.getId()).orElseThrow();

        assertThat(found).isNotSameAs(saved);
        assertThat(found.getFirstName()).isEqualTo("Jan");
        assertThat(found.getLastName()).isEqualTo("Kowalski");
        assertThat(found.getEmail()).isEqualTo("Jan.Kowalski@Example.com");
        assertThat(found.getEmailKey()).isEqualTo("jan.kowalski@example.com");
        assertThat(found.getPhone()).isEqualTo("+48 600 100 200");
        assertThat(found.getNip()).isEqualTo("1234567890");
    }

    @Test
    void existsByEmailKeyAndIdNotFindsEmailOfAnotherCustomer() {
        Customer first = customerRepository.save(customer("jan@example.com"));
        Customer second = customerRepository.save(customer("anna@example.com"));
        entityManager.flush();

        assertThat(customerRepository.existsByEmailKey("jan@example.com")).isTrue();
        assertThat(customerRepository.existsByEmailKeyAndIdNot("jan@example.com", second.getId())).isTrue();
        assertThat(customerRepository.existsByEmailKeyAndIdNot("jan@example.com", first.getId())).isFalse();
        assertThat(customerRepository.existsByEmailKey("piotr@example.com")).isFalse();
    }

    @Test
    void databaseRejectsEmailThatDiffersOnlyInCase() {
        customerRepository.saveAndFlush(customer("jan@example.com"));

        assertThatThrownBy(() -> customerRepository.saveAndFlush(customer("JAN@Example.COM")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Customer customer(String email) {
        return new Customer("Jan", "Kowalski", email, "600100200", null);
    }
}
