package test;

import com.alkacon.opencms.v8.calendar.CmsCalendarDisplay;
import com.alkacon.opencms.v8.calendar.CmsCalendarEntryDateSerial;
import com.alkacon.opencms.v8.calendar.CmsSerialDateContentBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.main.CmsException;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestCmsCalendarEntryDateSerial {
    @Mock
    CmsObject cmsObject;
    @Mock
    CmsResource calResource;

    @Test
    void test_daily_event_repeating_everyday() {
//        TODO: implement test. Mock a CmsObject and a CmsResource to call the getSerialEntryFrom() method from CmsSerialDateContentBean.
        try {
            Mockito.when(cmsObject.readPropertyObject(calResource, CmsCalendarDisplay.PROPERTY_CALENDAR_STARTDATE, false)
                    .getValueMap(new HashMap<>())).thenReturn(buildMap(CmsCalendarDisplay.PROPERTY_CALENDAR_STARTDATE));
            Assert.fail("can't mock value map");
        } catch (CmsException e) {
            e.printStackTrace();
        }

        Mockito.when(cmsObject.getRequestContext().getLocale()).thenReturn(new Locale("en"));

        CmsCalendarEntryDateSerial entryDateSerial = CmsSerialDateContentBean.getSerialEntryFrom(cmsObject, calResource);
        Assert.assertNotNull(entryDateSerial);
    }

    @Test
    void test_daily_event_repeating_weekdays() {
//        TODO: implement test.
    }

    @Test
    void test_daily_event_repeating_every_other_day() {
//        TODO: implement test.
    }

    @Test
    void test_weekly_event_repeating_every_week_monday_wednesday_friday() {
//        TODO: implement test.
    }

    @Test
    void test_weekly_event_repeating_every_other_week_tuesday_thursday() {
//        TODO: implement test.
    }

    @Test
    void test_monthly_event_on_seventh_day_every_month() {
//        TODO: implement test.
    }

    @Test
    void test_monthly_event_at_second_monday_every_two_months() {
//        TODO: implement test.
    }

    @Test
    void test_yearly_event_on_january_seventh() {
//        TODO: implement test.
    }

    @Test
    void test_yearly_event_at_first_sunday_in_march() {
//        TODO: implement test.
    }

    private HashMap<String, String> buildMap(String property) {
        HashMap<String, String> values = new HashMap<>();
        if (property.equals(CmsCalendarDisplay.PROPERTY_CALENDAR_STARTDATE)) {
            values.put("endtype", "3");
            values.put("startdate", "1488556800000");
            values.put("enddate", "1488560400000");
            values.put("everyworkingday", "false");
            values.put("interval", "1");
            values.put("type", "1");
            values.put("serialenddate", "1489420800000");
        }
        return values;
    }
}
