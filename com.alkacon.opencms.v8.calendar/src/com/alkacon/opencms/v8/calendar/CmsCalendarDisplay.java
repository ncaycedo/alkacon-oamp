/*
 * File   : $Source: /alkacon/cvs/alkacon/com.alkacon.opencms.v8.calendar/src/com/alkacon/opencms/v8/calendar/CmsCalendarDisplay.java,v $
 * Date   : $Date: 2010/02/10 08:54:49 $
 * Version: $Revision: 1.4 $
 *
 * This file is part of the Alkacon OpenCms Add-On Module Package
 *
 * Copyright (c) 2008 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * The Alkacon OpenCms Add-On Module Package is free software: 
 * you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The Alkacon OpenCms Add-On Module Package is distributed 
 * in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Alkacon OpenCms Add-On Module Package.  
 * If not, see http://www.gnu.org/licenses/.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com.
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org.
 */

package com.alkacon.opencms.v8.calendar;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.i18n.CmsMessages;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsStringUtil;
import org.opencms.search.solr.CmsSolrResultList;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.logging.Log;

/**
 * Provides help methods to display calendar entries for the frontend.<p>
 * 
 * This includes methods to get calendar entries for a given date range as well as common settings
 * usable on the frontend to render holiday days and output tables.<p>
 * 
 * Extend this class to create special frontend views of a calendar, e.g. monthly or yearly views.<p>
 * 
 * @author Andreas Zahner
 * @author Peter Bonrad
 * 
 * @version $Revision: 1.4 $ 
 * 
 * @since 6.0.1
 */
public class CmsCalendarDisplay extends CmsCalendar {

    /** Node name of the UseConfig element. */
    public static final String NODE_USECONFIG = "UseConfig";

    /** Request parameter name for the calendar date. */
    public static final String PARAM_DATE = "calDat";

    /** Request parameter name for the calendar day. */
    public static final String PARAM_DAY = "calDay";

    /** Request parameter name for the calendar month. */
    public static final String PARAM_MONTH = "calMonth";

    /** Request parameter name for the uri. */
    public static final String PARAM_URI = "uri";

    /** Request parameter name for the calendar view resource. */
    public static final String PARAM_CALENDARVIEWRESOURCE = "calendarViewResource";

    /** Request parameter name for the view type. */
    public static final String PARAM_VIEWTYPE = "calView";

    /** Request parameter name for the calendar year. */
    public static final String PARAM_YEAR = "calYear";

    /** The type of the page for the day view. */
    public static final int PERIOD_DAY = 0;

    /** The type of the page for the month view. */
    public static final int PERIOD_MONTH = 1;

    /** The type of the page for the week view. */
    public static final int PERIOD_WEEK = 2;

    /** The type of the page for the year view. */
    public static final int PERIOD_YEAR = 3;

    /** Name of the property that stores the calendar end date. */
    public static final String PROPERTY_CALENDAR_ENDDATE = "calendar.enddate";

    /** Name of the property that determines if the calendar should be shown. */
    public static final String PROPERTY_CALENDAR_SHOW = "calendar.show";

    /** Name of the property that stores the calendar start date. */
    public static final String PROPERTY_CALENDAR_STARTDATE = "calendar.startdate";

    /** Name of the property that stores the calendar view URI. */
    public static final String PROPERTY_CALENDAR_URI = "calendar.uri";

    /** The name of the resource bundle for localized frontend messages. */
    public static final String RESOURCEBUNDLE_FRONTEND = "com.alkacon.opencms.v8.calendar.display";

    /** Resource type name for a calendar entry. */
    public static final String RESTYPE_ENTRY = "alkacon-v8-cal-entry";

