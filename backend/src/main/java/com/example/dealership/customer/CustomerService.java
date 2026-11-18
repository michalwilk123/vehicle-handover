package com.example.dealership.customer;

import java.util.List;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerResponse> findAll() {
        return customerRepository.findAll(Sort.by("id")).stream()
                .map(CustomerMapper::toResponse)
                .toList();
    }

    public CustomerResponse findById(Long id) {
        return CustomerMapper.toResponse(getCustomer(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByEmailKey(Customer.emailKey(request.email()))) {
            throw emailAlreadyExists(request.email());
        }
        Customer customer = customerRepository.saveAndFlush(CustomerMapper.toEntity(request));
        return CustomerMapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getCustomer(id);
        if (customerRepository.existsByEmailKeyAndIdNot(Customer.emailKey(request.email()), id)) {
            throw emailAlreadyExists(request.email());
        }
        CustomerMapper.updateEntity(customer, request);
        // Flush here so that a concurrent duplicate fails inside this call as a constraint violation (409).
        return CustomerMapper.toResponse(customerRepository.saveAndFlush(customer));
    }

    @Transactional
    public void delete(Long id) {
        customerRepository.delete(getCustomer(id));
    }

    private Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer with id " + id + " not found"));
    }

    private static ConflictException emailAlreadyExists(String email) {
        return new ConflictException("Customer with email " + email + " already exists");
    }
}
