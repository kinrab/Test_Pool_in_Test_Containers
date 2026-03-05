/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.barnik.PostgresCheckTest;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

public class PostgresCheckTest 
{
    @Test
    void testSystemIntegration() 
    {
        // 1. Создаем общую виртуальную сеть для контейнеров
        try (Network network = Network.newNetwork();

        // 2. Описываем контейнер базы данных
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withNetwork(network)
            .withNetworkAliases("db-host") // Имя хоста для Tomcat
            .withInitScript("init.sql")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

        // 3. Описываем контейнер Tomcat 9
        GenericContainer<?> tomcat = new GenericContainer<>("tomcat:9.0.115-jdk11-corretto")
            .withNetwork(network)
            .withExposedPorts(8080)
            // Копируем WAR файл (из ресурсов в вебаппы)
            .withCopyFileToContainer(MountableFile.forClasspathResource("MyServletProject.war"), "/usr/local/tomcat/webapps/MyServletProject.war")
            // Копируем настройки пула
            .withCopyFileToContainer(MountableFile.forClasspathResource("context.xml"), "/usr/local/tomcat/conf/context.xml")
            .waitingFor(Wait.forLogMessage(".*org.apache.catalina.startup.Catalina.start Server startup in.*\\n", 1))
            .withStartupTimeout(java.time.Duration.ofSeconds(120)) // Ждем до 2 минут
             // ЭТО ПОЗВОЛИТ ВИДЕТЬ ЛОГИ TOMCAT В КОНСОЛИ:
            .withLogConsumer(new org.testcontainers.containers.output.Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger("TOMCAT"))))
            {

                System.out.println("=== Step1 1: Run database... ===");
                postgres.start();

                System.out.println("=== Step 2: Run Tomcat 9... ===");
                tomcat.start();

                // Получаем динамический URL (Host обычно localhost, порт случайный)
                String tomcatUrl = "http://" + tomcat.getHost() + ":" + tomcat.getMappedPort(8080);

                System.out.println("=== System is ready for test. ===");
                System.out.println("URL of servlet: " + tomcatUrl);

                // Простая проверка, что Tomcat "живой"
                assertTrue(tomcat.isRunning(), "Tomcat must be run");

                // Здесь запустим тесты:
                
                // Завершение работы:    
                System.out.println("=== Test is finished. Deletion of containers. ===");
            }
            
    } // End of test method   

} // End of class




















