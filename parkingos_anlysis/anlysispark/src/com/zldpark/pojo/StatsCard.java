package com.zldpark.pojo;

import java.io.Serializable;

public class StatsCard extends StatsAccount implements Serializable {
	//��Ƭͳ��
	private double regFee = 0;//������ʼ�����
	private double chargeCashFee = 0;//��Ƭ��ֵ���
	private double returnFee = 0;//�˿��˻����
	private double actFee = 0;//���Ƭ��ʼ�����
	private long returnCount = 0;//�˿�����
	private long actCount = 0;//���Ƭ����
	private long regCount = 0;//��������
	private long bindCount = 0;//���û�����
	
	public double getRegFee() {
		return regFee;
	}
	public void setRegFee(double regFee) {
		this.regFee = regFee;
	}
	public double getChargeCashFee() {
		return chargeCashFee;
	}
	public void setChargeCashFee(double chargeCashFee) {
		this.chargeCashFee = chargeCashFee;
	}
	public double getReturnFee() {
		return returnFee;
	}
	public void setReturnFee(double returnFee) {
		this.returnFee = returnFee;
	}
	public double getActFee() {
		return actFee;
	}
	public void setActFee(double actFee) {
		this.actFee = actFee;
	}
	public long getReturnCount() {
		return returnCount;
	}
	public void setReturnCount(long returnCount) {
		this.returnCount = returnCount;
	}
	public long getActCount() {
		return actCount;
	}
	public void setActCount(long actCount) {
		this.actCount = actCount;
	}
	public long getRegCount() {
		return regCount;
	}
	public void setRegCount(long regCount) {
		this.regCount = regCount;
	}
	public long getBindCount() {
		return bindCount;
	}
	public void setBindCount(long bindCount) {
		this.bindCount = bindCount;
	}
	@Override
	public String toString() {
		return "StatsCard [regFee=" + regFee + ", chargeCashFee="
				+ chargeCashFee + ", returnFee=" + returnFee + ", actFee="
				+ actFee + ", returnCount=" + returnCount + ", actCount="
				+ actCount + ", regCount=" + regCount + ", bindCount="
				+ bindCount + "]";
	}
	
}
