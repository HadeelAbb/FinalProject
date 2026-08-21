package com.hsts.shared.model;

import java.io.Serializable;

/**
 * Client-side copy of Partner 1's User entity (PDOM).
 * Password is intentionally NOT included here - the server should never
 * send it back to the client after login.
 */
public class User implements Serializable {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    public User() {
    }

    public User(String id, String firstName, String lastName, String email, String role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        if (firstName == null || firstName.isBlank()) {
            return lastName != null ? lastName.trim() : "";
        }
        if (lastName == null || lastName.isBlank()) {
            return firstName.trim();
        }
        return firstName.trim() + " " + lastName.trim();
    }
}
