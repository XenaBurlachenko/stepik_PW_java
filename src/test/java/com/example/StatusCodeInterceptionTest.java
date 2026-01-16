package com.example;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class StatusCodeInterceptionTest {
    Playwright playwright;
    Browser browser;
    BrowserContext context;
    Page page;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();
        
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
        launchOptions.setHeadless(true);
        
        // Критические настройки для CI
        launchOptions.setArgs(java.util.Arrays.asList(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu"
        ));
        
        browser = playwright.chromium().launch(launchOptions);
        context = browser.newContext();
        page = context.newPage();
        
        // Увеличиваем таймауты для CI
        page.setDefaultTimeout(60000);
        page.setDefaultNavigationTimeout(60000);

        // Перехват запроса
        context.route("**/status_codes/404", route -> {
            route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setBody("Mocked Success Response")
            );
        });
    }

    @Test
    void testMockedStatusCode() {
        // Открываем страницу
        page.navigate("https://the-internet.herokuapp.com/status_codes");
        
        // Ждем загрузки
        page.waitForLoadState(Page.LoadState.NETWORKIDLE);
        
        // Кликаем по ссылке 404
        page.locator("a[href='status_codes/404']").click();
        
        // Ждем ответа
        page.waitForResponse("**/status_codes/404");
        
        // Проверяем текст на странице
        String content = page.content();
        assertTrue(content.contains("Mocked Success Response"), 
            "Должен отображаться мок-текст. Фактический контент: " + content);
    }

    @AfterEach
    void tearDown() {
        if (page != null) page.close();
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}