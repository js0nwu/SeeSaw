package edu.gatech.ubicomp.synchro.offlinetester;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by gareyes on 3/27/17.
 */

public class DateTester {

	public static void main(String[] args) {
		long time = System.currentTimeMillis();
		Date date = new Date(time);
		DateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		String dateFormatted = formatter.format(date);
		System.out.println(dateFormatted);
	}
}
