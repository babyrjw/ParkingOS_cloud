package com.zld.pcloud_xunlang.pojo;

public class XunLangBase {
	private String nid;
	private byte command;
	private byte sign;
	public String getNid() {
		return nid;
	}
	public void setNid(String nid) {
		this.nid = nid;
	}
	public byte getCommand() {
		return command;
	}
	public void setCommand(byte command) {
		this.command = command;
	}
	public byte getSign() {
		return sign;
	}
	public void setSign(byte sign) {
		this.sign = sign;
	}
	@Override
	public String toString() {
		return "XunLangBase [nid=" + nid + ", command=" + command + ", sign=" + sign + "]";
	}
}
