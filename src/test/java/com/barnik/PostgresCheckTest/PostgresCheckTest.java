/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.barnik.PostgresCheckTest;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import java.net.http.*;
import java.net.URI;
import java.util.concurrent.*;
import java.util.List;
import java.util.stream.*;
import java.io.File;

// Импорты для метода UpdateContextXml:
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import java.time.Duration;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;



public class PostgresCheckTest 
{
    // Класс для хранения параметров нашего теста: 
    static class TestConfig 
    {
        // Входные параметры для тестов:
        String name;                                //  Имя или цель или описание смысла выполнения теста с этими параметрами.
        int maxActive;                              //  Значение MaxActive для текущего элемента скиска параметров. 
        long maxWait;                               //  Значение MaxWait для текущего элемента скиска параметров. Сколько ждать максимально завершения запроса HTTP в сервлет.
        long clientSleep;                           //  Значение Sleep для указания времени которое клиентский поток должен ждать освобождения коннекшена в пуле 
        int threads;                                //  Общее число фич/потоков запускаемых клиентом в текущем выполняемом тесте.
        
        // Ожидаемые результаты для ассертов:
        long expectedOk;                            //  Сколько запросов фич (потоков) дожлно быть завершено успешно в данном тесте. 
        long expectedFast;                          //  Сколько запросов фич (потоков) дожлно быть завершено успешно быстро без ожидания.  
        long expectedDelayed;                       //  Сколько запросов фич (потоков) дожлно быть завершено успешно после ожидания.
        long expectedError;                         //  Сколько запросов фич (потоков) должно быть завершено по таймауту с ошибкой после ожидания истечения таймаута.
        

        // Конструктор класса хранения параметров нагшего теста: 
        public TestConfig(String name, int maxActive, int maxWait, int clientSleep, int threads, long ok, long fast, long delayed,long err) 
        {
            this.name = name;
            this.maxActive = maxActive;
            this.maxWait = maxWait;
            this.clientSleep = clientSleep;
            this.threads = threads;
            this.expectedOk = ok;
            this.expectedFast = fast;
            this.expectedDelayed = delayed;
            this.expectedError = err; // Записываем ожидаемые ошибки
        }
        
        // Переопределим метод для удобства: JUnit 5 при отображении в дереве тестов (и Allure в заголовке) часто использует toString() объекта, если не указано иное.
        @Override
        public String toString() 
        {
            return name;
        }

    } // End of class TestConfig
    
    // Реальные значения параметров для наших тестов - если нужно добавить новых тестов просто здесь заполняем ноые строки:
    private static final List<TestConfig> SCENARIOS = 
            List.of(
                    //                Имя,                                                                                        MaxActive,       MaxWait,     Sleep,  Threads,    ExpOk,    ExpFast,   ExpDelayed  ExpError
                    new TestConfig("Normal Wait   All: 4  MaxAct:2  OK:4 Fast:2 Delay:2 Error:0 MaxWait:  10000 Sleep:5000",           2,            10000,      5000,     4,         4L,       2L,          2L,       0L   ) 
                    //new TestConfig("Timeout Fail  All: 4  MaxAct:2  Ok:2 Fast:2 Delay:0 Error:2 MaxWait:   3000 Sleep:5000",           2,             3000,      5000,     4,         2L,       2L,          0L,       2L   ),
                    //new TestConfig("Stairway      All: 4  MaxAct:2  OK:4 Fast:1 Delay:3 Error:0 MaxWait:  30000 Sleep:5000",           1,            30000,      5000,     4,         4L,       1L,          3L,       0L   )                        
                    //new TestConfig("Fast timeout  All: 4  MaxAct:2  OK:2 Fast:2 Delay:0 Error:2 MaxWait:    100 Sleep:5000",           2,              100,      5000,     4,         2L,       2L,          0L,       2L   ),                        
                    //new TestConfig("Wide gateway  All: 4  MaxAct:10 OK:4 Fast:4 Delay:0 Error:0 MaxWait: 100000 Sleep:5000",          10,           100000,      5000,     4,         4L,       4L,          0L,       0L   ),
                    //new TestConfig("Stairway 10   All:10  MaxAct:2  OK:6 Fast:2 Delay:4 Error:4 MaxWait:  14000 Sleep:5000",           2,            14000,      5000,     10,        6L,       2L,          4L,       4L   ), 
                    //new TestConfig("Border   10   All:10  MaxAct:2  OK:6 Fast:2 Delay:4 Error:4 MaxWait:  15000 Sleep:5000",           2,            15000,      5000,     10,        6L,       2L,          4L,       4L   ) 
                    // Тут в Border граничные условия - таймаут 15 и успеют ли освободившиеся конекшены подхватиться или таймаут истечет? Вроде истекает таймаут.
                   ); 
    
