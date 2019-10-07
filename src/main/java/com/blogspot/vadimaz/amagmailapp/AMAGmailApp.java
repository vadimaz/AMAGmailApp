package com.blogspot.vadimaz.amagmailapp;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;

public class AMAGmailApp {

    private static final String[] ZIPS =
            ("89012 89014 89052 89053 89074 89077 89101 89102 89103 89104 89105 89106 " +
                    "89107 89108 89109 89110 89111 89112 89113 89114 89115 89116 89117 " +
                    "89118 89119 89120 89121 89122 89123 89124 89125 89126 89127 89128 " +
                    "89129 89130 89131 89132 89133 89134 89135 89136 89137 89138 89139 " +
                    "89140 89141 89142 89143 89144 89145 89146 89147 89148 89149 89150 " +
                    "89151 89152 89153 89154 89155 89156 89157 89158 89159 89160 89161 " +
                    "89162 89164 89165 89166 89169 89170 89173 89177 89178 89179 89180 " +
                    "89183 89185 89193 89195 89199").split(" ");

    private static int count;

    public static void main(String[] args) throws IOException, GeneralSecurityException, InterruptedException {

        Company company1 = new Company("Fidelity National Home Warranty", "dispatch@fnf.com",
                "FNHW Rush Dispatch Offer", "fnhw.com/swobid/BidForm.aspx", "a=1");
        Company company2 = new Company("Old Republic Home Protection", "donotreply@orhp.com",
                "Can you accept this job?", "orhp.com/index.cfm?go=contractors.acceptWOOffer");
        Company company3 = new Company("First American Home Warranty", "DoNotReply@home-warranty-info.com",
                "Emergency Service Confirmation", "s.fahw.com/woo");
        Company[] companies = {company1, company2, company3};

        GmailService service = new GmailService();
        service.setZips(ZIPS);
        while (true) {
            System.out.println("----- " + new Date() + " ------\n");
            for (Company company : companies) {
                service.setCompany(company);
                System.out.println(company.getName());
                List<URL> urls = service.getNewURLs();
                if (urls == null) {
                    System.out.println("There are no new messages.\n");
                    continue;
                }
                System.out.println("There are " + urls.size() + " new messages.\n");
                for (URL url : urls) {
                    System.out.println(url);
                    count++;
                    new Thread(new URLConnectRunnable(url, service.getService(), company)).start();
                }
                System.out.println("\n");
            }
            System.out.println("----- " + count + " messages proceed -----\n");
            Thread.sleep(5000);
        }

    }
}
