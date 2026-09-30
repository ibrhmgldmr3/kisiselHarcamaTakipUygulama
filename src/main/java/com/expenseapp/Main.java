package com.expenseapp;

import javafx.application.Application;

/**
 * Giriş noktası. Application'dan türemeyen ayrı bir main sınıfı, uygulamanın IDE'den
 * classpath üzerinde çalıştırılırken "JavaFX runtime components are missing" hatası vermesini önler.
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(ExpenseTrackerApp.class, args);
    }
}
