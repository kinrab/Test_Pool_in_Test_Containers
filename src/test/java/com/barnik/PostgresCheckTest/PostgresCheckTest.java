/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.barnik.PostgresCheckTest;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostgresCheckTest 
{

    @Test
    void checkConnection () 
    {   
        // Используем легковесный образ на базе Alpine Linux
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine") 
             .withInitScript("init.sql"))
        {           
            System.out.println("=== Step 1: Initialisation and image downloading (if nedded) ===");
            postgres.start();       
            
            System.out.println("=== Step 2: Container is running successfully! ===");
            System.out.println("HOST: " + postgres.getHost());
            System.out.println("Port in Docker: " + postgres.getMappedPort(5432));
            System.out.println("JDBC URL: " + postgres.getJdbcUrl()); 
            
            assertTrue(postgres.isRunning(), "The container should be running");  
            
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                java.sql.ResultSet rs = conn.createStatement().executeQuery("SELECT count(*) FROM test_table")) 
            {
                    rs.next();
                    System.out.println("Quantity of records in databse: " + rs.getInt(1));
            }
            catch (java.sql.SQLException e) 
            {
                System.err.println("Ошибка при работе с БД: " + e.getMessage());
            }
     
            System.out.println("=== Step 3: Finalisation of test. The container will be deleted automatically");
    
        }
    }

} // End of class






























