package com.zldpark.pojo;

import java.io.Serializable;
import java.util.List;

public class StatsReq implements Serializable {
	private long startTime = -1;//��ѯ��ʼʱ��
	private long endTime = -1;//��ѯ����ʱ��
	private List<Object> idList;
	private int type;//0�����շ�Ա���ͳ�� 1�����������ͳ�� 2������λ�α�Ų�ѯ 3������λ��ѯ
	
	public List<Object> getIdList() {
		return idList;
	}
	public void setIdList(List<Object> idList) {
		this.idList = idList;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	@Override
	public String toString() {
		return "StatsReq [startTime=" + startTime + ", endTime=" + endTime
				+ ", idList=" + idList + ", type=" + type + "]";
	}
	
}
