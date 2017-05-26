package com.alkacon.opencms.v8.calendar.test.selenium.management;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import java.util.concurrent.TimeUnit;

/**
 * Browser setup for the driver object. It helps up clearing up space and making the code cleaner in the test classes.
 *
 * Created by Nicolas Caycedo on 09.02.17.
 */
public abstract class DriverManager {
    /**
     * Sets up the driver to start working with any selected browser. Momentarily only Firefox and Chrome are included.
     *
     * @param browser the browser to work with, lowercase and no spaces.
     * @return a WebDriver ready to start working with
     */
    public static WebDriver setDriver(String browser) {
//        Set the directory for all supported browser drivers
        System.setProperty("webdriver.gecko.driver", "webdrivers/geckodriver");
        System.setProperty("webdriver.chrome.driver", "webdrivers/chromedriver");

//        Select a driver based on the parameters
        WebDriver driver;
        switch (browser.toLowerCase()) {
            case "chrome":
                driver = new ChromeDriver();
                break;
            case "firefox":
                driver = new FirefoxDriver();
                break;
            default:
                System.out.println("Browser not recognized");
                throw new IllegalArgumentException();
        }

//        Set the default wait time and return
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        return driver;
    }
}