    // Класс для хранения результатов теста:
    static class TestResult 
    {
        int code;
        long startOffset;
        long duration;
        String body; 
        private int Code;
        
        TestResult(int code, long startOffset, long duration, String body) 
        {
            this.code = code;
            this.startOffset = startOffset;
            this.duration = duration;
            this.body = body;
        }
    }
    
    private void RunTests(TestConfig config, String TomCatUrl) throws Exception 
    {
        int N = config.threads; // Сколько нужно запустить потоков?
              
        // Создаем клиента HTTP:
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
             
        // 1. Получаем хост (обычно localhost)
        String host = TomCatUrl;

        // 3. Собираем финальную строку (контекст и имя сервлета берем из твоего web.xml)
        //String dynamicUrl = String.format("http://%s:%d/MyServletProject/MyServlet", host, port);
        String dynamicUrl = host + "/MyServletProject/MyServlet";

        System.out.println(">>> [DYNAMIC URL] " + dynamicUrl);
             
        // Начинаем тест:
        Instant testStart = Instant.now(); 
        System.out.println("\n>>> Test run (Time start: 0s)");

        // ШАГ 4. ЗАПУСКАЕМ N потоков с HTTP-запросом GET:
        List<TestResult> results = Allure.step("4. Executing of  " + N + " parallel requests to servlet", () -> 
        {
            List<CompletableFuture<TestResult>> futures = IntStream.rangeClosed(1, N) // Т - число запускаемых клиентских запросов (фич / потоков)
                .mapToObj(id -> CompletableFuture.supplyAsync
                            (   () -> 
                                    {
                                        Instant requestStart = Instant.now();
                                        try {
                                            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(dynamicUrl)).build();
                                            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                                            long duration = Duration.between(requestStart, Instant.now()).toSeconds();
                                            long startOffset = Duration.between(testStart, requestStart).toSeconds();

                                            
                                              // --- ВОТ ЭТОТ БЛОК ВЫВЕДЕТ ВСЁ В КОНСОЛЬ ---
                                                synchronized (System.out) 
                                                {
                                                    System.out.println("\n[HTTP RESPONSE DEBUG] Request ID: " + id);
                                                    System.out.println("Status Code: " + response.statusCode());
                                                    System.out.println("Body: " + response.body());
                                                    System.out.println("----------------------");
                                                }
                                                
                                            return new TestResult(response.statusCode(), startOffset, duration, response.body());
                                        } catch (Exception e) 
                                        {
                                            return new TestResult(500, -1, -1,"");
                                        }
                                    }
                            )
                         )
                .collect(Collectors.toList());

            // Ждем здесь же, внутри шага, чтобы Allure замерил полное время выполнения всех потоков
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

            // Возвращаем результат из шага наружу в переменную results
            return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        });  
        
        // Теперь нужно обрабработать результаты теста:
        
        // ШАГ 6: Формируем детальный отчет:
        Allure.step("6. Detail repost preparation...", () -> 
        {
            int Error = 0;
            System.out.println("\n======= Detail report =======");
            for (int i = 0; i < results.size(); i++) 
            {
                TestResult r = results.get(i);
                // Проверим были ли sql ошибки в response (то есть отвалился ли поток по таймауту?):
                if (r.body.contains("SQL query execution error")== true)
                {
                    Error = 1;                    
                }
                else
                    Error = 0;
                
                String logLine = String.format("Request #%d | Start on %d sec | Duration %d sec | StatusHttp: %d | TimeoutError: %d | MaxWait: %d | Sleep: %d ", 
                                               (i + 1), r.startOffset, r.duration, r.code, Error, config.maxWait, config.clientSleep);
                System.out.println(logLine);
                // Добавляем строчку лога прямо в Allure как вложение или текст
                Allure.addAttachment("Request " + (i+1), logLine);
            }
        });
        
