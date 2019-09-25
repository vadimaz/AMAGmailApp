package com.blogspot.vadimaz.amagmailapp;

public class Company {
    private String name;
    private String email;
    private String subject;
    private String[] triggers;

    public Company(String name, String email, String subject, String... triggers) {
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.triggers = triggers;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getSubject() {
        return subject;
    }

    public String[] getTriggers() {
        return triggers;
    }
}
