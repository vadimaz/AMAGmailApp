package com.blogspot.vadimaz.amagmailapp.config;

import com.blogspot.vadimaz.amagmailapp.Company;

public class Configuration {
    private Company[] companies;
    private String[] zips;

    public Company[] getCompanies() {
        return companies;
    }

    public void setCompanies(Company[] companies) {
        this.companies = companies;
    }

    public String[] getZips() {
        return zips;
    }

    public void setZips(String[] zips) {
        this.zips = zips;
    }

}
