package com.zldpark.pojo;

import java.io.Serializable;

public class WorkRecord implements Serializable {
	private Long id;
	private Long start_time;//�ϰ࿪ʼʱ��
	private Long end_time;//�°�ʱ��
	private Long worksite_id = -1L;//����վ���
	private Long uid = -1L;//�շ�Ա���
	private Long berthsec_id = -1L;//��λ�α��
	private String device_code;//�豸��
	private Integer state = 0;//0��ǩ��  1��ǩ��
	private Double history_money = 0d;//�ϸ�ʱ����λ���ϵ���Ԥ�ս��
	private String out_log;//ǩ��СƱ����
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getStart_time() {
		return start_time;
	}
	public void setStart_time(Long start_time) {
		this.start_time = start_time;
	}
	public Long getEnd_time() {
		return end_time;
	}
	public void setEnd_time(Long end_time) {
		this.end_time = end_time;
	}
	public Long getWorksite_id() {
		return worksite_id;
	}
	public void setWorksite_id(Long worksite_id) {
		if(worksite_id == null)
			worksite_id = -1L;
		this.worksite_id = worksite_id;
	}
	public Long getUid() {
		return uid;
	}
	public void setUid(Long uid) {
		if(uid == null)
			uid = -1L;
		this.uid = uid;
	}
	public Long getBerthsec_id() {
		return berthsec_id;
	}
	public void setBerthsec_id(Long berthsec_id) {
		if(berthsec_id == null)
			berthsec_id = -1L;
		this.berthsec_id = berthsec_id;
	}
	public String getDevice_code() {
		return device_code;
	}
	public void setDevice_code(String device_code) {
		this.device_code = device_code;
	}
	public Integer getState() {
		return state;
	}
	public void setState(Integer state) {
		if(state == null)
			state = 0;
		this.state = state;
	}
	public Double getHistory_money() {
		return history_money;
	}
	public void setHistory_money(Double history_money) {
		if(history_money == null)
			history_money = 0d;
		this.history_money = history_money;
	}
	public String getOut_log() {
		return out_log;
	}
	public void setOut_log(String out_log) {
		this.out_log = out_log;
	}
	
}
