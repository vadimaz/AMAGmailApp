package com.blogspot.vadimaz.amagmailapp;


public interface OrderListener {
    void onOrderReady(Order order);
    void onOrderFailed(Order order);
    void onOrderSkipped(Order order);


}
