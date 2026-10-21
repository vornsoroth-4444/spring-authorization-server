package org.soroth.springauthorizationserver.controller;

import org.soroth.springauthorizationserver.entity.Customer;
import org.soroth.springauthorizationserver.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody Customer customer,
                                            @AuthenticationPrincipal Jwt jwt) {
        String createdBy = jwt != null ? jwt.getSubject() : "anonymous";
        Customer savedCustomer = customerService.createCustomer(customer, createdBy);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Customer created successfully!");
        response.put("authenticated_identity", createdBy);
        response.put("token_scopes", jwt != null ? jwt.getClaimAsStringList("scope") : List.of());
        response.put("customer", savedCustomer);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Customer not found with id: " + id)));
    }
}
