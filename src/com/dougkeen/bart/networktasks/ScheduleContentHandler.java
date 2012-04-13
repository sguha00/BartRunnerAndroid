package com.dougkeen.bart.networktasks;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.Line;
import com.dougkeen.bart.model.RealTimeDepartures;
import com.dougkeen.bart.model.Route;
import com.dougkeen.bart.model.ScheduleInformation;
import com.dougkeen.bart.model.ScheduleItem;
import com.dougkeen.bart.model.Station;

public class ScheduleContentHandler extends DefaultHandler {
	public ScheduleContentHandler(Station origin, Station destination) {
		super();
		schedule = new ScheduleInformation(origin, destination);
	}

	private final static List<String> TAGS = Arrays.asList("date", "time",
			"trip");

	private final static DateFormat TRIP_DATE_FORMAT;
	private final static DateFormat REQUEST_DATE_FORMAT;

	private final static TimeZone PACIFIC_TIME = TimeZone
			.getTimeZone("America/Los_Angeles");

	static {
		TRIP_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy h:mm a");
		REQUEST_DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy h:mm a");

		TRIP_DATE_FORMAT.setTimeZone(PACIFIC_TIME);
		REQUEST_DATE_FORMAT.setTimeZone(PACIFIC_TIME);
	}

	private ScheduleInformation schedule;

	public ScheduleInformation getSchedule() {
		return schedule;
	}

	private String currentValue;
	private boolean isParsingTag;

	private String requestDate;
	private String requestTime;

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (isParsingTag) {
			currentValue = new String(ch, start, length);
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (TAGS.contains(localName)) {
			isParsingTag = true;
		}
		if (localName.equals("trip")) {
			ScheduleItem trip = new ScheduleItem();
			String originDate = null;
			String originTime = null;
			String destinationDate = null;
			String destinationTime = null;
			for (int i = attributes.getLength() - 1; i >= 0; i--) {
				if (attributes.getLocalName(i).equalsIgnoreCase("origin")) {
					trip.setOrigin(Station.getByAbbreviation(attributes
							.getValue(i)));
				} else if (attributes.getLocalName(i).equalsIgnoreCase(
						"destination")) {
					trip.setDestination(Station.getByAbbreviation(attributes
							.getValue(i)));
				} else if (attributes.getLocalName(i).equalsIgnoreCase("fare")) {
					trip.setFare(attributes.getValue(i));
				} else if (attributes.getLocalName(i).equalsIgnoreCase(
						"origTimeMin")) {
					originTime = attributes.getValue(i);
				} else if (attributes.getLocalName(i).equalsIgnoreCase(
						"origTimeDate")) {
					originDate = attributes.getValue(i);
				} else if (attributes.getLocalName(i).equalsIgnoreCase(
						"destTimeMin")) {
					destinationTime = attributes.getValue(i);
				} else if (attributes.getLocalName(i).equalsIgnoreCase(
						"destTimeDate")) {
					destinationDate = attributes.getValue(i);
				} else if (attributes.getLocalName(i).equalsIgnoreCase(
						"bikeFlag")) {
					trip.setBikesAllowed(attributes.getValue(i).equals("1"));
				} else if (attributes.getLocalName(i).equalsIgnoreCase(
						"trainHeadStation")) {
					trip.setTrainHeadStation(attributes.getValue(i));
				}
			}

			long departTime = parseDate(TRIP_DATE_FORMAT, originDate,
					originTime);
			if (departTime > 0)
				trip.setDepartureTime(departTime);

			long arriveTime = parseDate(TRIP_DATE_FORMAT, destinationDate,
					destinationTime);
			if (arriveTime > 0)
				trip.setArrivalTime(arriveTime);

			schedule.addTrip(trip);
		}
	}

	private long parseDate(DateFormat format, String dateString,
			String timeString) {
		if (dateString == null || timeString == null) {
			return -1;
		}
		try {
			return format.parse(dateString + " " + timeString).getTime();
		} catch (ParseException e) {
			Log.e(Constants.TAG, "Unable to parse datetime '" + dateString
					+ " " + timeString + "'", e);
			return -1;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (localName.equals("date")) {
			requestDate = currentValue;
		} else if (localName.equals("time")) {
			requestTime = currentValue;
		}
		isParsingTag = false;
		currentValue = null;
	}

	@Override
	public void endDocument() {
		long date = parseDate(REQUEST_DATE_FORMAT, requestDate, requestTime);
		if (date > 0) {
			schedule.setDate(date);
		}
	}
}
