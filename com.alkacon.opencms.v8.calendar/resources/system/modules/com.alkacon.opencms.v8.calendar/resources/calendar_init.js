$(document).ready(function() {
    var currDay = new Date().getDate();
    var currMonth = new Date().getMonth();
    var currYear = new Date().getFullYear();
    var calItemsJsp = "/system/modules/com.alkacon.opencms.v8.calendar/elements/fullcalendar-event-list-json.jsp";

    function calendarCenterLoad() {
        calendarCenterShow(currDay, currMonth, currYear);
    }

    function calendarCenterShow(cDay, cMonth, cYear) {

        $("#calendarcenter").fullCalendar({
            defaultDate: moment({year: cYear, month: cMonth, date: cDay}),
            editable: false,
            allDayDefault: false,
            defaultView: ["basicDay", "basicWeek", "month"][calDefaultView],
            lang: calLocale,
            timeFormat: 'H:mm',
            header: {
                left: 'prev,next today',
                center: 'title',
                right: 'month,basicWeek,basicDay'
            },
            events: function(start, end, timezone, callback) {
                $.get(calItemsJsp,
                    {
                        calendarViewResource: contentFilename,
                        __locale: __calLocale,
                        sYear: start.year(),
                        sMonth: start.month(),
                        sDay: start.date(),
                        eYear: end.year(),
                        eMonth: end.month(),
                        eDay: end.date()
                    },
                    function(data) {
                        var events = eval(data);
                        callback(events);
                    }
                );
            },
            fixedWeekCount: false
        });
    }

    calendarCenterLoad();
    $("#calendarcenterload").remove();
})