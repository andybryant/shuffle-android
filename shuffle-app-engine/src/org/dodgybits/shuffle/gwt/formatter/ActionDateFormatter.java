package org.dodgybits.shuffle.gwt.formatter;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;

public class ActionDateFormatter {

	private DateTimeFormat dateFormat;
	
	public ActionDateFormatter() {
		dateFormat = DateTimeFormat.getFormat("d MMM");
	}
	public String getShortDate(Date date) {
		String result = "";
		if (date != null) {
			if (isSameDay(date, new Date())) {
				// only show time if date is today
				result = DateTimeFormat.getShortTimeFormat().format(date);
			} else {
				result = dateFormat.format(date);
			}
		}
		return result;
	}
	
	@SuppressWarnings("deprecation")
	private static boolean isSameDay(Date date1, Date date2) {
		return date1.getYear() == date2.getYear() &&
			date1.getMonth() == date2.getMonth() &&
			date1.getDate() == date2.getDate();
	}
}
