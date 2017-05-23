package com.zldpark.pojo;

import java.io.Serializable;

public class ParkEpayAccount implements Serializable {
	private Long id = -1L;
	private Long comid = -1L;//�������
	private Double amount = 0d;//���
	private Integer type = 0;//0��ֵ 1���֣�2���֣��ѷ�����
	private Long create_time;//��¼ʱ��
	private String remark;//˵��
	private Long uid = -1L;//�շ�Ա�˻�
	private Integer source = 0;//��Դ��0��ͣ���ѣ���Ԥ������1�����֣�2�������ѣ�3���Ƽ�����4�������ֽ��ѷ�������5���������֣�6��ͣ�������а��ܽ���7��׷��ͣ���ѣ�8������Ԥ��ͣ���ѣ�9��Ԥ���˿Ԥ�������10��Ԥ�����ɣ�Ԥ�����㣩 11�������˿�
	private Long orderid = -1L;//�������
	private Long withdraw_id = -1L;//���ּ�¼���
	private Long berthseg_id = -1L;//���������ˮ���ڵĲ�λ�α��
	private Long berth_id = -1L;//���������ˮ���ڵĲ�λ���
	private Long groupid = -1L;//��Ӫ�����˺�
	private Integer is_delete = 0;//��Ŀ��ˮ״̬ 0������ 1��ɾ��
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getComid() {
		return comid;
	}
	public void setComid(Long comid) {
		if(comid == null)
			comid = -1L;
		this.comid = comid;
	}
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		if(amount == null)
			amount = 0d;
		this.amount = amount;
	}
	public Integer getType() {
		return type;
	}
	public void setType(Integer type) {
		this.type = type;
	}
	public Long getCreate_time() {
		return create_time;
	}
	public void setCreate_time(Long create_time) {
		this.create_time = create_time;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	public Long getUid() {
		return uid;
	}
	public void setUid(Long uid) {
		if(uid == null)
			uid = -1L;
		this.uid = uid;
	}
	public Integer getSource() {
		return source;
	}
	public void setSource(Integer source) {
		this.source = source;
	}
	public Long getOrderid() {
		return orderid;
	}
	public void setOrderid(Long orderid) {
		if(orderid == null)
			orderid = -1L;
		this.orderid = orderid;
	}
	public Long getWithdraw_id() {
		return withdraw_id;
	}
	public void setWithdraw_id(Long withdraw_id) {
		if(withdraw_id == null)
			withdraw_id = -1L;
		this.withdraw_id = withdraw_id;
	}
	public Long getBerthseg_id() {
		return berthseg_id;
	}
	public void setBerthseg_id(Long berthseg_id) {
		if(berthseg_id == null)
			berthseg_id = -1L;
		this.berthseg_id = berthseg_id;
	}
	public Long getBerth_id() {
		return berth_id;
	}
	public void setBerth_id(Long berth_id) {
		if(berth_id == null)
			berth_id = -1L;
		this.berth_id = berth_id;
	}
	public Long getGroupid() {
		return groupid;
	}
	public void setGroupid(Long groupid) {
		if(groupid == null)
			groupid = -1L;
		this.groupid = groupid;
	}
	public Integer getIs_delete() {
		return is_delete;
	}
	public void setIs_delete(Integer is_delete) {
		this.is_delete = is_delete;
	}
	
	
}