        // ШАГ 7: Логирование в отчет Allure и универсальные проверки Asserts
        Allure.step("7. Results checking (Assertions)", () -> 
        {
            // 0. ОБЪЯВЛЯЕМ переменные в начале блока, чтобы их видели все вложенные шаги
            final long sleep = config.clientSleep / 1000; 
            final int maxActive = config.maxActive;

            // 1. Считаем РЕАЛЬНЫЕ успехи (код 200 И в теле нет фразы об ошибке)
            long realOkCount = results.stream()
                .filter(r -> r.code == 200 && !r.body.contains("SQL query execution error"))
                .count();

            // 2. Считаем РЕАЛЬНЫЕ ошибки (либо код 500, либо 200 с текстом ошибки)
            long realErrorCount = results.stream()
                .filter(r -> r.code == 500 || (r.code == 200 && r.body.contains("SQL query execution error")))
                .count();

            // 3. Проверка количества
            Allure.step("7.1 Проверка: Успешных ответов с данными (ожидаем " + config.expectedOk + ")", () -> 
                assertEquals(config.expectedOk, realOkCount, "Кол-во чистых 200 не совпало!")
            );

            Allure.step("7.2 Проверка: Отвалов пула (ожидаем " + config.expectedError + ")", () -> 
                assertEquals(config.expectedError, realErrorCount, "Ожидаемые ошибки не найдены в ответах!")
            );

            // 4. Лесенка времени (только для тех, где реально были данные)
         
            List<TestResult> successResults = results.stream()
                .filter(r -> r.body.contains("InternetTelecom") || r.body.contains("Terayon"))
                .sorted(Comparator.comparingLong(r -> r.duration))
                .collect(Collectors.toList());

            // 2. Фильтруем ТАЙМАУТЫ (те, кто получил текст ошибки SQL)
            List<TestResult> timeoutResults = results.stream()
                .filter(r -> r.body.contains("SQL query execution error"))
                .collect(Collectors.toList());

            // Печать для отладки
            System.out.println("DEBUG: Success: " + successResults.size() + ", Taimouts: " + timeoutResults.size());

            // 3. ПРОВЕРКА УСПЕШНЫХ (Волны ожидания по 5с)
            for (int i = 0; i < successResults.size(); i++) {
                final int idx = i;
                int wave = i / maxActive; 
                long expectedDuration = sleep * (wave + 1);

                Allure.step("7.3 Успешный запрос в волне #" + (wave + 1), () -> {
                    long actual = successResults.get(idx).duration;
                    assertTrue(Math.abs(actual - expectedDuration) <= 1, 
                        "Запрос длился " + actual + "с вместо " + expectedDuration + "с");
                });
            }

            // 4. ПРОВЕРКА ТАЙМАУТОВ (Отсечка по 3с)
            for (int i = 0; i < timeoutResults.size(); i++) {
                final int idx = i;
                long expectedTimeoutSec = config.maxWait / 1000; 

                Allure.step("7.4 Проверка отсечки по таймауту #" + (idx + 1), () -> {
                    long actual = timeoutResults.get(idx).duration;
                    assertTrue(actual >= expectedTimeoutSec && actual <= (expectedTimeoutSec + 2), 
                        "Запрос отвалился за " + actual + "с. Ожидали ~" + expectedTimeoutSec + "с");
                });
            }

            // 5. ИТОГОВЫЙ БАЛАНС
            Allure.step("7.5 Итоговая проверка баланса пула", () -> {
                assertEquals(maxActive, successResults.size(), 
                    "Должно быть ровно " + maxActive + " успеха(ов) по размеру пула");
                assertEquals(N - maxActive, timeoutResults.size(), 
                    "Остальные " + (N - maxActive) + " запросов должны были упасть");
            });
            
            
            ////////////////////////////////////////////////////////////
            
        }); // Конец главного Allure.step
        
