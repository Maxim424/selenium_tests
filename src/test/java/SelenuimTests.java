import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.security.SecureRandom;
import java.time.Duration;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.fail;

public class SelenuimTests {
    private static final String PATH_TO_CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
    private static final String PATH_TO_CHROMEDRIVER = "/Users/maximkuznetsov/Desktop/hw8/src/driver/chromedriver";
    private static final String WEBSITE_URL = "http://localhost:8091";
    private WebDriver driver;
    private JavascriptExecutor js;
    private StringBuffer verificationErrors = new StringBuffer();
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String ALL_CHARACTERS = UPPER + LOWER + DIGITS;
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateRandomString(int length) {
        if (length < 1) throw new IllegalArgumentException("Length must be greater than 0");

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALL_CHARACTERS.length());
            sb.append(ALL_CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    private boolean isAlertPresent() {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    private void waitOnlineStatus() {
        WebDriverWait dwd = new WebDriverWait(driver,Duration.ofSeconds(30) );
        String statusOnline = "//div[@id='status' and contains(., 'Online')]";
        dwd.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(statusOnline)));
    }

    private void waitMoveOver() {
        WebDriverWait dwd = new WebDriverWait(driver,Duration.ofSeconds(30) );
        dwd.until(ExpectedConditions.invisibilityOfElementLocated(By.className("moveAnimationContent")));
    }

    private void waitTimerZero(){
        WebDriverWait dwd = new WebDriverWait(driver,Duration.ofSeconds(30) );
        String statusOnline = "//div[contains(@class, 'timer') and contains(., '0')]";
        dwd.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(statusOnline)));
    }

    @BeforeEach
    public void setUp() {
        System.setProperty("webdriver.chrome.driver", PATH_TO_CHROMEDRIVER);
        ChromeOptions options = new ChromeOptions();
        options.setBinary(PATH_TO_CHROME);
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(60));
        js = (JavascriptExecutor) driver;
        driver.get(WEBSITE_URL);
    }

    @AfterEach
    public void logOut() {
        driver.findElement(By.id("logout-btn")).click();
        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!verificationErrorString.isEmpty()) {
            fail(verificationErrorString);
        }
    }

    public void fillSession() {
        driver.findElement(By.id("sessionId")).click();
        driver.findElement(By.id("sessionId")).clear();
        driver.findElement(By.id("sessionId")).sendKeys(generateRandomString(18));
        driver.findElement(By.id("login-btn")).click();
        waitOnlineStatus();
    }

    @Test
    public void testLongSessionIdentifier() throws InterruptedException {
        driver.findElement(By.id("sessionId")).click();
        driver.findElement(By.id("sessionId")).clear();
        driver.findElement(By.id("sessionId")).sendKeys(generateRandomString(20));
        driver.findElement(By.id("login-btn")).click();
        sleep(1000);
        assert(!isAlertPresent()); // не смогли войти в систему, хотя sessionId из 20 символов удовлетворяет условию.
    }

    @Test
    public void testLeftBorder() {
        fillSession();
        WebElement wl = driver.findElement(By.id("arrowLeft"));
        for (int i = 0; i < 30; i++) {
            wl.click(); // 30 раз кликаем "влево".
        }
        waitTimerZero();
        waitMoveOver();
        waitOnlineStatus();
        var loc = driver.findElement(By.id("place")).getText();
        long position = Long.parseLong(loc.substring(loc.indexOf('[')+1, loc.indexOf(']')));
        assert(position > -20 && position < 20); // вышли за границы карты.
    }

    @Test
    public void testMultipleBoats() throws InterruptedException {
        // создаем вторую сессию.
        ChromeOptions options = new ChromeOptions();
        options.setBinary(PATH_TO_CHROME);
        var driver2 = new ChromeDriver(options);
        driver2.manage().timeouts().implicitlyWait(Duration.ofSeconds(60));
        driver2.get(WEBSITE_URL);

        fillSession();

        driver2.findElement(By.id("sessionId")).click();
        driver2.findElement(By.id("sessionId")).clear();
        driver2.findElement(By.id("sessionId")).sendKeys(generateRandomString(18));
        driver2.findElement(By.id("login-btn")).click();
        waitOnlineStatus();

        // перемещаем корабли на одну и ту же клетку.
        driver.findElement(By.id("arrowRight")).click();
        driver2.findElement(By.id("arrowRight")).click();
        sleep(30000); // ждем, чтобы изменения синхронизировались.
        var text = driver.findElement(By.id("sname5")).getText();
        var text2 = driver2.findElement(By.id("sname5")).getText();

        // второе окно браузера уже можно закрыть, а первое закроется самостоятельно после завершения теста.
        driver2.findElement(By.id("logout-btn")).click();
        driver2.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!verificationErrorString.isEmpty()) {
            fail(verificationErrorString);
        }

        /*
        проверяем, что для каждого корабля пользователя первым отображается
        его имя, а не чужого корабля.
        */
        assert(text.contains("[") && text.contains("]") &&
                text2.contains("[") && text2.contains("]"));
    }

    @Test
    public void testPriceUpdate() {
        fillSession();

        WebDriverWait dwd = new WebDriverWait(driver,Duration.ofSeconds(30) );
        dwd.until(ExpectedConditions.elementToBeClickable(By.id("act-0-0")));
        driver.findElement(By.id("act-0-0")).click();
        waitOnlineStatus();

        var initialMoney = Double.parseDouble(driver.findElement(By.id("money")).getText());
        driver.findElement(By.id("item1008buy")).click();
        var moneyAfterBuying = Double.parseDouble(driver.findElement(By.id("money")).getText());
        var firstPrice = initialMoney - moneyAfterBuying; // столько стоил товар.

        driver.findElement(By.id("item1008sell")).click();
        var moneyAfterSelling = Double.parseDouble(driver.findElement(By.id("money")).getText());
        driver.findElement(By.id("item1008buy")).click();
        var moneyAfterBuying2 = Double.parseDouble(driver.findElement(By.id("money")).getText());
        var secondPrice = moneyAfterSelling - moneyAfterBuying2; // столько стоил товар при повторной покупке (после якобы снижения цены от продажи).
        assert(firstPrice > secondPrice); // проверка снижения цены от продажи.
    }

    @Test
    public void testPricesRestore() throws InterruptedException {
        fillSession();

        WebDriverWait dwd = new WebDriverWait(driver,Duration.ofSeconds(30) );
        dwd.until(ExpectedConditions.elementToBeClickable(By.id("act-0-0")));
        driver.findElement(By.id("act-0-0")).click();
        waitOnlineStatus();

        var initialMoney = Double.parseDouble(driver.findElement(By.id("money")).getText());
        driver.findElement(By.id("item1008buy")).click();
        var moneyAfterBuying = Double.parseDouble(driver.findElement(By.id("money")).getText());
        var firstPrice = initialMoney - moneyAfterBuying; // столько стоил товар.

        var we = driver.findElement(By.id("act-0-1"));
        js.executeScript("arguments[0].scrollIntoView();", we);
        dwd.until(ExpectedConditions.elementToBeClickable(we));
        we.click();
        waitOnlineStatus();
        sleep(60000);
        driver.findElement(By.id("act-0-0")).click();
        waitOnlineStatus();
        driver.findElement(By.id("item1008buy")).click();
        var moneyAfterBuying2 = Double.parseDouble(driver.findElement(By.id("money")).getText());
        var secondPrice = moneyAfterBuying - moneyAfterBuying2; // столько стал стоить товар.
        assert(secondPrice == firstPrice); // проверяем, что цена вернулась к исходному значению.
    }
}
