package com.example.dealership.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final String EMAIL = "Jan.Kowalski@Example.com";
    private static final String OTHER_EMAIL = "anna.nowak@example.com";

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void findAllReturnsMappedCustomersSortedById() {
        when(customerRepository.findAll(Sort.by("id"))).thenReturn(List.of(customer(EMAIL), customer(OTHER_EMAIL)));

        List<CustomerResponse> responses = customerService.findAll();

        assertThat(responses).extracting(CustomerResponse::email).containsExactly(EMAIL, OTHER_EMAIL);
    }

    @Test
    void findByIdThrowsNotFoundWhenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer with id 99 not found");
    }

    @Test
    void createSavesCustomerKeepingEmailCase() {
        when(customerRepository.existsByEmailKey("jan.kowalski@example.com")).thenReturn(false);
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.create(request(EMAIL));

        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.firstName()).isEqualTo("Jan");
        assertThat(response.nip()).isNull();
    }

    @Test
    void createThrowsConflictWhenEmailExistsWithDifferentCase() {
        when(customerRepository.existsByEmailKey("jan.kowalski@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request(EMAIL)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Customer with email " + EMAIL + " already exists");
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateChangesExistingCustomer() {
        Customer customer = customer(EMAIL);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailKeyAndIdNot(OTHER_EMAIL, 1L)).thenReturn(false);
        when(customerRepository.saveAndFlush(customer)).thenReturn(customer);

        CustomerResponse response = customerService.update(1L, request(OTHER_EMAIL));

        assertThat(response.email()).isEqualTo(OTHER_EMAIL);
        assertThat(customer.getEmailKey()).isEqualTo(OTHER_EMAIL);
    }

    @Test
    void updateKeepsOwnEmailWithDifferentCase() {
        Customer customer = customer(EMAIL);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailKeyAndIdNot("jan.kowalski@example.com", 1L)).thenReturn(false);
        when(customerRepository.saveAndFlush(customer)).thenReturn(customer);

        CustomerResponse response = customerService.update(1L, request("JAN.KOWALSKI@example.com"));

        assertThat(response.email()).isEqualTo("JAN.KOWALSKI@example.com");
    }

    @Test
    void updateThrowsConflictWhenEmailBelongsToAnotherCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(EMAIL)));
        when(customerRepository.existsByEmailKeyAndIdNot(OTHER_EMAIL, 1L)).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(1L, request(OTHER_EMAIL)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Customer with email " + OTHER_EMAIL + " already exists");
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateThrowsNotFoundWhenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(99L, request(EMAIL)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer with id 99 not found");
    }

    @Test
    void deleteRemovesExistingCustomer() {
        Customer customer = customer(EMAIL);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.delete(1L);

        verify(customerRepository).delete(customer);
    }

    @Test
    void deleteThrowsNotFoundWhenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer with id 99 not found");
    }

    private static Customer customer(String email) {
        return new Customer("Jan", "Kowalski", email, "+48 600 100 200", null);
    }

    private static CustomerRequest request(String email) {
        return new CustomerRequest("Jan", "Kowalski", email, "+48 600 100 200", null);
    }
}
