package com.example;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleLoginInterceptionTest {
    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void setupAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

    @BeforeEach
    void setup() {
        context = browser.newContext();
        page = context.newPage();
    }

    @Test
    void simpleLoginInterceptionTest() {
        final boolean[] requestIntercepted = {false};
        final String[] interceptedData = new String[2]; 

        page.route("**/authenticate", route -> {
            requestIntercepted[0] = true;
            System.out.println("Запрос перехвачен!");

            String originalPostData = route.request().postData();
            interceptedData[0] = originalPostData;
            System.out.println("Было: " + originalPostData);

            String modifiedPostData = originalPostData.replace(
                "username=tomsmith", 
                "username=HACKED_USER"
            );
            interceptedData[1] = modifiedPostData;
            System.out.println("Стало: " + modifiedPostData);

            Route.ResumeOptions options = new Route.ResumeOptions()
                .setPostData(modifiedPostData);

            route.resume(options);
        });

        page.navigate("https://the-internet.herokuapp.com/login");

        page.fill("#username", "tomsmith");
        page.fill("#password", "SuperSecretPassword!");

        page.click("button[type='submit']");

        page.waitForResponse(response -> 
            response.url().contains("authenticate") && response.status() == 200,
            () -> {}
        );

        assertTrue(requestIntercepted[0], "Запрос должен был быть перехвачен");
        
        assertNotNull(interceptedData[0]);
        assertNotNull(interceptedData[1]);
        assertTrue(interceptedData[0].contains("username=tomsmith"));
        assertTrue(interceptedData[1].contains("username=HACKED_USER"));
        
        String errorMessage = page.locator("#flash").textContent();
        assertTrue(errorMessage.contains("invalid") || errorMessage.contains("error"), 
            "Должно быть сообщение об ошибке: " + errorMessage);
        
        System.out.println("Тест успешно завершен!");
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @AfterAll
    static void tearDownAll() {
        browser.close();
        playwright.close();
    }
}