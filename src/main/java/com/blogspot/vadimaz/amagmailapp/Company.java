package com.blogspot.vadimaz.amagmailapp;

public class Company {
    private String name;
    private String email;
    private String subject;
    private String[] triggers;

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

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setTriggers(String[] triggers) {
        this.triggers = triggers;
    }
}
