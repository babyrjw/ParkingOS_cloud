package com.zld.pcloud_xunlang.pojo;

import java.util.List;

import com.zld.pcloud_xunlang.Constants;

public class XunLangAttach{
	public static final byte COMMAND = Constants.UPLOAD_ATTACH;
	
	private List<? extends Attach> attaches;
	
	public List<? extends Attach> getAttaches() {
		return attaches;
	}
	public void setAttaches(List<? extends Attach> attaches) {
		this.attaches = attaches;
	}
	
	
	
	@Override
	public String toString() {
		return "XunLangAttach [attaches=" + attaches + "]";
	}



	public static class Attach{
		private int type;

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return "Attach [type=" + type + "]";
		}
		
	}
	public static class DiciAttach extends Attach{
		private int status;
		private int diciId;
		private int voltageBattery;
		private int voltageCapacity;
		
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
		public int getVoltageBattery() {
			return voltageBattery;
		}
		public void setVoltageBattery(int voltageBattery) {
			this.voltageBattery = voltageBattery;
		}
		public int getVoltageCapacity() {
			return voltageCapacity;
		}
		public void setVoltageCapacity(int voltageCapacity) {
			this.voltageCapacity = voltageCapacity;
		}
		@Override
		public String toString() {
			return super.toString()+ " DiciAttach [status=" + status + ", diciId=" + diciId + ", voltageBattery=" + voltageBattery
					+ ", voltageCapacity=" + voltageCapacity + "]";
		}
		
		
	}
	public static class TranAttach extends Attach{
		private int voltage;

		public int getVoltage() {
			return voltage;
		}

		public void setVoltage(int voltage) {
			this.voltage = voltage;
		}

		@Override
		public String toString() {
			return super.toString()+" TranAttach [voltage=" + voltage + "]";
		}
		
	}
}
