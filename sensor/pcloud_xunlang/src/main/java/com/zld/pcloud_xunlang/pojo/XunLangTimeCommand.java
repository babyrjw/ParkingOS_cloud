package com.zld.pcloud_xunlang.pojo;

import java.util.Date;

public class XunLangTimeCommand extends XunLangBase{
	public static final byte COMMAND = (byte)0xA9;
	//数据包内容长度
	private Date date;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	public static XunLangTimeCommand getNowTimeCommand(XunLangBase base){
		XunLangTimeCommand c = new XunLangTimeCommand();
		c.setNid(base.getNid());
		c.setCommand(COMMAND);
		c.setDate(new Date());
		return c;
	}
}
