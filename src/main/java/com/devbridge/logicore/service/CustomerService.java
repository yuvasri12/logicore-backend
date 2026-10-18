package com.devbridge.logicore.service;

import com.devbridge.logicore.dto.CustomerRequest;
import com.devbridge.logicore.dto.CustomerResponse;
import com.devbridge.logicore.enums.AuditAction;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.exception.BusinessException;
import com.devbridge.logicore.model.Customer;
import com.devbridge.logicore.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private static final String ENTITY = "CUSTOMER";

    private final CustomerRepository customers;
    private final AuditService audit;

    public CustomerService(CustomerRepository customers, AuditService audit) {
        this.customers = customers;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list() {
        List<CustomerResponse> result = new ArrayList<CustomerResponse>();
        for (Customer customer : customers.findAllByOrderByNameAsc()) {
            result.add(toResponse(customer));
        }
        return result;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        String name = request.getName().trim();
        if (customers.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.CUSTOMER_NAME_EXISTS);
        }
        Customer customer = new Customer();
        apply(customer, request, name);
        Customer saved = customers.save(customer);
        audit.record(AuditAction.CUSTOMER_CREATED, ENTITY, saved.getId(), "name=" + saved.getName());
        log.info("Customer created: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = find(id);
        String name = request.getName().trim();
        if (customers.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.CUSTOMER_NAME_EXISTS);
        }
        String oldName = customer.getName();
        apply(customer, request, name);
        Customer saved = customers.save(customer);
        String detail = oldName.equals(saved.getName())
                ? "name=" + saved.getName()
                : "name=" + oldName + " -> " + saved.getName();
        audit.record(AuditAction.CUSTOMER_UPDATED, ENTITY, saved.getId(), detail);
        log.info("Customer updated: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public CustomerResponse setActive(Long id, boolean active) {
        Customer customer = find(id);
        customer.setActive(active);
        Customer saved = customers.save(customer);
        audit.record(saved.isActive() ? AuditAction.CUSTOMER_ACTIVATED : AuditAction.CUSTOMER_DEACTIVATED,
                ENTITY, saved.getId(), "name=" + saved.getName());
        log.info("Customer active changed: id={}, active={}", saved.getId(), saved.isActive());
        return toResponse(saved);
    }

    private Customer find(Long id) {
        return customers.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private void apply(Customer customer, CustomerRequest request, String name) {
        customer.setName(name);
        customer.setContactPerson(clean(request.getContactPerson()));
        customer.setEmail(clean(request.getEmail()));
        customer.setPhone(clean(request.getPhone()));
        customer.setAddress(clean(request.getAddress()));
    }

    /** Trims text; an empty value is stored as null. */
    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getContactPerson(),
                customer.getEmail(), customer.getPhone(), customer.getAddress(),
                customer.isActive(), customer.getCreatedAt());
    }
}