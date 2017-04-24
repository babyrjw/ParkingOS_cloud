package com.zld.pcloud_xunlang.pojo;

import java.util.List;

public class SensorCar extends XunLangBase{
	private int year;
	private int month;
	private List<Dici> cars;
	private List<HeartBeat> heartBeats;
	
	
	
	
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public int getMonth() {
		return month;
	}
	public void setMonth(int month) {
		this.month = month;
	}
	public List<Dici> getCars() {
		return cars;
	}
	public void setCars(List<Dici> cars) {
		this.cars = cars;
	}
	public List<HeartBeat> getHeartBeats() {
		return heartBeats;
	}
	public void setHeartBeats(List<HeartBeat> heartBeats) {
		this.heartBeats = heartBeats;
	}
	
	
	
	



	@Override
	public String toString() {
		
		return super.toString() + " SensorCar [year=" + year + ", month=" + month + ", cars=" + cars + ", heartBeats=" + heartBeats + "]";
	}
	public static class Dici{
		//1 到達 0 離開
		private int status;
		private int diciId;
		private int flag;
		private int day;
		private int hour;
		private int minite;
		private int second;
		private String label;
		private String serial;
		private String lastSerial;
		private String retry;
		private String matchId;
		
		private int mx;
		private int my;
		private int mz;
		
		
		public int getFlag() {
			return flag;
		}
		public void setFlag(int flag) {
			this.flag = flag;
		}
		public int getMx() {
			return mx;
		}
		public void setMx(int mx) {
			this.mx = mx;
		}
		public int getMy() {
			return my;
		}
		public void setMy(int my) {
			this.my = my;
		}
		public int getMz() {
			return mz;
		}
		public void setMz(int mz) {
			this.mz = mz;
		}
		public int getStatus() {
			return status;
		}
		public void setStatus(int status) {
			this.status = status;
		}
		public int getDiciId() {
			return diciId;
		}
		public void setDiciId(int diciId) {
			this.diciId = diciId;
		}
		public int getDay() {
			return day;
		}
		public void setDay(int day) {
			this.day = day;
		}
		public int getHour() {
			return hour;
		}
		public void setHour(int hour) {
			this.hour = hour;
		}
		public int getMinite() {
			return minite;
		}
		public void setMinite(int minite) {
			this.minite = minite;
		}
		public int getSecond() {
			return second;
		}
		public void setSecond(int second) {
			this.second = second;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
	
		public String getSerial() {
			return serial;
		}
		public void setSerial(String serial) {
			this.serial = serial;
		}
		public String getLastSerial() {
			return lastSerial;
		}
		public void setLastSerial(String lastSerial) {
			this.lastSerial = lastSerial;
		}
		public String getRetry() {
			return retry;
		}
		public void setRetry(String retry) {
			this.retry = retry;
		}
		public String getMatchId() {
			return matchId;
		}
		public void setMatchId(String matchId) {
			this.matchId = matchId;
		}
		@Override
		public String toString() {
			return "Dici [status=" + status + ", diciId=" + diciId + ", flag=" + flag + ", day=" + day + ", hour="
					+ hour + ", minite=" + minite + ", second=" + second + ", label=" + label + ", serial=" + serial
					+ ", lastSerial=" + lastSerial + ", retry=" + retry + ", matchId=" + matchId + ", mx=" + mx
					+ ", my=" + my + ", mz=" + mz + "]";
		}
		
	}
	public static class HeartBeat{
		private int status;
		private int diciId;
		
		public int getStatus() {
			return status;
		}
		public void setStatus(int status) {
			this.status = status;
		}
		public int getDiciId() {
			return diciId;
		}
		public void setDiciId(int diciId) {
			this.diciId = diciId;
		}
		@Override
		public String toString() {
			return "HeartBeat [status=" + status + ", diciId=" + diciId
					+ "]";
		}
		
	}
}

