package com.zldpark.pojo;

import java.io.Serializable;

public class StatsOrder implements Serializable {
	//ͳ�Ʒ���
	private long id = -1;//ͳ�Ʊ��
	//����ͳ��
	private double escapeFee = 0;//�ӵ�δ׷�ɶ������
	private double sensorFee = 0;//�������������
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public double getEscapeFee() {
		return escapeFee;
	}
	public void setEscapeFee(double escapeFee) {
		this.escapeFee = escapeFee;
	}
	public double getSensorFee() {
		return sensorFee;
	}
	public void setSensorFee(double sensorFee) {
		this.sensorFee = sensorFee;
	}
	@Override
	public String toString() {
		return "StatsOrder [id=" + id + ", escapeFee=" + escapeFee
				+ ", sensorFee=" + sensorFee + "]";
	}
}
