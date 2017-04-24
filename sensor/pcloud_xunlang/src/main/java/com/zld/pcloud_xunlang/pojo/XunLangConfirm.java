package com.zld.pcloud_xunlang.pojo;

public class XunLangConfirm extends XunLangBase{
	public static final byte COMMAND = (byte)0xA1;
	private int length;
	private int data;
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public int getData() {
		return data;
	}
	public void setData(int data) {
		this.data = data;
	}
	public static XunLangConfirm getOkConfirm(XunLangBase base){
		XunLangConfirm c = new XunLangConfirm();
		c.setNid(base.getNid());
		c.setCommand(COMMAND);
		c.setLength(1);
		c.setData(0);
		return c;
	}
	public static XunLangConfirm getErrorConfirm(XunLangBase base){
		XunLangConfirm c = new XunLangConfirm();
		c.setNid(base.getNid());
		c.setLength(1);
		c.setData(1);
		return c;
	}
}
