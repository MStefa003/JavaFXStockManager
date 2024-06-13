module com.example {
    requires java.desktop;
    requires javafx.controls;
    requires java.sql;
    requires jbcrypt;
    requires javafx.graphics;

    exports com.example.controller;
    exports com.example.model;
    exports com.example.util;
}
