package com.zld.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;

public class TimeTools {

	private static Logger logger = Logger.getLogger(TimeTools.class);
	
	private static GregorianCalendar gCalendar = null;

	private static SimpleDateFormat dateFormat = null;

	public static Date getDateFromString(String str, String pattern) throws ParseException {
		return new SimpleDateFormat(pattern).parse(str);
	}

	// ʱ���ʽ����
	private static String[] formatArray = { "yyyy-MM-dd", "yyyy-MM-dd HH:mm",
			"yyyy-MM-dd HH:mm:ss", "yy-MM-dd HH:mm", "yyyyMMdd HH:mm", "yyyy-MM-dd HH" };

	// ���һ��ʱ���ʽ�Ƿ�Ϊ�Ϸ���ʽ
	private static boolean isRightFormat(String formatStr) {
		boolean isRight = false;
		int j = formatArray.length;
		for (int i = 0; i < j; i++) {
			if (formatArray[i].equalsIgnoreCase(formatStr)) {
				isRight = true;
				break;
			}
		}
		return isRight;
	}

	public static Long getLongMilliSecondFromStrDate(String strDate, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);

		long millSeconds = new GregorianCalendar().getTimeInMillis();
		try {
			millSeconds = sdf.parse(strDate).getTime();
		} catch (Exception e) {
			// logger.error("---------get seconds error:"+e.getMessage());
		}
		return new Long(millSeconds);
	}

	/**
	 * @param strDate
	 * @return �����ַ���ʱ��õ���Ӧ������
	 */
	public static Long getLongMilliSecondFrom_HHMMDD(String strDate) {
		return getLongMilliSecondFromStrDate(strDate, "yyyy-MM-dd");
	}

	/**
	 * @param strDate
	 * @return �����ַ���ʱ��õ���Ӧ����
	 */
	public static Long getLongMilliSecondFrom_HHMMDDHHmmss(String strDate) {
		return getLongMilliSecondFromStrDate(strDate, "yyyy-MM-dd HH:mm:ss") / 1000;
	}

	public static String checkMounth(String endDateSelect) {
		String year = endDateSelect.split("-")[0];
		String mounth = endDateSelect.split("-")[1];
		if (mounth.equals("1") || mounth.equals("3") || mounth.equals("5") || mounth.equals("7")
				|| mounth.equals("8") || mounth.equals("10") || mounth.equals("12"))
			return "-31";
		else if (mounth.equals("4") || mounth.equals("6") || mounth.equals("9")
				|| mounth.equals("11"))
			return "-30";
		else {
			Integer yInteger = Integer.parseInt(year);
			// if(year.equals("2012")||year.equals("2016")||year.equals("2020")||
			// year.equals("2024")||year.equals("2028")||year.equals("2008"))
			if ((yInteger % 4 == 0 && yInteger % 100 != 0) || yInteger % 400 == 0)// �����ж�
				return "-29";
			else
				return "-28";
		}

	}

	/**
	 * @return �õ���ǰʱ�������(long��)
	 */
	public static long getlongMilliSeconds() {
		return new java.util.Date().getTime();
	}

	/**
	 * @return �õ���ǰʱ��ĺ�����(Long��)
	 */
	public static Long getLongMilliSeconds() {
		long d = new java.util.Date().getTime();
		return new Long(d / 1000);
	}

	public static Long getLongSeconds() {
		long d = new java.util.Date().getTime();
		return new Long(d);
	}

	/**
	 * @param milliSeconds
	 *            ������
	 * @return ��ʽ�����ʱ���ַ��� yyyy-MM-dd HH:mm:ss
	 */
	public static String getTime_yyyyMMdd_HHmmss(Long milliSeconds) {

		return secondsToDateStr(milliSeconds, "yyyy-MM-dd HH:mm:ss");
	}
	/**
	 * @param milliSeconds
	 *            ������
	 * @return ��ʽ�����ʱ���ַ��� yyyy-MM-dd HH:mm:ss
	 */
	public static String getTime_MMdd_HHmm(Long milliSeconds) {

		return secondsToDateStr(milliSeconds, "yyyy-MM-dd HH:mm").substring(5);
	}

	/**
	 * @param milliSeconds
	 *            ������
	 * @return ��ʽ�����ʱ���ַ��� yyyy-MM-dd HH:mm:ss
	 */
	public static String getTime_yyyyMMdd_HHmm(Long milliSeconds) {
		if(milliSeconds==null) return "";
		return secondsToDateStr(milliSeconds, "yyyy-MM-dd HH:mm");
	}

	/**
	 * @param milliSeconds
	 *            ������
	 * @return ��ʽ�����ʱ���ַ��� yy-MM-dd HH:mm
	 */
	public static String getTime_yyMMdd_HHmm(Long milliSeconds) {

		return secondsToDateStr(milliSeconds, "yy-MM-dd HH:mm");
	}
	
	public static String getTime_yyyyMMdd_HH(Long milliSeconds) {
		return secondsToDateStr(milliSeconds, "yyyy-MM-dd HH");
	}
	
	/**
	 * @param milliSeconds
	 *            ������
	 * @return ��ʽ�����ʱ���ַ��� yyyy-MM-dd
	 */
	public static String getTimeStr_yyyy_MM_dd(Long milliSeconds) {

		return secondsToDateStr(milliSeconds, "yyyy-MM-dd");
	}

	/**
	 * @return ��ǰ���ڵ��ַ��� yyyy-MM-dd ��ʽ
	 */
	public static String getDate_YY_MM_DD() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.format(new java.util.Date());

	}

	/**
	 * @return ��ǰ���ڵ��ַ��� yyyy/M/d ��ʽ
	 */
	public static String getDate_YY_M_D() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/M/d");
		return dateFormat.format(new java.util.Date());

	}

	public static Date str2Date(String date) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date ddate = new Date();
		try {
			ddate = df.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return ddate;
	}

	/**
	 * @todo ����ֵʱ���ʽ��Ϊ�ַ���
	 * @param milliSeconds
	 * @param formatStr
	 * @return
	 */
	public static String secondsToDateStr(Long milliSeconds, String formatStr) {

		if (milliSeconds == null)
			return "";
		if (isRightFormat(formatStr) == false) {
			formatStr = "yyyy-MM-dd HH:mm:ss";
		}
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat(formatStr);

			if (milliSeconds.longValue() > 1) {
				GregorianCalendar gCalendar = new GregorianCalendar();
				gCalendar.setTimeInMillis(milliSeconds.longValue());
				return dateFormat.format(gCalendar.getTime());
			} else {
				return "";
			}
		} catch (Exception e) {
			return "";
		}

	}
 
	// �õ�����ʱ��
	public static String getTomorrowday() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, +1);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String mDateTime = formatter.format(c.getTime());
		String strStart = mDateTime.substring(0, 19);//
		return strStart;
	}

	public static String getTwoLaterday() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, +2);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String mDateTime = formatter.format(c.getTime());
		String strStart = mDateTime.substring(0, 19);//
		return strStart;
	}

	public static String getThirdLaterday() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, +3);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String mDateTime = formatter.format(c.getTime());
		String strStart = mDateTime.substring(0, 19);//
		return strStart;
	}

	public static String getForthLaterday() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, +4);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String mDateTime = formatter.format(c.getTime());
		String strStart = mDateTime.substring(0, 19);//
		return strStart;
	}

	public static String getFiveLaterday() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, +5);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String mDateTime = formatter.format(c.getTime());
		String strStart = mDateTime.substring(0, 19);//
		return strStart;
	}

	public static String getSixLaterday() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, +6);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String mDateTime = formatter.format(c.getTime());
		String strStart = mDateTime.substring(0, 19);//
		return strStart;
	}

	public static String getSevenLaterday() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, +7);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String mDateTime = formatter.format(c.getTime());
		String strStart = mDateTime.substring(0, 19);//
		return strStart;
	}

	public static String getCoutomday(int days) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, +days);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String mDateTime = formatter.format(c.getTime());
		String strStart = mDateTime.substring(0, 10);//
		return strStart;
	}

	public static Long getStrDateToSecond(String strDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long millSeconds = (new GregorianCalendar()).getTimeInMillis();
		try {
			millSeconds = sdf.parse(strDate).getTime();
		} catch (Exception e) {

		}
		return new Long(millSeconds / 1000);
	}

	public static Long getStrDateToSecond2(String strDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long millSeconds = (new GregorianCalendar()).getTimeInMillis();
		try {
			millSeconds = sdf.parse(strDate).getTime();
		} catch (Exception e) {

		}
		return new Long(millSeconds);
	}

	// ת��
	public static String secondsToDateStr(Long seconds) {

		String second = "1";
		if (seconds.equals("") && seconds == null) {
			seconds = new Long(1);
		}
		try {

			dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			gCalendar = new GregorianCalendar();
			gCalendar.setTimeInMillis(seconds.longValue() * 1000);
			second = dateFormat.format(gCalendar.getTime());
		} catch (Exception e) {
			second = "1";

		}
		return second;
	}

	public static String MillsecondsToDateStr(Long seconds) {

		String second = "1";
		if (seconds.equals("") && seconds == null) {
			seconds = new Long(1);
		}
		try {

			dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			gCalendar = new GregorianCalendar();
			gCalendar.setTimeInMillis(seconds.longValue());
			second = dateFormat.format(gCalendar.getTime());
		} catch (Exception e) {
			second = "1";

		}
		return second;
	}

	public static Long getDatestart() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, -2);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String mDateTime = formatter.format(c.getTime());
		String strStart = mDateTime.substring(0, 19);//
		Long State = getLongMilliSecondFrom_HHMMDD(strStart);
		return State;
	}

	public static Long getDateend() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, +2);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String mDateTime = formatter.format(c.getTime());
		String strStart = mDateTime.substring(0, 19);//
		Long State = getLongMilliSecondFrom_HHMMDD(strStart);
		return State;
	}

	public static Long getToDayBeginTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String today = sdf.format(new Date());
		today = today.substring(0, 10) + " 00:00:00";
		return getStrDateToSecond(today);
	}
	
	/*
	 * ��ô������ڵ������ֵ
	 * miliseconds������
	 */
    public static Long getBeginTime(Long miliseconds){
    	Date date=new Date(miliseconds);
		SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time=formatter.format(date);
		String day = time.substring(0,10)+ " 00:00:00";
    	return getStrDateToSecond(day);
    }
    
	/*
	 * ��ô������ڵ������ֵ
	 * seconds������
	 */
    public static Long getBeginTimeBySec(Long second){
    	Date date=new Date(second*1000);
		SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time=formatter.format(date);
		String day = time.substring(0,10)+ " 00:00:00";
    	return getStrDateToSecond(day);
    }

	/**
	 * ����ʱ��
	 * 
	 * @param seconds
	 *            ���룩
	 * @return HH:mm:ss exp��"00:00:00"
	 */
	public static String getShiChangString(Long seconds) {
		StringBuffer shichang = null;
		if (seconds != null) {
			if (seconds > 0) {
				shichang = new StringBuffer("");
				int hour = (int) (seconds / 3600);
				if (hour > 0) {
					if (hour > 9)
						shichang.append(hour + ":");
					else
						shichang.append("0" + hour + ":");
				} else {
					shichang.append("00:");
				}
				int minute = (int) (seconds % 3600) / 60;
				if (minute > 0) {
					if (minute > 9)
						shichang.append(minute + ":");
					else
						shichang.append("0" + minute + ":");
				} else {
					shichang.append("00:");
				}
				int second = (int) (seconds % 3600) % 60;
				if (second > 0) {
					if (second > 9)
						shichang.append(second);
					else
						shichang.append("0" + second);
				} else {
					shichang.append("00");
				}
			} else {
				return "00:00:00";
			}
		} else {
			return "";
		}
		return shichang.toString();
	}

	/*
	 * ����¼�����ص�ʱ���ʽ��2013-03-13_145012
	 */
	public static String getRecordTime(long time) {
		String datestr = secondsToDateStr(time);
		datestr = datestr.replace(" ", "_").replaceAll(":", "");
		return datestr;
	}

	private static String dtime = "";

	public static String getdate1()// ��þ�ȷ���յĵ�ǰ����
	{
		dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		gCalendar = new GregorianCalendar();
		dtime = dateFormat.format(gCalendar.getTime());
		return dtime;
	}

	public static String gettime1()// ��þ�ȷ����ĵ�ǰ����
	{
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		gCalendar = new GregorianCalendar();
		dtime = dateFormat.format(gCalendar.getTime());
		return dtime;
	}

	public static long getlongtime()// ��õ�ǰʱ��ĺ�����
	{
		java.util.Date nows = new java.util.Date();
		long d = 0;
		d = nows.getTime();
		return d;
	}

	public static long getSeconds() {
		return new Long((new GregorianCalendar().getTimeInMillis()) / 1000);
	}

	public static String getdate()// ��þ�ȷ���յĵ�ǰ���� ����oracle
	{
		dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		gCalendar = new GregorianCalendar();
		dtime = dateFormat.format(gCalendar.getTime());
		return dtime;
	}

	public static String gettime()// ��þ�ȷ����ĵ�ǰ���� ����oracle
	{
		try {
			dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			gCalendar = new GregorianCalendar();
			dtime = dateFormat.format(gCalendar.getTime());
		} catch (Exception ex) {

		}
		return dtime;
	}

	/*
	 * @author: yangzi
	 * @fun : format the date
	 */
	public static String dateFormat(Date myDate) {
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		gCalendar = new GregorianCalendar();
		dtime = dateFormat.format(myDate);
		return dtime;

	}

	/**
	 * @author : yangzi
	 * @function :�õ���ȷ�����ӵ�ʱ��
	 * @param myDate
	 * @return
	 * @date: 2006-9-22
	 */
	public static String dateFormat(Date myDate, String strFormat) {
		dateFormat = new SimpleDateFormat(strFormat);
		gCalendar = new GregorianCalendar();
		dtime = dateFormat.format(myDate);
		return dtime;

	}

	public static String dateFormat() {
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		gCalendar = new GregorianCalendar();
		dtime = dateFormat.format(gCalendar.getTime());
		return dtime;

	}

	/**
	 * ���ܣ�ָ�����ڵĻ������������ڣ��ꡢ�¡��ա�ʱ���֡��룩
	 * 
	 * @param millis
	 *            //ָ������
	 * @param amount
	 *            //����������������
	 * @param field
	 *            //�ꡢ�¡��ա�ʱ���֡���
	 * @return long //���غ��뼶
	 */
	public static long getMillis(long millis, int amount, int field) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(millis);
		switch (field) {
		case 10:// ����
			cal.add(Calendar.YEAR, amount);
			break;
		case 20:// ����
			cal.add(Calendar.MONTH, amount);
			break;
		case 30:// ����
			cal.add(Calendar.DATE, amount);
			break;
		case 40:// ��ʱ
			cal.add(Calendar.HOUR, amount);
			break;
		case 50:// �ӷ�
			cal.add(Calendar.MINUTE, amount);
			break;
		case 60:// ����
			cal.add(Calendar.SECOND, amount);
		default:// Ĭ�ϼ���
			cal.add(Calendar.DATE, amount);
		}
		return cal.getTime().getTime();
	}

	public static boolean compareStringTime(String begindateStr, String enddateStr) {
		if (begindateStr == null || begindateStr.equals(""))
			return false;
		if (enddateStr == null || enddateStr.equals(""))
			return false;
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
		Date dateA = null;
		Date dateB = null;
		try {
			dateA = sdf1.parse(begindateStr);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			dateB = sdf1.parse(enddateStr);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (dateA.before(dateB)) {
			// begindateStr С�� enddateStr ����false
			return false;
		} else {
			// begindateStr ���� enddateStr ����true
			return true;
		}
	}

	public static String formatMs(long ms, int flag) {// �������������x��xʱx��x��x����
		int ss = 1000;
		int mi = ss * 60;
		int hh = mi * 60;
		int dd = hh * 24;

		long day = 0;
		long hour = 0;
		long minute1 = 0;
		long minute2 = 0;
		long second = 0;
		switch (flag) {
		case 0:// ������תΪ��
			minute1 = (ms / ss) / 60;
			minute2 = (ms / ss) % 60;
			return minute1 + "��" + minute2 + "��";
		default:
			break;
		}

		return "";
	}

	public static String getDateStrC(Date date) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy��MM��dd��");
		return format.format(date);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	public static long dateFormat(String isFlag) {
		dateFormat = new SimpleDateFormat("yyMMddHHmm");
		gCalendar = new GregorianCalendar();
		dtime = dateFormat.format(gCalendar.getTime());
		return (new Long(dtime)).longValue();
	}

	// ***************************************************************************

	/**
	* @author yangzi
	* @version 2007-5-26 ����04:00:27
	* @todo ���ַ���ʱ��ת���ڣ�date��
	* @param strDate
	* @return  
	*/
	public static Date getDateFromStr(String strDate){
		  SimpleDateFormat   sdf   =   new   SimpleDateFormat("yyyy-MM-dd");   
		    
		 // Calendar   calendar   =   new   GregorianCalendar();  
		  Date   date = null;
		  try{
			  date   =   sdf.parse(strDate);   
		  }catch(Exception e){
			  
		  }
		  
		  return date;
	}
	
	
	/**
	* @author yangzi
	* @version 2007-5-28 ����11:57:41
	* @todo �����ַ���ʱ�䷵������
	* @param strDate
	* @param format
	* @return  
	*/
	public static Long getStrDateToSecond(String strDate,String format){ 
		  SimpleDateFormat   sdf   =   new   SimpleDateFormat(format); 
		 
		  long millSeconds = new GregorianCalendar().getTimeInMillis();
		  try{			  
			  millSeconds =sdf.parse(strDate).getTime();
		  }catch(Exception e){
			  logger.error("---------get seconds error:"+e.getMessage());
		  }		  
		  return new Long(millSeconds/1000);
	}
	
	
	/**
	* @author yangzi
	* @version 2007-5-26 ����04:04:35
	* @todo �������ַ���ʱ���Ӧ������
	* @param strDate
	* @return  
	*/
	public static Long getSecondsFromStrDate(String strDate){
		
		Long seconds =null;
		try{
			Date   date =getDateFromStr( strDate);
			
			 seconds = new Long(date.getTime()/1000);
		}catch(Exception e){
			
		}

		return seconds ;
		
	}
	
	
	

	/**
	 * @author yangzi
	 * @version 2007-5-20 ����02:55:48
	 * @todo ���ص��� ĳʱ�̵�����
	 * @param hh
	 *            Сʱ
	 * @param mm
	 *            ����
	 * @param ss
	 *            ��
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static Long getLongSecond(int hh, int mm, int ss) {
		Date rightNow = new Date();

		if (hh > 0 && hh < 23)
			rightNow.setHours(hh);
		if (mm > 0 && mm < 60)
			rightNow.setMinutes(mm);
		if (ss > 0 && ss < 60)
			rightNow.setSeconds(ss);

		return new Long(rightNow.getTime() / 1000);
	}

	/**
	 * @todo ���ص�ǰʱ�� yyyy-MM-dd HH:mm:ss
	 * @return 2007-4-12
	 */
	// public static Date getRightDate()//��þ�ȷ����ĵ�ǰ����
	// {
	// // dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	// // gCalendar=new GregorianCalendar();
	// // dtime=dateFormat.format(gCalendar.getTime());
	// // System.out.print(new Date(dtime));
	// return new Date();
	// }


	/**
	 * @return ���꿪ʼһ������� 2007-4-12
	 */
	@SuppressWarnings("deprecation")
	public static long getYearStartSeconds() {
		// Calendar rightNow = Calendar.getInstance();
		Date rightNow = new Date();
		rightNow.setMonth(0);
		rightNow.setDate(1);
		rightNow.setHours(0);
		rightNow.setMinutes(0);
		rightNow.setSeconds(0);
		// System.out.print(rightNow);
		return rightNow.getTime() / 1000;
	}

	/**
	 * @���ص�ǰ�µ�һ���1970-01-01 00:00:00 ������ 2007-4-12
	 */
	@SuppressWarnings("deprecation")
	public static long getMonthStartSeconds() {
		// Calendar rightNow = Calendar.getInstance();
		Date rightNow = new Date();
		rightNow.setDate(1);
		rightNow.setHours(0);
		rightNow.setMinutes(0);
		rightNow.setSeconds(0);
		// System.out.println(rightNow);
		// System.out.println(rightNow.getTime()/1000);
		return rightNow.getTime() / 1000;
	}
	public static void main(String[] args) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		cal.setTimeInMillis(System.currentTimeMillis());
		System.err.println(cal.get(Calendar.HOUR_OF_DAY));
	}
	/**
	 * @���ص����µ�һ���1970-01-01 00:00:00 ������ 2007-4-12
	 */
	@SuppressWarnings("deprecation")
	public static long getLastMonthStartSeconds() {
		Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		rightNow.setTimeInMillis(getMonthStartSeconds()*1000);
		rightNow.set(Calendar.MONTH, rightNow.get(Calendar.MONTH)-1);
		return rightNow.getTimeInMillis()/1000;
	}
	/**
	 * @return ����һ������ 2007-4-12
	 */
	@SuppressWarnings("deprecation")
	public static long getWeekStartSeconds() {
		Date rightNow = new Date();
		rightNow.setHours(0);
		rightNow.setMinutes(0);
		rightNow.setSeconds(0);

		int days = rightNow.getDay();
		if (days == 0)
			days = 7;
		long reValue = rightNow.getTime() - (days - 1) * 24 * 60 * 60 * 1000;

		// System.out.println(reValue);
		// System.out.println(new Date(reValue));
		// System.out.println("1176048000");
		// System.out.println(reValue/1000-1176048000);
		// System.out.println(24*60*60*7);
		return reValue / 1000;
	}
	public static String getTimeYYYYMMDDHHMMSS()
	{
		String s = gettime();
		s = s.replaceAll("-", "").replaceAll(":", "").replaceAll(" ","").trim();
		return s;
	}
	/**
	 * ���ض����볡���볡ʱ��,��ȷ������
	 * @param btime �볡ʱ��
	 * @return
	 */
	public static Long getOrderTime(Long btime){
		//Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		Long ntime = System.currentTimeMillis()/1000;
		/*if(btime!=null){//�볡ʱ���������ʱ�����볡ʱ��С��60�룬����ʱ�䣬�����ж����Ƿ����30�룬�Ǿͼ�һ����
			if(ntime-btime<100){
				return btime;
			}else {
				calendar.setTimeInMillis(ntime*1000);
				if(calendar.get(Calendar.SECOND)>30)
					calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE)+1);
				calendar.set(Calendar.SECOND, 0);
			}
		}else {//�볡ʱ������>30ʱ��ʱ1����
//			if(calendar.get(Calendar.SECOND)>30)
			calendar.setTimeInMillis(ntime*1000);
//			if(calendar.get(Calendar.SECOND)>30)
//				calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE)+1);
			calendar.set(Calendar.SECOND, 0);
		}
		ntime = calendar.getTimeInMillis()/1000;*/
		return ntime;
	}
	

}
