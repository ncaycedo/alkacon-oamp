package com.alkacon.opencms.v8.calendar.test.selenium;

import com.alkacon.opencms.v8.calendar.test.selenium.management.Default;
import com.alkacon.opencms.v8.calendar.test.selenium.management.DriverManager;
import com.alkacon.opencms.v8.calendar.test.selenium.management.UserManager;

import org.junit.Before;

import org.openqa.selenium.WebDriver;


/**
 * Integration test for the calendar view resource.
 *
 * @author Nicolas Caycedo
 */
public class TestCalendarView {
    private WebDriver driver;

    @Before
    public void setup() {
        driver = DriverManager.setDriver("firefox");
        UserManager.adminLogin(driver);
    }
}