    /** Resource type name for a calendar serial entry. */
    public static final String RESTYPE_ENTRY_SERIAL = "alkacon-v8-cal-serial";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsCalendarDisplay.class);

    /** The calendar object for the current date to display. */
    private Calendar m_currentDate;

    /** Flag if a real entry was found. */
    private boolean m_foundRealEntry;

    /** The JSP action element to use. */
    private CmsJspActionElement m_jsp;

    /** The calendar messages to use for the frontend. */
    private CmsMessages m_messages;

    /** The style. */
    private CmsCalendarStyle m_style;

    /** Flag to determine if AJAX links should be created. */
    private boolean m_useAjaxLinks;

    /** The type of the view which is displayed when clicking on a day.  */
    private int m_viewPeriod;

    /** The week day that should be marked as holiday day. */
    private int m_weekdayHoliday;

    /** The week day that should be marked as maybe holiday day. */
    private int m_weekdayMaybeHoliday;
    
    /** Path to the calendar view resource that configures the calendar */
    private String m_calendarViewFilePath;

    /**
     * @param calendarViewFilePath path to the calendar view resource that configures the calendar
     * @return this instance, for expressiveness (so that method calls can be chained)
     */
    public CmsCalendarDisplay setCalendarViewFilePath(String calendarViewFilePath) {
        this.m_calendarViewFilePath = calendarViewFilePath;
        return this;
    }

    /**
     * Empty constructor.<p>
     * 
     * Be sure to call the {@link #init(CmsJspActionElement)} method with an JSP action element.<p>
     */
    public CmsCalendarDisplay() {

        super();
    }

    /**
     * Constructor with initialized calendar object and JSP action element.<p>
     * 
     * @param jsp the JSP action element to use
     * @deprecated Use the default constructor and call {@link #init(org.opencms.jsp.CmsJspActionElement) }
     */
    @Deprecated
    public CmsCalendarDisplay(CmsJspActionElement jsp) {
        throw new UnsupportedOperationException("CmsCalendarDisplay(CmsJspActionElement)"
                + " not supported anymore."
                + " Use the default constructor and invoke"
                + " init(org.opencms.jsp.CmsJspActionElement)");
    }

    /**
     * Determines if the calendar day to check is the current day.<p>
     * 
     * @param current the current date
     * @param day the day to check
     * @return true if the calendar day to check is the current day, otherwise false
     */
    public static boolean isCurrentDay(Calendar current, Calendar day) {

        return ((current.get(Calendar.DATE) == day.get(Calendar.DATE))
            && (current.get(Calendar.YEAR) == day.get(Calendar.YEAR)) && (current.get(Calendar.MONTH) == day.get(Calendar.MONTH)));
    }

    /**
     * Sets the time of a calendar object.<p>
     * 
     * @param calendar the calendar to set the time
     * @param hour the new hour 
     * @param minute the new minute
     * @param second the new second
     * @return the calendar object with adjusted time
     */
    public static Calendar setDayTime(Calendar calendar, int hour, int minute, int second) {

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    /**
     * Adds optional calendar holiday days to the entry list.<p>
     * 
     * The bundle to use must have a key "calendar.holidays.datepattern" to specify the date pattern used in the bundle.
     * The holidays are stored in the format: "date = title_of_holiday;weekdaystatus". The date is used as key, the
     * weekday status has to be 1 (maybe holiday) or 2 (holiday).<p>
     * Example: <code>01/01/2006=New year;2</code> is a valid holiday entry.<p>
     * 
     * @param bundleName the name of the resource bundle
     * @return this instance, for expressiveness (so that method calls can be chained)
     */
    public CmsCalendarDisplay addHolidays(String bundleName) {

        addHolidays(bundleName, getJsp().getRequestContext().getLocale());
        return this;
    }

    /**
     * Adds optional calendar holiday days to the entry list.<p>
     * 
     * The bundle to use must have a key "calendar.holidays.datepattern" to specify the date pattern used in the bundle.
     * The holidays are stored in the format: "date = title_of_holiday;weekdaystatus". The date is used as key, the
     * weekday status has to be 1 (maybe holiday) or 2 (holiday).<p>
     * Example: <code>1/1/2009=New year;2</code> is a valid holiday entry.<p>
     * 
     * @param bundleName the name of the resource bundle
     * @param calendarLocale the Locale for the calendar to build
     * @return this instance, for expressiveness (so that method calls can be chained)
     */
    public CmsCalendarDisplay addHolidays(String bundleName, Locale calendarLocale) {

        CmsMessages holidays = new CmsMessages(bundleName, calendarLocale);
        String datePattern = holidays.key("calendar.holidays.datepattern");
        DateFormat df = new SimpleDateFormat(datePattern, calendarLocale);
        // get all keys from the bundle
        Enumeration<String> en = holidays.getResourceBundle().getKeys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            try {
                // try to get a valid date from the key
                Date holidayDate = df.parse(key);
                String value = holidays.key(key);
                String[] data = value.split(";");
                int weekdayStatus = 1;
                if (CmsStringUtil.isNotEmpty(data[1])) {
                    try {
                        weekdayStatus = Integer.parseInt(data[1].trim());
                    } catch (Exception e) {
                        // invalid entry, skip it
                        continue;
                    }
                }
                // create the calendar objects
                CmsCalendarEntryData entryData = new CmsCalendarEntryData(data[0].trim(), "", "", "", weekdayStatus);
                CmsCalendarEntryDate entryDate = new CmsCalendarEntryDate(
                    holidayDate.getTime(),
                    CmsCalendarEntryDate.MILLIS_01_PER_HOUR,
                    2 * CmsCalendarEntryDate.MILLIS_01_PER_HOUR,
                    0);
                CmsCalendarEntry entry = new CmsCalendarEntry(entryData, entryDate);
                // add the new entry to the calendar
                addEntry(entry);
            } catch (Exception e) {
                // ignore this exception, simply skip the entry 
            }
        }
        return this;
    }

    /**
     * Returns the HTML for a single calendar entry on an overview page.<p>
     * 
     * @param entry the calendar entry to use
     * @return the HTML for a single calendar entry on an overview page
     */
    public String buildOverviewDayEntry(CmsCalendarEntry entry) {

        return buildOverviewDayEntry(entry, true);
    }

    /**
     * Returns the HTML for a single calendar entry on an overview page.<p>
     * 
     * @param entry the calendar entry to use
     * @param timeFirst flag to check if the time information should be shown first or not
     * @return the HTML for a single calendar entry on an overview page
     */
    public String buildOverviewDayEntry(CmsCalendarEntry entry, boolean timeFirst) {

        StringBuilder result = new StringBuilder(2048);
        Calendar start = entry.getEntryDate().getStartDate();
        Calendar end = entry.getEntryDate().getEndDate();

        if (timeFirst && entry.getEntryData().isShowTime()) {
            // create entry date and time information at the beginning
            result.append("<span class=\"cal_entry_date\">");
            result.append(getMessages().key("calendar.day.starttime", new Object[] {start.getTime()}));
            if (entry.getEntryDate().getDuration() < 1) {
                // entry ends the same day
                if (entry.getEntryDate().getStartTime() != entry.getEntryDate().getEndTime()) {
                    result.append(" ");
                    result.append(getMessages().key("calendar.day.endtime", new Object[] {end.getTime()}));
                }
            } else {
                // entry ends on other day
                result.append(" ");
                result.append(getMessages().key("calendar.day.enddate", new Object[] {end.getTime()}));
            }
            result.append("</span><br/>");
        }

        // create the (linked) title of the entry
        String link = entry.getEntryData().getDetailUri();
        boolean linkPresent = false;
        if (CmsStringUtil.isNotEmpty(link)) {
            linkPresent = true;
            result.append("<a class=\"cal_entry_link\" href=\"");
            if (entry.getEntryDate().isSerialDate()) {
                // a serial entry has to get the time information in a request parameter to show the entry correctly
                StringBuffer params = new StringBuffer(64);
                params.append("?");
                params.append(CmsCalendarDisplay.PARAM_DATE);
                params.append("=");
                params.append(start.getTimeInMillis());
                result.append(getJsp().link(link + params));
            } else {
                result.append(getJsp().link(link));
            }
            result.append("\">");
        }
        result.append(entry.getEntryData().getTitle());
        if (linkPresent) {
            result.append("</a>");
        }

        // show the entry type if present and localized
        if (CmsStringUtil.isNotEmpty(entry.getEntryData().getType())) {
            String localizedType = getMessages().key("calendar.entry.type." + entry.getEntryData().getType());
            if (!localizedType.startsWith(CmsMessages.UNKNOWN_KEY_EXTENSION)) {
                result.append(" <span class=\"cal_entry_type\">(").append(localizedType).append(")</span>");
            }
        }

        // show the description if present
        String description = entry.getEntryData().getDescription();
        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(description)) {
            result.append("<br/><span class=\"cal_entry_description\">");
            result.append(description);
            result.append("</span>");
        }
        if (!timeFirst && entry.getEntryData().isShowTime()) {
            // create entry date and time information at the end
            result.append("<br/>");
            result.append("<span class=\"cal_entry_date\">");
            result.append(getMessages().key("calendar.day.starttime", new Object[] {start.getTime()}));
            if (entry.getEntryDate().getDuration() < 1) {
                // entry ends the same day
                if (entry.getEntryDate().getStartTime() != entry.getEntryDate().getEndTime()) {
                    result.append(" ");
                    result.append(getMessages().key("calendar.day.endtime", new Object[] {end.getTime()}));
                }
            } else {
                // entry ends on other day
                result.append(" ");
                result.append(getMessages().key("calendar.day.enddate", new Object[] {end.getTime()}));
            }
            result.append("</span>");
        }
        return result.toString();
    }

    /**
     * Returns the HTML to generate a link to a overview page using the specified calendar date.<p>
     * The needed parameters are added to the currently requested uri.
     * 
     * @param calendar the calendar date to build the link
     * @return the HTML to generate a link to the overview
     * @see #createLink(Calendar, String, boolean)
     */
    public String createLink(Calendar calendar) {

        return createLink(calendar, getJsp().getRequestContext().getUri(), true);
    }

    /**
     * Returns the HTML to generate a link to a overview page using the specified calendar date.<p>
     * The needed parameters are added to the currently requested uri.
     * 
     * @param calendar the calendar date to build the link
     * @param viewPeriod the view period type to link to
     * @return the HTML to generate a link to the overview
     * @see #createLink(Calendar, String, boolean)
     */
    public String createLink(Calendar calendar, int viewPeriod) {

        return createLink(calendar, getJsp().getRequestContext().getUri(), true, viewPeriod);
    }

    /**
     * Returns the HTML to generate a link to a overview page using the specified calendar date.<p>
     * The needed parameters are added to the given uri.
     * 
     * @param calendar the calendar date to build the link
     * @param uri the uri of the overview page
     * @return the HTML to generate a link to the overview
     * @see #createLink(Calendar, String, boolean)
     */
    public String createLink(Calendar calendar, String uri) {

        return createLink(calendar, uri, true);
    }

    /**
     * Returns the HTML to generate a link to a overview page using the specified calendar date.<p>
     * The needed parameters are added to the given uri.
     * 
     * @param calendar the calendar date to build the link
     * @param uri the uri of the overview page
     * @param useCmsLink should the generated link be handled by the OpenCms Link Handler 
     * @return the HTML to generate a link to the overview
     */
    public String createLink(Calendar calendar, String uri, boolean useCmsLink) {

        return createLink(calendar, uri, useCmsLink, getViewPeriod());
    }

    /**
     * Returns the HTML to generate a link to a overview page using the specified calendar date.<p>
     * The needed parameters are added to the given uri.
     * 
     * @param calendar the calendar date to build the link
     * @param uri the uri of the overview page
     * @param useCmsLink should the generated link be handled by the OpenCms Link Handler 
     * @param viewPeriod the view period type to link to
     * @return the HTML to generate a link to the overview
     */
    public String createLink(Calendar calendar, String uri, boolean useCmsLink, int viewPeriod) {

        if (isUseAjaxLinks()) {
            StringBuilder ajaxLink = new StringBuilder(64);
            String viewType = "";
            switch (viewPeriod) {
                case PERIOD_DAY:
                    viewType = "day";
                    break;
                case PERIOD_WEEK:
                    viewType = "week";
                    break;
                case PERIOD_MONTH:
                    viewType = "month";
                    break;
                case PERIOD_YEAR:
                    viewType = "year";
                    break;
                default:
                    viewType = "day";
            }
            ajaxLink.append("calendarCenterShow('");
            ajaxLink.append(viewType).append("', ");
            ajaxLink.append(calendar.get(Calendar.DATE)).append(", ");
            ajaxLink.append(calendar.get(Calendar.MONTH)).append(", ");
            ajaxLink.append(calendar.get(Calendar.YEAR)).append(");");
            return ajaxLink.toString();
        } else {
            StringBuilder nextLink = new StringBuilder(64);

            // append the URI to the JSP
            nextLink.append(uri);

            // append date parameters
            nextLink.append("?").append(PARAM_YEAR).append("=").append(calendar.get(Calendar.YEAR));
            nextLink.append("&amp;").append(PARAM_MONTH).append("=").append(calendar.get(Calendar.MONTH));
            nextLink.append("&amp;").append(PARAM_DAY).append("=").append(calendar.get(Calendar.DATE));

            // append view type parameter
            nextLink.append("&amp;").append(PARAM_VIEWTYPE).append("=").append(viewPeriod);

            // return a valid OpenCms link
            if (useCmsLink) {
                return getJsp().link(nextLink.toString());
            } else {
                return nextLink.toString();
            }
        }
    }

    /**
     * Returns the current date to display, depending on the found request parameters.<p>
     * 
     * @return the current date to display, depending on the found request parameters
     */
    public Calendar getCurrentDate() {

        if (m_currentDate == null) {
            Calendar calendar = new GregorianCalendar(getJsp().getRequestContext().getLocale());
            // get day parameters from request
            String yearParam = getJsp().getRequest().getParameter(PARAM_YEAR);
            String monthParam = getJsp().getRequest().getParameter(PARAM_MONTH);
            String dayParam = getJsp().getRequest().getParameter(PARAM_DAY);
            if (CmsStringUtil.isNotEmpty(yearParam)
                && CmsStringUtil.isNotEmpty(monthParam)
                && CmsStringUtil.isNotEmpty(dayParam)) {
                // build calendar settings specified by given request parameters
                int year = Integer.parseInt(yearParam);
                int month = Integer.parseInt(monthParam);
                int day = Integer.parseInt(dayParam);
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DATE, day);
            }
            m_currentDate = calendar;
        }
        return m_currentDate;
    }

    /**
     * @return the path if already configured with {@link #setCalendarViewFilePath(java.lang.String)};
     *      if not, tries recursively to find a uri property to a config file
     *      in the vfs
     */
    public String getCalendarViewFilePath() {
        // "search" means that opencms will look for the property in parent file
        // and parent folders
        return (null != m_calendarViewFilePath)
                ? m_calendarViewFilePath
                : getJsp().property(
                        CmsCalendarDisplay.PROPERTY_CALENDAR_URI,
                        "search",
                        getJsp().getRequestContext().getUri());
    }

    /** 
     * Returns the calendar entries of the collected resources that match the actual
     * time period.<p>
     * 
     * @param type the type of time period
     * @return all entries for the days of the specified range with their corresponding entries as lists
     */
    public Map<Date,List<CmsCalendarEntry>> getEntriesForCurrentPeriod(int type) {

        Calendar current = getCurrentDate();
        switch (type) {
            case PERIOD_DAY:
                return getEntriesForDay(current.get(Calendar.YEAR), current.get(Calendar.DAY_OF_YEAR));
            case PERIOD_WEEK:
                return getEntriesForWeek(current.get(Calendar.YEAR), current.get(Calendar.WEEK_OF_YEAR));
            case PERIOD_MONTH:
                return getEntriesForMonth(current.get(Calendar.YEAR), current.get(Calendar.MONTH));
            case PERIOD_YEAR:
                return getEntriesForYear(current.get(Calendar.YEAR));
            default:
                return new TreeMap<Date,List<CmsCalendarEntry>>();
        }
    }

    /**
     * Returns the list of calendar entries as {@link CmsCalendarEntry} objects for the specified day.<p>
     * 
     * @param year the year of the day to display
     * @param day the day of the year to get the entries for
     * @return the list of calendar entries for the specified day
     */
    public Map<Date,List<CmsCalendarEntry>> getEntriesForDay(int year, int day) {

        Calendar startDay = new GregorianCalendar(getJsp().getRequestContext().getLocale());
        startDay.set(Calendar.YEAR, year);
        startDay.set(Calendar.DAY_OF_YEAR, day);

        Calendar endDay = (Calendar)startDay.clone();
        return getEntriesForDays(startDay, endDay);
    }

    /**
     * Returns all entries for the days of the specified range with their corresponding entries as lists.<p>
     * 
     * The key of the Map has to be a Date object.<p>
     * 
     * The Map values are always lists of {@link CmsCalendarEntry} objects, if no entries are available for a specific day,
     * an empty List is stored in the Map.<p>
     * 
     * @param startDay the start day of the range
     * @param endDay the end day of the range
     * @return all entries for the days of the specified range with their corresponding entries as lists
     */
    public Map<Date,List<CmsCalendarEntry>> getEntriesForDays(Calendar startDay, Calendar endDay) {

        Map<Date,List<CmsCalendarEntry>> displayDays =
                new TreeMap<Date,List<CmsCalendarEntry>>();
        // first get all entries to display
        Map<Date,List<CmsCalendarEntry>> displayEntries =
                getDisplayedEntries(startDay, endDay);
        
        Calendar runDay = setDayTime(startDay, 0, 0, 0);
        while (true) {
            List<CmsCalendarEntry> entries = displayEntries.get(runDay.getTime());
            if (entries == null) {
                entries = new ArrayList<CmsCalendarEntry>(0);
            }
            displayDays.put(runDay.getTime(), entries);

            // increase day to next day
            runDay.add(Calendar.DAY_OF_YEAR, 1);
            if (runDay.after(endDay)) {
                // runDay has reached endDay -> stop loop
                break;
            }
        }
        return displayDays;
    }

    /**
     * Returns all displayed days of the specified month with their corresponding entries as lists.<p>
     * 
     * The key of the Map has to be a Date object.<p>
     * 
     * The Map values are always lists of {@link CmsCalendarEntry} objects, if no entries are available for a specific day,
     * an empty List is returned.<p>
     * 
     * @param year the year of the month to display
     * @param month the month to display
     * @return all displayed days of the specified day range with their corresponding entries as lists
     */
    public Map<Date,List<CmsCalendarEntry>> getEntriesForMonth(int year, int month) {

        Calendar startDay = new GregorianCalendar(year, month, 1);
        Calendar endDay = new GregorianCalendar(year, month, startDay.getActualMaximum(Calendar.DAY_OF_MONTH));
        return getEntriesForDays(startDay, endDay);
    }

    /**
     * Returns all displayed days of the specified week with their corresponding entries as lists.<p>
     * 
     * The key of the Map has to be a Date object.<p>
     * 
     * The Map values are always lists of {@link CmsCalendarEntry} objects, if no entries are available for a specific day,
     * an empty List is returned.<p>
     * 
     * @param year the year of the month to display
     * @param week the week of the year to display
     * @return all displayed days of the specified day range with their corresponding entries as lists
     */
    public Map<Date,List<CmsCalendarEntry>> getEntriesForWeek(int year, int week) {

        Calendar startDay = new GregorianCalendar(getJsp().getRequestContext().getLocale());
        startDay.set(Calendar.YEAR, year);
        startDay.set(Calendar.WEEK_OF_YEAR, week);
        startDay.set(Calendar.DAY_OF_WEEK, startDay.getFirstDayOfWeek());

        Calendar endDay = (Calendar)startDay.clone();
        endDay.add(Calendar.DAY_OF_YEAR, 6);

        return getEntriesForDays(startDay, endDay);
    }

    /**
     * Returns all displayed days of the specified year with their corresponding entries as lists.<p>
     * 
     * The key of the Map has to be a Date object.<p>
     * 
     * The Map values are always lists of {@link CmsCalendarEntry} objects, if no entries are available for a specific day,
     * an empty List is returned.<p>
     * 
     * @param year the year of the month to display
     * @return all displayed days of the specified day range with their corresponding entries as lists
     */
    public Map<Date,List<CmsCalendarEntry>> getEntriesForYear(int year) {

        Calendar startDay = new GregorianCalendar(getJsp().getRequestContext().getLocale());
        startDay.set(Calendar.YEAR, year);
        startDay.set(Calendar.MONTH, Calendar.JANUARY);
        startDay.set(Calendar.DAY_OF_MONTH, 1);

        Calendar endDay = (Calendar)startDay.clone();
        endDay.set(Calendar.MONTH, Calendar.DECEMBER);
        endDay.set(Calendar.DAY_OF_MONTH, 31);

        return getEntriesForDays(startDay, endDay);
    }

    /**
     * Returns the name of the resource bundle to use for frontend output.<p>
     * 
     * @return the name of the resource bundle to use for frontend output
     */
    public String getFrontendResourceBundle() {

        return "com.alkacon.opencms.v8.calendar.display";
    }

    /**
     * Filters the calendar entries and separates them depending if they are holiday or common entries.<p>
     *
     * @param entries a list of calendar entries
     * @return a list where only "holiday" entries are in the list (holidays are filtered out)
     */
    public List<CmsCalendarEntry> getHolidayEntries(List<CmsCalendarEntry> entries) {

        List<CmsCalendarEntry> result = new ArrayList<CmsCalendarEntry>();

        for (CmsCalendarEntry entry : entries) {
            if (entry.getEntryData().getWeekdayStatus() > I_CmsCalendarEntryData.WEEKDAYSTATUS_WORKDAY) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * Returns a string with the holiday included in the given list with calendar entries.<p> 
     * 
     * @param entries list of calendar entries with the included holidays
     * @return a string with all holiday included in the given list of calendar entries
     */
    public String getHolidays(List<CmsCalendarEntry> entries) {

        StringBuilder result = new StringBuilder();

        Iterator<CmsCalendarEntry> holidayIt = getHolidayEntries(entries).iterator();
        if (holidayIt.hasNext()) {
            boolean isFirst = true;
            while (holidayIt.hasNext()) {
                if (!isFirst) {
                    result.append(" - ");
                }
                CmsCalendarEntry holiday = holidayIt.next();
                result.append(holiday.getEntryData().getTitle());
                isFirst = false;
            }
        }

        return result.toString();
    }

    /**
     * Returns the JSP action element to use.<p>
     *
     * @return the JSP action element to use
     */
    public CmsJspActionElement getJsp() {

        return m_jsp;
    }

    /**
     * Returns the calendar messages to use for the frontend.<p>
     *
     * @return the calendar messages to use for the frontend
     */
    public CmsMessages getMessages() {

        return m_messages;
    }

    /**
     * Returns the specified amount of most current entries, starting from the current date.<p>
     * 
     * The result is sorted by the start date of the entries, ascending.<p>
     * 
     * @param count the amount of entries to return
     * @return the most current entries
     */
    public List<CmsCalendarEntry> getMostCurrentEntries(int count) {

        List<CmsCalendarEntry> result = new ArrayList<CmsCalendarEntry>(count);

        // determine start date
        Calendar startDay = new GregorianCalendar(getJsp().getRequestContext().getLocale());
        startDay.setTimeInMillis(System.currentTimeMillis());
        startDay = setDayTime(startDay, 0, 0, 0);
        // determine current view range
        Calendar endDay = (GregorianCalendar)startDay.clone();
        endDay.add(Calendar.YEAR, 30);

        List<CmsCalendarEntryDate> viewDates =
                new ArrayList<CmsCalendarEntryDate>(1);
        CmsCalendarEntryDate viewDate = new CmsCalendarEntryDate(startDay, setDayTime(endDay, 23, 59, 59));
        viewDates.add(viewDate);
        // create the simple view
        CmsCalendarViewSimple calendarView = new CmsCalendarViewSimple(viewDates);

        Iterator<CmsCalendarEntry> entryIt = getEntries().iterator();
        while (entryIt.hasNext()) {
            CmsCalendarEntry entry = entryIt.next();
            if (entry.getEntryDate().isSerialDate()) {
                // serial date, create entry clones for every future occurance
                CmsCalendarEntryDateSerial serialDate = (CmsCalendarEntryDateSerial)entry.getEntryDate();
                result.addAll(serialDate.matchCalendarView(entry, calendarView, count));
            } else if (entry.getEntryDate().getStartDate().getTimeInMillis() > startDay.getTimeInMillis()) {
                // common entry that is in the future, add it to result
                result.add(entry);
            }
        }

        // sort the collected entries by date ascending
        calendarView.sort(result);

        if ((count > 0) && (result.size() > count)) {
            // cut off all items > count
            result = result.subList(0, count);
        }

        return result;
    }

    /**
     * Returns the next time range to show calendar entries for.<p>
     * Used for the navigation.
     * 
     * @param actual the actual date from which the next time range should be calculated
     * @param type the type of period
     * @return a date for which a list of calendar entries should be shown
     */
    public Calendar getNextPeriod(Calendar actual, int type) {

        Calendar cal = (Calendar)actual.clone();
        switch (type) {
            case PERIOD_DAY:
                cal.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case PERIOD_MONTH:
                cal.add(Calendar.MONTH, 1);
                break;
            case PERIOD_WEEK:
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case PERIOD_YEAR:
                cal.add(Calendar.YEAR, 1);
                break;
            default:
                break;
        }

        return cal;
    }

    /**
     * Returns the next time range to the actual date to show calendar entries for.<p>
     * Used for the navigation.
     * 
     * @param type the type of period
     * @return a date for which a list of calendar entries should be shown
     */
    public Calendar getNextPeriod(int type) {

        return getNextPeriod(getCurrentDate(), type);
    }

    /**
     * Returns the previous time range to show calendar entries for.<p>
     * Used for the navigation.
     * 
     * @param actual the actual date from which the previous time range should be calculated
     * @param type the type of period
     * @return a date for which a list of calendar entries should be shown
     */
    public Calendar getPreviousPeriod(Calendar actual, int type) {

        Calendar cal = (Calendar)actual.clone();
        switch (type) {
            case PERIOD_DAY:
                cal.add(Calendar.DAY_OF_YEAR, -1);
                break;
            case PERIOD_MONTH:
                cal.add(Calendar.MONTH, -1);
                break;
            case PERIOD_WEEK:
                cal.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case PERIOD_YEAR:
                cal.add(Calendar.YEAR, -1);
                break;
            default:
                break;
        }

        return cal;
    }

    /**
     * Returns the previous time range to the actual date to show calendar entries for.<p>
     * Used for the navigation.
     * 
     * @param type the type of period
     * @return a date for which a list of calendar entries should be shown
     */
    public Calendar getPreviousPeriod(int type) {

        return getPreviousPeriod(getCurrentDate(), type);
    }

    /**
     * Filters the calendar entries and separates them depending if they are holiday or common entries.<p>
     *
     * @param entries a list of calendar entries
     * @return a list where only "real" entries are in the list (holidays are filtered out)
     */
    public List<CmsCalendarEntry> getRealEntries(List<CmsCalendarEntry> entries) {

        List<CmsCalendarEntry> result = new ArrayList<CmsCalendarEntry>();

        for (CmsCalendarEntry entry : entries) {
            if (entry.getEntryData().getWeekdayStatus() <= I_CmsCalendarEntryData.WEEKDAYSTATUS_WORKDAY) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * Returns the CSS style object that is used to format the calendar output.<p>
     * 
     * @return the CSS style object that is used to format the calendar output
     */
    public CmsCalendarStyle getStyle() {

        return m_style;
    }

    /**
     * Returns the type of the view which is displayed when clicking on a day.<p>
     * 
     * @return the type of the view which is displayed when clicking on a day
     */
    public int getViewPeriod() {

        return m_viewPeriod;
    }

    /**
     * Returns the week day that should be marked as holiday day.<p>
     *
     * @return the week day that should be marked as holiday day
     */
    public int getWeekdayHoliday() {

        return m_weekdayHoliday;
    }

    /**
     * Returns the week day that should be marked as maybe holiday day.<p>
     *
     * @return the week day that should be marked as maybe holiday day
     */
    public int getWeekdayMaybeHoliday() {

        return m_weekdayMaybeHoliday;
    }

    /**
     * Returns if a real entry was found.<p>
     * 
     * @return if a real entry was found
     */
    public boolean hasRealEntries() {

        return m_foundRealEntry;
    }

    /**
     * Initializes the JSP action element and the calendar messages to use for the frontend.<p>
     * 
     * @param jsp the JSP action element to use
     * @return this instance, for expressiveness (so that method calls can be chained)
     */
    public CmsCalendarDisplay init(CmsJspActionElement jsp) {

        // initialize style class
        m_style = new CmsCalendarStyle();
        // initialize other members
        m_foundRealEntry = false;
        if (jsp != null) {
            m_jsp = jsp;
            // initializes localized messages
            m_messages = getJsp().getMessages(
                getFrontendResourceBundle(),
                getJsp().getRequestContext().getLocale().toString());
            m_weekdayHoliday = Integer.parseInt(getMessages().key("calendar.weekday.holiday", "-1"));
            m_weekdayMaybeHoliday = Integer.parseInt(getMessages().key("calendar.weekday.maybeholiday", "-1"));
            // initialize view period type
            String period = getJsp().getRequest().getParameter(PARAM_VIEWTYPE);
            if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(period)) {
                try {
                    setViewPeriod(Integer.parseInt(period));
                } catch (NumberFormatException e) {
                    setViewPeriod(PERIOD_DAY);
                }
            } else {
                setViewPeriod(PERIOD_DAY);
            }
        } else {
            m_weekdayHoliday = -1;
            m_weekdayMaybeHoliday = -1;
            setViewPeriod(PERIOD_DAY);
        }
        return this;
    }

    /**
     * Initializes the calendar entries using Solr.<p>
     * 
     *
     * @return this instance, for expressiveness (so that method calls can be chained)
     * @param isOnline
     */
    public CmsCalendarDisplay initCalendarEntries(boolean isOnline) {
        String solrIndexName = isOnline ? "Solr Online" : "Solr Offline";

        try {
            String query = String.format("fq=type:(%s OR %s)&rows=10000000", RESTYPE_ENTRY, RESTYPE_ENTRY_SERIAL);
            CmsSolrResultList resources = OpenCms.getSearchManager().getIndexSolr(solrIndexName).search(getJsp().getCmsObject(), query);

            List<CmsCalendarEntry> calendarEntries = createCalendarEntries(
                    resources,
                    PROPERTY_CALENDAR_STARTDATE,
                    PROPERTY_CALENDAR_ENDDATE,
                    CmsPropertyDefinition.PROPERTY_TITLE,
                    CmsPropertyDefinition.PROPERTY_DESCRIPTION);
            setEntries(calendarEntries);
        } catch (CmsException e) {
            // error collecting resources, an empty calendar is returned
            if (LOG.isErrorEnabled()) {
                LOG.error(Messages.get().getBundle().key(
                    Messages.LOG_CALENDAR_RESOURCES_1,
                    getJsp().getRequestContext().getUri()),e);
            }
        }
        return this;
    }

    /**
     * Returns if AJAX links should be created for calendar pagination.<p>
     * 
     * @return true if AJAX links should be created for calendar pagination, otherwise false
     */
    public boolean isUseAjaxLinks() {

        return m_useAjaxLinks;
    }

    /**
     * Sets the JSP action element to use.<p>
     *
     * @param jsp the JSP action element to use
     * @return this instance, for expressiveness (so that method calls can be chained)
     */
    public CmsCalendarDisplay setJsp(CmsJspActionElement jsp) {

        m_jsp = jsp;
        return this;
    }

    /**
     * Sets the calendar messages to use for the frontend.<p>
     *
     * @param messages the calendar messages to use for the frontend
     * @return this instance, for expressiveness (so that method calls can be chained)
     */
    public CmsCalendarDisplay setMessages(CmsMessages messages) {

        m_messages = messages;
        return this;
    }

    /**
     * Sets the CSS style object that is used to format the calendar output.<p>
     * 
     * @param style the CSS style object that is used to format the calendar output
     * @return this instance, for expressiveness (so that method calls can be chained)
     */
    public CmsCalendarDisplay setStyle(CmsCalendarStyle style) {

        m_style = style;
        return this;
    }

    /**
     * Sets if AJAX links should be created for calendar pagination.<p>
     * 
     * @param useAjaxLinks true if AJAX links should be created for calendar pagination, otherwise false
     * @return this instance, for expressiveness (so that method calls can be chained)
     */
    public CmsCalendarDisplay setUseAjaxLinks(boolean useAjaxLinks) {

        m_useAjaxLinks = useAjaxLinks;
        return this;
    }

    /**
     * Sets the type of the view which is displayed when clicking on a day.<p>
     * 
     * @param viewPeriod the type of the view which is displayed when clicking on a day
     * @return this instance, for expressiveness (so that method calls can be chained)
     */
    public CmsCalendarDisplay setViewPeriod(int viewPeriod) {

        m_viewPeriod = viewPeriod;
        return this;
    }

    /**
     * Creates calendar entries from the given list of resources and their properties.<p>
     * 
     * The following properties are read to create calendar entries from the
     * resources:
     * <dl>
     * <dt>the start date (mandatory)</dt>
     * <dd>The start date of the entry is stored in this value, as long value
     * for common entries and key/value pairs for serial entries.</dd>
     * <dt>the end date (optional)</dt>
     * <dd>The optional end date of an entry that is used for generating the
     * overviews as long value. For serial date entries, this property can also
     * be used to store the class name of a class implementing the
     * {@link I_CmsCalendarSerialDateContent} interface. In this case, this
     * class will be used to create the <tt>CmsCalendarEntry</tt>s from the
     * resources.</dd>
     * <dt>the entry title (mandatory)</dt>
     * <dt>the entry description (optional)</dt>
     * <dd>teaser text for calendar entries that is shown in the overviews.</dd>
     * </dl>
     * The mandatory properties must have been set to create correct calendar
     * entries.<p>
     * 
     * @param resources the resources to create calendar entries from
     * @param pStartDate the name of the property to use for the start date
     * @param pEndDate the name of the property to use for the end date
     * @param pTitle the name of the property to use for the title
     * @param pDescription the name of the property to use for the description
     * @return the calendar entries from the given list of resources and their properties
     */
    protected List<CmsCalendarEntry> createCalendarEntries(
        CmsSolrResultList resources,
        String pStartDate,
        String pEndDate,
        String pTitle,
        String pDescription) {

        CmsObject cms = getJsp().getCmsObject();
        List<CmsCalendarEntry> result = new ArrayList<CmsCalendarEntry>(resources.size());
        // instanciate default serial date content class
        I_CmsCalendarSerialDateContent defaultContent = new CmsSerialDateContentBean();
        for (int i = resources.size() - 1; i > -1; i--) {
            // loop the resources
            CmsResource res = resources.get(i);
            // read the title of the resource
            String title = getPropertyValue(cms, res, pTitle, null);
            // read the start date property.
            // AlkaconV8CalendarSerialDates entries save the serial data in
            //  the "Serialdate" element that is mapped to the property
            // "calendar.startdate"
            String startDate = getPropertyValue(cms, res, pStartDate, null);
            if (CmsStringUtil.isNotEmpty(title) && CmsStringUtil.isNotEmpty(startDate)) {
                // required properties were found, resource can be used as an entry
                String endDate = getPropertyValue(cms, res, pEndDate, startDate);
                String description = getPropertyValue(cms, res, pDescription, "");
                String showTimeStr = getPropertyValue(
                    cms,
                    res,
                    I_CmsCalendarEntryData.PROPERTY_CALENDAR_SHOWTIME,
                    CmsStringUtil.TRUE);
                // determine entry resource type
                String type = "";
                try {
                    type = OpenCms.getResourceManager().getResourceType(res.getTypeId()).getTypeName();
                } catch (CmsException e) {
                    // ignore, this information is not important
                    LOG.warn("Could not retrieve entry type of calendar resource: "
                            + e.getLocalizedMessage(), e);
                }

                // create the calendar entry data
                String resPath = getJsp().getRequestContext().getSitePath(res);
                CmsCalendarEntryData entryData = new CmsCalendarEntryData(title, description, type, resPath, 0);
                entryData.setShowTime(Boolean.valueOf(showTimeStr));

                // create the calendar entry date
                CmsCalendarEntryDate entryDate = null;

                if (startDate.indexOf('=') == -1) {
                    // start date property does not contain key/value pairs, this is a common entry
                    Calendar start = new GregorianCalendar(getJsp().getRequestContext().getLocale());
                    start.setTimeInMillis(Long.parseLong(startDate));
                    Calendar end = new GregorianCalendar(getJsp().getRequestContext().getLocale());
                    end.setTimeInMillis(Long.parseLong(endDate));
                    // create a new calendar entry date
                    entryDate = new CmsCalendarEntryDate(start, end);
                    // create the calendar entry
                    CmsCalendarEntry entry = new CmsCalendarEntry(entryData, entryDate);
                    // add the entry to the result
                    result.add(entry);
                } else {
                    // this might be a serial date, try to create it
                    CmsCalendarEntry entry = null;
                    String clazzName = getPropertyValue(cms, res, pEndDate, null);
                    if (CmsStringUtil.isEmpty(clazzName)) {
                        // did not find a class name in the property, use default content bean to get entry
                        entry = defaultContent.getSerialEntryForCalendar(cms, res);
                    } else {
                        // found a class name, use it to get the entry 
                        try {
                            I_CmsCalendarSerialDateContent serialContent = (I_CmsCalendarSerialDateContent)Class.forName(
                                clazzName).newInstance();
                            entry = serialContent.getSerialEntryForCalendar(cms, res);
                        } catch (Exception e) {
                            // implementing class was not found
                            if (LOG.isErrorEnabled()) {
                                LOG.error(
                                    Messages.get().getBundle().key(
                                        Messages.LOG_CALENDAR_SERIALDATE_CLASS_3,
                                        clazzName,
                                        PROPERTY_CALENDAR_ENDDATE,
                                        resPath),
                                    e);
                            }
                        }
                    }
                    if (entry != null) {
                        result.add(entry);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns value of the given property from the given resource, or the given default value if any problem.<p>
     * 
     * @param cms the current CMS context
     * @param resource the resource 
     * @param propertyName the property name
     * @param defaultValue the default value
     * 
     * @return value of the given property from the given resource
     */
    protected String getPropertyValue(CmsObject cms, CmsResource resource, String propertyName, String defaultValue) {

        try {
            return cms.readPropertyObject(resource, propertyName, false).getValue(defaultValue);
        } catch (CmsException e) {
            LOG.warn(e.getLocalizedMessage(), e);
            return defaultValue;
        }
    }

    /**
     * Creates a sorted Map of entries for the given date range.<p>
     * 
     * The Map uses the Date object representing a day of the range (with time information 0:00:00) as key, 
     * the value is a (sorted) list of calendar entries for the day.<p>
     * 
     * @param startDay the start day of the range
     * @param endDay the end day of the range
     * @return a sorted Map of entries for the given date range
     */
    private Map<Date,List<CmsCalendarEntry>> getDisplayedEntries(Calendar startDay, Calendar endDay) {
        
        // create the list of view dates
        List<CmsCalendarEntryDate> viewDates = new ArrayList<CmsCalendarEntryDate>(1);
        CmsCalendarEntryDate viewDate = new CmsCalendarEntryDate(setDayTime(startDay, 0, 0, 0), setDayTime(
            endDay,
            23,
            59,
            59));
        viewDates.add(viewDate);

        // create the simple view
        CmsCalendarViewSimple view = new CmsCalendarViewSimple(viewDates);
        // get all entries for the view range
        List<CmsCalendarEntry> entries = getEntries(view);

        Map<Date,List<CmsCalendarEntry>> displayEntries =
                new TreeMap<Date,List<CmsCalendarEntry>>();

        for (CmsCalendarEntry entry : entries) {
            // get the entry start date
            Calendar entryStart = (Calendar)entry.getEntryDate().getStartDate().clone();
            entryStart = setDayTime(entryStart, 0, 0, 0);
            // get the list of entries for the start day
            List<CmsCalendarEntry> dayEntries =
                    displayEntries.get(entryStart.getTime());
            if (dayEntries == null) {
                // no entries for the current day found, create empty list
                dayEntries = new ArrayList<CmsCalendarEntry>(3);
            }
            // check if a real entry was found
            if (entry.getEntryData().getWeekdayStatus() <= I_CmsCalendarEntryData.WEEKDAYSTATUS_WORKDAY) {
                m_foundRealEntry = true;
            }
            // add current entry to list
            dayEntries.add(entry);
            // put list of entries to Map
            displayEntries.put(entryStart.getTime(), dayEntries);
        }
        return displayEntries;
    }
}
