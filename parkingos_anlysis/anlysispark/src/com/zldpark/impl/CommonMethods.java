package com.zldpark.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.zldpark.service.PgOnlyReadService;
import com.zldpark.utils.SqlInfo;


@Repository
public class CommonMethods {
	private Logger logger = Logger.getLogger(CommonMethods.class);
	@Autowired
	private PgOnlyReadService pgOnlyReadService;
	
	public List<Map<String, Object>> getIncome(Long startTime, Long endTime, List<Object> comidList, List<Object> uinList, Map<String, Object> otherMap){
		try {
			List<Object> params = new ArrayList<Object>();
			String preParams = "";
			String sql = "";
			String groupSql = "";
			if(comidList != null && !comidList.isEmpty()){
				for(Object o : comidList){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				sql = " o.comid in ("+preParams+")";
				groupSql = ",o.comid ";
				params.addAll(comidList);
			}else if(uinList != null && !uinList.isEmpty()){
				for(Object o : uinList){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				sql = " a.uin in ("+preParams+")";
				groupSql = ",a.uin";
				params.addAll(uinList);
			}
			List<Object> params1 = new ArrayList<Object>();
			params1.addAll(params);
			params1.add(0);//ͣ���ѣ���Ԥ��
			params1.add(1);//Ԥ��ͣ����
			params1.add(2);//Ԥ���˿Ԥ�����
			params1.add(3);//Ԥ�����ɣ�Ԥ�����㣩
			params1.add(4);//׷��ͣ����
			SqlInfo sqlInfo1 = new SqlInfo(sql + " and a.target in (?,?,?,?,?)", params1);
			List<Map<String, Object>> list1 = anlysisMoney(1, startTime, endTime, new String[]{"a.target" + groupSql}, sqlInfo1, null);
			params1.clear();
			params1.addAll(params);
			params1.add(4);//������ͣ���ѣ���Ԥ�������ߴ����շ�Ա
			params1.add(5);//׷��ͣ����
			params1.add(6);//����Ԥ��ͣ����
			params1.add(7);//Ԥ���˿Ԥ�����
			params1.add(8);//Ԥ�����ɣ�Ԥ�����㣩
			SqlInfo sqlInfo2 = new SqlInfo(sql + " and a.target in (?,?,?,?,?)", params1);
			List<Map<String, Object>> list2 = anlysisMoney(2, startTime, endTime, new String[]{"a.target" + groupSql}, sqlInfo2, null);
			params1.clear();
			params1.addAll(params);
			params1.add(0);//ͣ���ѣ���Ԥ����
			params1.add(7);//׷��ͣ����
			params1.add(8);//����Ԥ��ͣ����
			params1.add(9);//Ԥ���˿Ԥ�����
			params1.add(10);//Ԥ�����ɣ�Ԥ�����㣩
			String sql2 = sql;
			String groupSql2 = groupSql;
			if(sql.contains("a.uin")){
				sql2 = sql.replace("a.uin", "a.uid");
			}
			if(groupSql.contains("a.uin")){
				groupSql2 = groupSql.replace("a.uin", "a.uid");
			}
			SqlInfo sqlInfo3 = new SqlInfo(sql2 + " and a.source in (?,?,?,?,?)", params1);
			List<Map<String, Object>> list3 = anlysisMoney(3, startTime, endTime, new String[]{"a.source" + groupSql2}, sqlInfo3, null);
			params1.clear();
			params1.addAll(params);
			params1.add(0);//ͣ���ѣ���Ԥ����
			params1.add(2);//׷��ͣ����
			params1.add(3);//Ԥ��ͣ����
			params1.add(4);//Ԥ���˿Ԥ����
			params1.add(5);//Ԥ�����ɣ�Ԥ�����㣩
			SqlInfo sqlInfo4 = new SqlInfo(sql2 + " and a.source in (?,?,?,?,?)", params1);
			List<Map<String, Object>> list4 = anlysisMoney(4, startTime, endTime, new String[]{"a.source" + groupSql2}, sqlInfo4, null);
			params1.clear();
			params1.addAll(params);
			params1.add(0);//ͣ���ѣ���Ԥ����
			params1.add(2);//׷��ͣ����
			params1.add(3);//Ԥ��ͣ����
			params1.add(4);//Ԥ���˿Ԥ����
			params1.add(5);//Ԥ�����ɣ�Ԥ�����㣩
			SqlInfo sqlInfo5 = new SqlInfo(sql2 + " and a.source in (?,?,?,?,?)", params1);
			List<Map<String, Object>> list5 = anlysisMoney(5, startTime, endTime, new String[]{"a.source" + groupSql2}, sqlInfo5, null);
			params1.clear();
			params1.addAll(params);
			String sql3 = sql;
			String groupSql3 = groupSql;
			if(sql.contains("a.uin")){
				sql3 = sql.replace("a.uin", "uid");
			}
			if(sql.contains("o.comid")){
				sql3 = sql.replace("o.comid", "comid");
			}
			if(groupSql.contains("a.uin")){
				groupSql3 = groupSql.replace("a.uin", "uid");
			}
			if(groupSql.contains("o.comid")){
				groupSql3 = groupSql.replace("o.comid", "comid");
			}
			if(groupSql3.contains(",")){
				groupSql3 = groupSql3.substring(1);
			}
			SqlInfo sqlInfo6 = new SqlInfo(sql3, params1);
			List<Map<String, Object>> list6 = anlysisMoney(6, startTime, endTime, new String[]{groupSql3}, sqlInfo6, null);
			String sql4 = sql;
			String groupSql4 = groupSql;
			if(sql.contains("a.uin")){
				sql4 = sql.replace("a.uin", "out_uid");
			}
			if(sql.contains("o.comid")){
				sql4 = sql.replace("o.comid", "comid");
			}
			if(groupSql.contains("a.uin")){
				groupSql4 = groupSql.replace("a.uin", "out_uid");
			}
			if(groupSql.contains("o.comid")){
				groupSql4 = groupSql.replace("o.comid", "comid");
			}
			if(groupSql4.contains(",")){
				groupSql4 = groupSql4.substring(1);
			}
			SqlInfo sqlInfo7 = new SqlInfo(sql4, params1);
			List<Map<String, Object>> list7 = anlysisMoney(7, startTime, endTime, new String[]{groupSql4}, sqlInfo7, null);
			String sql5 = sql;
			String groupSql5 = groupSql;
			if(sql.contains("a.uin")){
				sql5 = sql.replace("a.uin", "o.uid");
			}
			if(groupSql.contains("a.uin")){
				groupSql5 = groupSql.replace("a.uin", "o.uid");
			}
			if(groupSql5.contains(",")){
				groupSql5 = groupSql5.substring(1);
			}
			SqlInfo sqlInfo8 = new SqlInfo(sql5, params1);
			List<Map<String, Object>> list8 = anlysisMoney(8, startTime, endTime, new String[]{groupSql5}, sqlInfo8, null);
			List<Map<String, Object>> list9 = anlysisMoney(9, startTime, endTime, new String[]{groupSql5}, sqlInfo8, null);
			List<Map<String, Object>> list10 = anlysisMoney(10, startTime, endTime, new String[]{groupSql5}, sqlInfo8, null);
			List<Map<String, Object>> list11 = anlysisMoney(11, startTime, endTime, new String[]{groupSql5}, sqlInfo8, null);
			List<Map<String, Object>> list12 = anlysisMoney(12, startTime, endTime, new String[]{groupSql5}, sqlInfo8, null);
			List<Map<String, Object>> list14 = anlysisMoney(14, startTime, endTime, new String[]{groupSql5}, sqlInfo8, null);
			params1.clear();
			params1.addAll(params);
			params1.add(4);//charge_type -- ��ֵ��ʽ��4��Ԥ֧���˿�
			params1.add(0);//consume_type --���ѷ�ʽ 0��֧��ͣ���ѣ���Ԥ����
			params1.add(1);//consume_type --���ѷ�ʽ 1��Ԥ��ͣ����
			params1.add(2);//consume_type --���ѷ�ʽ 2������ͣ����
			params1.add(3);//consume_type --���ѷ�ʽ3��׷��ͣ����
			SqlInfo sqlInfo9 = new SqlInfo(sql2 + " and (a.charge_type in (?) or a.consume_type in (?,?,?,?))", params1);
			List<Map<String, Object>> list13 = anlysisMoney(13, startTime, endTime, 
					new String[]{"a.charge_type,a.consume_type" + groupSql2}, sqlInfo9, null);
			
			List<Map<String, Object>> infoList = new ArrayList<Map<String,Object>>();
			List<Object> idList = new ArrayList<Object>();
			mergeIncome(1, list1, infoList, idList);
			mergeIncome(2, list2, infoList, idList);
			mergeIncome(3, list3, infoList, idList);
			mergeIncome(4, list4, infoList, idList);
			mergeIncome(5, list5, infoList, idList);
			mergeIncome(6, list6, infoList, idList);
			mergeIncome(7, list7, infoList, idList);
			mergeIncome(8, list8, infoList, idList);
			mergeIncome(9, list9, infoList, idList);
			mergeIncome(10, list10, infoList, idList);
			mergeIncome(11, list11, infoList, idList);
			mergeIncome(12, list12, infoList, idList);
			mergeIncome(13, list13, infoList, idList);
			mergeIncome(14, list14, infoList, idList);
			return infoList;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
	
	private List<Map<String, Object>> mergeIncome(int type, List<Map<String, Object>> list, List<Map<String, Object>> infoList, List<Object> idList){
		try {
			if(list != null && !list.isEmpty()){
				for(Map<String, Object> map : list){
					Long id = null;
					Integer target = null;
					Double summoney = 0d;
					if(map.get("summoney") != null){
						summoney = Double.valueOf(map.get("summoney") + "");
					}
					if(map.get("comid") != null){
						id = (Long)map.get("comid");
					}else if(map.get("uin") != null){
						id = (Long)map.get("uin");
					}else if(map.get("uid") != null){
						id = (Long)map.get("uid");
					}else if(map.get("out_uid") != null){
						id = (Long)map.get("out_uid");
					}
					if(map.get("target") != null){
						target = (Integer)map.get("target");
					}else if(map.get("source") != null){
						target = (Integer)map.get("source");
					}
					if(idList.contains(id)){
						for(Map<String, Object> infoMap : infoList){
							Long infoId = (Long)infoMap.get("id");
							if(id.intValue() == infoId.intValue()){
								Double prepay_cash = Double.valueOf(infoMap.get("prepay_cash") + "");//�ֽ�Ԥ֧��
								Double add_cash = Double.valueOf(infoMap.get("add_cash") + "");//�ֽ𲹽�
								Double refund_cash = Double.valueOf(infoMap.get("refund_cash") + "");//�ֽ��˿�
								Double pursue_cash = Double.valueOf(infoMap.get("pursue_cash") + "");//�ֽ�׷��
								Double pfee_cash = Double.valueOf(infoMap.get("pfee_cash") + "");//�ֽ�ͣ���ѣ���Ԥ����
								Double prepay_epay = Double.valueOf(infoMap.get("prepay_epay") + "");//����Ԥ֧��
								Double add_epay = Double.valueOf(infoMap.get("add_epay") + "");//���Ӳ���
								Double refund_epay = Double.valueOf(infoMap.get("refund_epay") + "");//�����˿�
								Double pursue_epay = Double.valueOf(infoMap.get("pursue_epay") + "");//����׷��
								Double pfee_epay = Double.valueOf(infoMap.get("pfee_epay") + "");//����ͣ���ѣ���Ԥ����
								Double escape = Double.valueOf(infoMap.get("escape") + "");//�ӵ�δ׷�ɵ�ͣ����
								Double prepay_escape = Double.valueOf(infoMap.get("prepay_escape") + "");//�ӵ�δ׷�ɵĶ�����Ԥ�ɵĽ��
								Double sensor_fee = Double.valueOf(infoMap.get("sensor_fee") + "");//������ͣ����
								Double prepay_card = Double.valueOf(infoMap.get("prepay_card") + "");//ˢ��Ԥ֧��
								Double add_card = Double.valueOf(infoMap.get("add_card") + "");//ˢ������
								Double refund_card = Double.valueOf(infoMap.get("refund_card") + "");//ˢ���˿�
								Double pursue_card = Double.valueOf(infoMap.get("pursue_card") + "");//ˢ��׷��
								Double pfee_card = Double.valueOf(infoMap.get("pfee_card") + "");//ˢ��ͣ���ѣ���Ԥ����
								if(type == 1){
									if(target == 0){//�ֽ�ͣ���ѣ���Ԥ����
										pfee_cash += summoney;
									}else if(target == 1){//Ԥ��ͣ����
										prepay_cash += summoney;
									}else if(target == 2){//Ԥ���˿Ԥ�����
										refund_cash += summoney;
									}else if(target == 3){//Ԥ�����ɣ�Ԥ�����㣩
										add_cash += summoney; 
									}else if(target == 4){//׷��ͣ����
										pursue_cash += summoney;
									}
								}else if(type == 2){
									if(target == 4){//������ͣ���ѣ���Ԥ�������ߴ����շ�Ա
										pfee_epay += summoney;
									}else if(target == 5){//׷��ͣ����
										pursue_epay += summoney;
									}else if(target == 6){//Ԥ��ͣ����
										prepay_epay += summoney;
									}else if(target == 7){//Ԥ���˿Ԥ�����
										refund_epay += summoney;
									}else if(target == 8){//Ԥ�����ɣ�Ԥ�����㣩
										add_epay += summoney; 
									}
								}else if(type == 3){
									if(target == 0){//ͣ���ѣ���Ԥ����
										pfee_epay += summoney;
									}else if(target == 7){//׷��ͣ����
										pursue_epay += summoney;
									}else if(target == 8){//Ԥ��ͣ����
										prepay_epay += summoney;
									}else if(target == 9){//Ԥ���˿Ԥ�����
										refund_epay += summoney;
									}else if(target == 10){//Ԥ�����ɣ�Ԥ�����㣩
										add_epay += summoney; 
									}
								}else if(type == 4){
									if(target == 0){//ͣ���ѣ���Ԥ����
										pfee_epay += summoney;
									}else if(target == 2){//׷��ͣ����
										pursue_epay += summoney;
									}else if(target == 3){//Ԥ��ͣ����
										prepay_epay += summoney;
									}else if(target == 4){//Ԥ���˿Ԥ�����
										refund_epay += summoney;
									}else if(target == 5){//Ԥ�����ɣ�Ԥ�����㣩
										add_epay += summoney; 
									}
								}else if(type == 5){
									if(target == 0){//ͣ���ѣ���Ԥ����
										pfee_epay += summoney;
									}else if(target == 2){//׷��ͣ����
										pursue_epay += summoney;
									}else if(target == 3){//Ԥ��ͣ����
										prepay_epay += summoney;
									}else if(target == 4){//Ԥ���˿Ԥ�����
										refund_epay += summoney;
									}else if(target == 5){//Ԥ�����ɣ�Ԥ�����㣩
										add_epay += summoney; 
									}
								}else if(type == 6){
									escape += summoney;
								}else if(type == 7){
									sensor_fee += summoney;
								}else if(type == 8 
										|| type == 9 
										|| type == 10 
										|| type == 11 
										|| type == 12 
										|| type == 14){
									prepay_escape += summoney;
								}else if(type == 13){
									Integer charge_type = (Integer)map.get("charge_type");
									Integer consume_type = (Integer)map.get("consume_type");
									if(charge_type == 4){//4��Ԥ֧���˿�
										refund_card += summoney;
									}else if(consume_type == 0){//0��֧��ͣ���ѣ���Ԥ����
										pfee_card += summoney;
									}else if(consume_type == 1){//1��Ԥ��ͣ����
										prepay_card += summoney;
									}else if(consume_type == 2){//2������ͣ����
										add_card += summoney;
									}else if(consume_type == 3){//3��׷��ͣ����
										pursue_card += summoney;
									}
								}
								infoMap.put("prepay_cash", prepay_cash);
								infoMap.put("add_cash", add_cash);
								infoMap.put("refund_cash", refund_cash);
								infoMap.put("pursue_cash", pursue_cash);
								infoMap.put("pfee_cash", pfee_cash);
								infoMap.put("prepay_epay", prepay_epay);
								infoMap.put("add_epay", add_epay);
								infoMap.put("refund_epay", refund_epay);
								infoMap.put("pursue_epay", pursue_epay);
								infoMap.put("pfee_epay", pfee_epay);
								infoMap.put("escape", escape);
								infoMap.put("prepay_escape", prepay_escape);
								infoMap.put("sensor_fee", sensor_fee);
								infoMap.put("prepay_card", prepay_card);
								infoMap.put("add_card", add_card);
								infoMap.put("refund_card", refund_card);
								infoMap.put("pursue_card", pursue_card);
								infoMap.put("pfee_card", pfee_card);
							}
						}
					}else{
						idList.add(id);
						Double prepay_cash = 0d;
						Double add_cash = 0d;
						Double refund_cash = 0d;
						Double pursue_cash = 0d;
						Double pfee_cash = 0d;
						Double prepay_epay = 0d;
						Double add_epay = 0d;
						Double refund_epay = 0d;
						Double pfee_epay = 0d;
						Double pursue_epay = 0d;
						Double escape = 0d;
						Double prepay_escape = 0d;
						Double sensor_fee = 0d;
						Double prepay_card = 0d;
						Double add_card = 0d;
						Double refund_card = 0d;
						Double pursue_card = 0d;
						Double pfee_card = 0d;
						if(type == 1){
							if(target == 0){//�ֽ�ͣ���ѣ���Ԥ����
								pfee_cash += summoney;
							}else if(target == 1){//Ԥ��ͣ����
								prepay_cash += summoney;
							}else if(target == 2){//Ԥ���˿Ԥ�����
								refund_cash += summoney;
							}else if(target == 3){//Ԥ�����ɣ�Ԥ�����㣩
								add_cash += summoney; 
							}else if(target == 4){//׷��ͣ����
								pursue_cash += summoney;
							}
						}else if(type == 2){
							if(target == 4){//������ͣ���ѣ���Ԥ�������ߴ����շ�Ա
								pfee_epay += summoney;
							}else if(target == 5){//׷��ͣ����
								pursue_epay += summoney;
							}else if(target == 6){//Ԥ��ͣ����
								prepay_epay += summoney;
							}else if(target == 7){//Ԥ���˿Ԥ�����
								refund_epay += summoney;
							}else if(target == 8){//Ԥ�����ɣ�Ԥ�����㣩
								add_epay += summoney; 
							}
						}else if(type == 3){
							if(target == 0){//ͣ���ѣ���Ԥ����
								pfee_epay += summoney;
							}else if(target == 7){//׷��ͣ����
								pursue_epay += summoney;
							}else if(target == 8){//Ԥ��ͣ����
								prepay_epay += summoney;
							}else if(target == 9){//Ԥ���˿Ԥ�����
								refund_epay += summoney;
							}else if(target == 10){//Ԥ�����ɣ�Ԥ�����㣩
								add_epay += summoney; 
							}
						}else if(type == 4){
							if(target == 0){//ͣ���ѣ���Ԥ����
								pfee_epay += summoney;
							}else if(target == 2){//׷��ͣ����
								pursue_epay += summoney;
							}else if(target == 3){//Ԥ��ͣ����
								prepay_epay += summoney;
							}else if(target == 4){//Ԥ���˿Ԥ�����
								refund_epay += summoney;
							}else if(target == 5){//Ԥ�����ɣ�Ԥ�����㣩
								add_epay += summoney; 
							}
						}else if(type == 5){
							if(target == 0){//ͣ���ѣ���Ԥ����
								pfee_epay += summoney;
							}else if(target == 2){//׷��ͣ����
								pursue_epay += summoney;
							}else if(target == 3){//Ԥ��ͣ����
								prepay_epay += summoney;
							}else if(target == 4){//Ԥ���˿Ԥ�����
								refund_epay += summoney;
							}else if(target == 5){//Ԥ�����ɣ�Ԥ�����㣩
								add_epay += summoney; 
							}
						}else if(type == 6){
							escape += summoney;
						}else if(type == 7){
							sensor_fee += summoney;
						}else if(type == 8 
								|| type == 9 
								|| type == 10 
								|| type == 11 
								|| type == 12 
								|| type ==14){
							prepay_escape += summoney;
						}else if(type == 13){
							Integer charge_type = (Integer)map.get("charge_type");
							Integer consume_type = (Integer)map.get("consume_type");
							if(charge_type == 4){//4��Ԥ֧���˿�
								refund_card += summoney;
							}else if(consume_type == 0){//0��֧��ͣ���ѣ���Ԥ����
								pfee_card += summoney;
							}else if(consume_type == 1){//1��Ԥ��ͣ����
								prepay_card += summoney;
							}else if(consume_type == 2){//2������ͣ����
								add_card += summoney;
							}else if(consume_type == 3){//3��׷��ͣ����
								pursue_card += summoney;
							}
						}
						Map<String, Object> infoMap = new HashMap<String, Object>();
						infoMap.put("prepay_cash", prepay_cash);
						infoMap.put("add_cash", add_cash);
						infoMap.put("refund_cash", refund_cash);
						infoMap.put("pursue_cash", pursue_cash);
						infoMap.put("pfee_cash", pfee_cash);
						infoMap.put("prepay_epay", prepay_epay);
						infoMap.put("add_epay", add_epay);
						infoMap.put("refund_epay", refund_epay);
						infoMap.put("pfee_epay", pfee_epay);
						infoMap.put("pursue_epay", pursue_epay);
						infoMap.put("escape", escape);
						infoMap.put("prepay_escape", prepay_escape);
						infoMap.put("sensor_fee", sensor_fee);
						infoMap.put("prepay_card", prepay_card);
						infoMap.put("add_card", add_card);
						infoMap.put("refund_card", refund_card);
						infoMap.put("pursue_card", pursue_card);
						infoMap.put("pfee_card", pfee_card);
						infoMap.put("id", id);
						infoList.add(infoMap);
					}
				}
			}
			return infoList;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
	
	/**
	 * ͳһ�ӿ�,ͳ��ͣ����
	 * @param type	1���ֽ�2���շ�Ա�˻������շѣ�3�������˻������շѣ�4����Ӫ�����˻������շѣ�5���̻��˻������շѣ�6����δ׷�ɶ�����7���鳵�����������
	 * @param startTime	��ʼʱ��
	 * @param endTime	����ʱ��
	 * @param groupby	�����ѯ���ֶ�
	 * @param sqlInfo	��������������
	 * @param otherMap	�ǻ�������������д������
	 * @return
	 */
	public List<Map<String, Object>> anlysisMoney(int type, Long startTime, Long endTime, 
			String[] groupby, SqlInfo sqlInfo, Map<String, Object> otherMap){
		List<Map<String, Object>> result= null;
		try {
			List<Object> params = new ArrayList<Object>();
			String sql = null;
			String condSql = "";
			params.add(startTime);
			params.add(endTime);
			String ogroupSql = groupSql(groupby);//��ѯ�����ֶ�
			String groupSql = "";
			if(!"".equals(ogroupSql)){
				groupSql = " group by " + ogroupSql.substring(1);
			}
			if(sqlInfo!=null){//������������
				condSql +=" and "+sqlInfo.getSql();
				params.addAll(sqlInfo.getParams());
			}
			switch (type) {
			case 1://���ֽ�
				sql = "select sum(a.amount) summoney "+ogroupSql+" from parkuser_cash_tb a,order_tb o " +
						" where a.orderid=o.id and a.create_time between ? and ? "+condSql + groupSql;
				break;
			case 2://���շ�Ա�˻������շ�
				sql = "select sum(a.amount) summoney "+ogroupSql+" from parkuser_account_tb a,order_tb o " +
						" where a.orderid=o.id and a.create_time between ? and ? " + condSql +
						" and a.remark like ? "+ groupSql;
				params.add("ͣ����%");
				break;
			case 3://�鳵���˻������շ�
				sql = "select sum(a.amount) summoney "+ogroupSql+" from park_account_tb a,order_tb o " +
						" where a.orderid=o.id and a.create_time between ? and ? "+condSql + groupSql;
				break;
			case 4://����Ӫ�����˻������շ�
				sql = "select sum(a.amount) summoney "+ogroupSql+" from group_account_tb a,order_tb o " +
						" where a.orderid=o.id and a.create_time between ? and ? "+condSql + groupSql;
				break;
			case 5://���̻��˻������շ�
				sql = "select sum(a.amount) summoney "+ogroupSql+" from city_account_tb a,order_tb o " +
						" where a.orderid=o.id and a.create_time between ? and ? "+condSql + groupSql;
				break;
			case 6://��δ׷�ɽ��
				sql = "select sum(total) summoney "+ogroupSql+" from no_payment_tb where end_time " +
						" between ? and ? "+condSql+" and state=? "+groupSql;
				params.add(0);
				break;
			case 7://�鳵�����������
				sql = "select sum(total) summoney "+ogroupSql+" from berth_order_tb where out_time" +
						" between ? and ? "+condSql + " " + groupSql;
				break;
			case 8://���ӵ���δ׷�ɵĶ����ֽ�Ԥ���Ľ��
				sql = "select sum(a.amount) summoney "+ogroupSql+" from no_payment_tb o,parkuser_cash_tb a" +
						" where o.order_id=a.orderid and o.end_time between ? and ? " + condSql + 
						" and o.state=? and a.target=? " + groupSql;
				params.add(0);//δ׷��
				params.add(1);//Ԥ��ͣ����
				break;
			case 9://���ӵ���δ׷�ɵĶ�������Ԥ���Ľ��
				sql = "select sum(a.amount) summoney "+ogroupSql+" from no_payment_tb o,parkuser_account_tb a" +
						" where o.order_id=a.orderid and o.end_time between ? and ? " + condSql + 
						" and o.state=? and a.target=? " + groupSql;
				params.add(0);//δ׷��
				params.add(6);//Ԥ��ͣ����
				break;
			case 10://���ӵ���δ׷�ɵĶ�������Ԥ���Ľ��
				sql = "select sum(a.amount) summoney "+ogroupSql+" from no_payment_tb o,park_account_tb a" +
						" where o.order_id=a.orderid and o.end_time between ? and ? " + condSql +
						" and o.state=? and a.source=? " + groupSql;
				params.add(0);//δ׷��
				params.add(8);//Ԥ��ͣ����
				break;
			case 11://���ӵ���δ׷�ɵĶ�������Ԥ���Ľ��
				sql = "select sum(a.amount) summoney "+ogroupSql+" from no_payment_tb o,group_account_tb a" +
						" where o.order_id=a.orderid and o.end_time between ? and ? " + condSql +
						" and o.state=? and a.source=? " + groupSql;
				params.add(0);//δ׷��
				params.add(3);//Ԥ��ͣ����
				break;
			case 12://���ӵ���δ׷�ɵĶ�������Ԥ���Ľ��
				sql = "select sum(a.amount) summoney "+ogroupSql+" from no_payment_tb o,city_account_tb a" +
						" where o.order_id=a.orderid and o.end_time between ? and ? " + condSql +
						" and o.state=? and a.source=? " + groupSql;
				params.add(0);//δ׷��
				params.add(3);//Ԥ��ͣ����
				break;
			case 13://��ѯˢ�����
				sql = "select sum(a.amount) summoney "+ogroupSql+" from card_account_tb a,order_tb o " +
						" where a.orderid=o.id and a.create_time between ? and ? "+condSql + groupSql;
				break;
			case 14://���ӵ���δ׷�ɵ�ˢ��Ԥ���Ľ��
				sql = "select sum(a.amount) summoney "+ogroupSql+" from no_payment_tb o,card_account_tb a" +
						" where o.order_id=a.orderid and o.end_time between ? and ? " + condSql + 
						" and o.state=? and a.consume_type=? " + groupSql;
				params.add(0);//δ׷��
				params.add(1);//Ԥ��ͣ����
				break;
			default:
				break;
			}
			if(sql != null){
				result = pgOnlyReadService.getAllMap(sql, params);
			}
		} catch (Exception e) {
			logger.error("anlysisMoney", e);
		}
		return result;
	}
	
	/**
	 * ƴ�ӷ����ֶ�sql
	 * @param groupMap
	 * @return
	 */
	private String groupSql(String[] groupby){
		String groupSql = "";//�����ֶ�
		try {
			if(groupby != null && groupby.length > 0){
				for(int i = 0; i < groupby.length; i++){
					groupSql += "," + groupby[i];
				}
			}
		} catch (Exception e) {
			logger.error("groupSql", e);
		}
		return groupSql;
	}
}
