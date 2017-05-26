package com.alkacon.opencms.v8.calendar.test.selenium.management;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


/**
 * Executes any user related actions
 *
 * Created by Nicolas Caycedo on 09.02.17.
 */
public abstract class UserManager {

    /**
     * Log in as a standard user.
     *
     * @param driver the WebDriver being used in the class where the login is required.
     */
    public static void standardLogin(WebDriver driver) {
        login(driver, Default.STANDARD_USER, Default.STANDARD_PASSWORD);
    }

    /**
     * Log in as an admin.
     *
     * @param driver the WebDriver being used in the class where the login is required.
     */
    public static void adminLogin(WebDriver driver) {
        login(driver, Default.ADMIN_USER, Default.ADMIN_PASSWORD);
    }

    /**
     * Internal method to log in as either an admin or a standard user.
     *
     * @param driver the WebDriver is passed by the public methods
     * @param user username taken from the Defaults class.
     * @param pass password taken from the Defaults class.
     */
    private static void login(WebDriver driver, String user, String pass) {
//        Get the login page
        String urlroot = "http://" + Default.HOST + ":" + Default.PORT;
        driver.get(urlroot + Default.SYS_LOGIN);
        driver.manage().window().maximize();

//        Send the keys to log in
        WebElement username = driver.findElement(By.name("ocUname"));
        WebElement password = driver.findElement(By.name("ocPword"));
        username.sendKeys(user);
        password.sendKeys(pass);
        password.sendKeys(Keys.ENTER);
    }
}
