package org.soroth.springauthorizationserver.service;

import org.soroth.springauthorizationserver.entity.Customer;
import org.soroth.springauthorizationserver.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer(Customer customer, String createdBy) {
        customer.setCreatedBy(createdBy);
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }
}
