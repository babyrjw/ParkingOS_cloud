package com.zldpark.pojo;

import java.io.Serializable;
import java.util.List;

public class AccountReq implements Serializable {
	private long startTime = -1;//��ѯ��ʼʱ��
	private long endTime = -1;//��ѯ����ʱ��
	private long id;
	private int type;//0�����շ�Ա���ͳ�� 1�����������ͳ�� 2������λ�α�Ų�ѯ 3������λ��ѯ 4:����Ӫ���Ų�ѯ
	private int pageNum = -1;
	private int pageSize = -1;
	
	public int getPageNum() {
		return pageNum;
	}
	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}
	public int getPageSize() {
		return pageSize;
	}
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
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
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	@Override
	public String toString() {
		return "AccountReq [startTime=" + startTime + ", endTime=" + endTime
				+ ", id=" + id + ", type=" + type + ", pageNum=" + pageNum
				+ ", pageSize=" + pageSize + "]";
	}
}
