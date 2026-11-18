package com.example.dealership.customer;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 254)
    private String email;

    // Lower-case copy of the email: the unique constraint compares emails without regard to case.
    @Column(nullable = false, length = 254)
    private String emailKey;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(length = 10)
    private String nip;

    protected Customer() {
    }

    public Customer(String firstName, String lastName, String email, String phone, String nip) {
        this.firstName = firstName;
        this.lastName = lastName;
        setEmail(email);
        this.phone = phone;
        this.nip = nip;
    }

    public static String emailKey(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.emailKey = emailKey(email);
    }

    public String getEmailKey() {
        return emailKey;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNip() {
        return nip;
    }

    public void setNip(String nip) {
        this.nip = nip;
    }
}