        System.out.println("============ All steps have been completed. Wow! :-) ==================\n");
        
    } // End of method RunTest
    
    // Метод который обновляет параметры MAxActive и MaxWaitMillis в context.xml перед стартом TomCat: 
    private void updateContextXml(TestConfig config) throws Exception 
    {
        int N = config.threads; // Вместо жесткого кодирования будем забирать из набора параметров теста.
        
        // 1. Собираем путь к файлу:
        String contextPath = "src/test/resources/context.xml";

        // 2. Создаем указатель на файл:
        File xmlFile = new File(contextPath);            
        System.out.println(">>> [CONFIG] Read file: " + xmlFile.getCanonicalPath());   //полный абсолютный путь  xmlFile.getCanonicalPath()

        // 3. Инициализируем XML-парсер
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        // 4. Читаем содержимое файла: 
        //    Вот здесь DBuilder (наш парсер) берет тот адрес, который мы ему дали в xmlFile, идет на диск, открывает файл, вычитывает все байты и закрывает его,
        //    оставив у себя в памяти «дерево» (объект doc).
        Document doc = builder.parse(xmlFile);

        // 3. Ищем все теги <Resource> и меняем значения на наши из переметров теста:
        NodeList resources = doc.getElementsByTagName("Resource");
        boolean isUpdated = false;

        for (int i = 0; i < resources.getLength(); i++) 
        {
            Element resource = (Element) resources.item(i);

            // Ищем наш ресурс по ключевому признаку (например, драйверу или имени)
            if (resource.getAttribute("driverClassName").contains("postgresql") || resource.getAttribute("name").contains("jdbc/postgres")) 
            {

                // Записываем новые значения из нашего текущего сценария
                resource.setAttribute("maxTotal", String.valueOf(config.maxActive));
                resource.setAttribute("maxWaitMillis", String.valueOf(config.maxWait));
                isUpdated = true;
            }
        }

        // Проверяем были ли изменены параметры или мы их не нашли? 
        if (!isUpdated) 
        {
            throw new RuntimeException("Error: In the context.xml tag Resource is not found (for PostgreSQL)!");
        }

        // 4. Сохраняем изменения обратно в файл
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        
        // 5. Добавим форматирование, чтобы XML не превратился в одну строку
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(xmlFile);
        transformer.transform(source, result);

        // 6. Нужно быть уверенным, что файл на диске точно получил наши параметры: 
        System.out.flush();
        
        // 7. Даем ОС микро-паузу (500 мс), чтобы закрыть все дескрипторы файла
        Thread.sleep(500); 
                
        System.out.println(">>> [CONFIG] Parameters has been updated successfully: maxTotal = " + config.maxActive + ", maxWaitMillis = " + config.maxWait);
        
    } // End of UpdateContextXml
    
    
    
    
     // **************************************************************************************************
    // Самый важный метод - наш параметризованный тест. 
    //***************************************************************************************************
    static List<TestConfig> getScenarios() 
    {
        return SCENARIOS;
    }
    
    @Feature("Postgres Connection Pool")
    @Story("MaxActive limit check")
    @Description("Проверка очереди Tomcat: запроса сразу, и в ожидании.")
    
    // Параметризованный запуск
    @ParameterizedTest(name = "{0}")
    @MethodSource("getScenarios")
 
    void testSystemIntegration(TestConfig config) throws Exception  
    {
        // 0. Перед созданием контейнера Tomcat подготавливаем файл
        updateContextXml(config);
       
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
            // Нужно положить драйвер:    
            .withCopyFileToContainer(MountableFile.forHostPath("src/test/resources/postgresql-42.7.10.jar"), "/usr/local/tomcat/lib/postgresql.jar")
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
                String tomcatUrl = "http://" + tomcat.getHost() + ":" + tomcat.getMappedPort(8080);;

                System.out.println("=== System is ready for test. ===");
                System.out.println("URL of servlet: " + tomcatUrl);

                // Простая проверка, что Tomcat "живой"
                assertTrue(tomcat.isRunning(), "Tomcat must be run");

                // Здесь запустим тесты:
                RunTests(config, tomcatUrl);
                
                // Завершение работы:    
                System.out.println("=== Test is finished. Deletion of containers. ===");
            }
            
    } // End of test method   

} // End of class




















