package com.zldpark.pojo;

import java.io.Serializable;

public class StatsAccount implements Serializable {
	//ͳ�Ʒ���
	private long id = -1;//ͳ�Ʊ��
	//��Ŀͳ��
	private double parkingFee = 0;//ͣ���ѣ���Ԥ����
	private double prepayFee = 0;//Ԥ��ͣ����
	private double refundFee = 0;//Ԥ���˿Ԥ�����
	private double addFee = 0;//Ԥ�����ɣ�Ԥ�����㣩
	private double pursueFee = 0;//׷��ͣ����
	
	public double getParkingFee() {
		return parkingFee;
	}
	public void setParkingFee(double parkingFee) {
		this.parkingFee = parkingFee;
	}
	public double getPrepayFee() {
		return prepayFee;
	}
	public void setPrepayFee(double prepayFee) {
		this.prepayFee = prepayFee;
	}
	public double getRefundFee() {
		return refundFee;
	}
	public void setRefundFee(double refundFee) {
		this.refundFee = refundFee;
	}
	public double getAddFee() {
		return addFee;
	}
	public void setAddFee(double addFee) {
		this.addFee = addFee;
	}
	public double getPursueFee() {
		return pursueFee;
	}
	public void setPursueFee(double pursueFee) {
		this.pursueFee = pursueFee;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	@Override
	public String toString() {
		return "StatsAccount [id=" + id + ", parkingFee=" + parkingFee
				+ ", prepayFee=" + prepayFee + ", refundFee=" + refundFee
				+ ", addFee=" + addFee + ", pursueFee=" + pursueFee + "]";
	}
	
}
