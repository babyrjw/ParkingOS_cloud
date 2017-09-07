package com.zld.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import com.zld.CustomDefind;
import com.zld.pojo.Berth;
import com.zld.pojo.Car;
import com.zld.pojo.Card;
import com.zld.pojo.CardCarNumber;
import com.zld.pojo.Group;
import com.zld.pojo.Induce;
import com.zld.pojo.Order;
import com.zld.pojo.Sensor;
import com.zld.pojo.Site;
import com.zld.pojo.Tenant;
import com.zld.pojo.WorkRecord;
import com.zld.pojo.WorkTime;
import com.zld.service.DataBaseService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.HttpProxy;
import com.zld.utils.SqlInfo;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
@Repository
public class CommonMethods {
	
	private Logger logger = Logger.getLogger(CommonMethods.class);
	@Autowired
	private MemcacheUtils memcacheUtils;
	@Autowired
	private DataBaseService daService;
	@Autowired
	private PublicMethods publicMethods;
	@Autowired
	private PgOnlyReadService pService;
	
	private static Map<String, Map<String, Object>> slowReqMap = 
			new ConcurrentHashMap<String, Map<String, Object>>();
	/**
	 * ���ǩ��ǩ���Ƿ��������ϰ�ʱ����
	 * @param role_id ��ɫ
	 * @param time	ǩ��ǩ��ʱ��
	 * @param type	0��ǩ�� 1��ǩ��
	 * @return
	 */
	public boolean checkWorkTime(Long uin, long time){
		try {
			if(uin != null && uin > 0 && time > 0){
				Map<String, Object> userMap = pService.getMap("select role_id from user_info_tb where id=? and role_id>? ",
						new Object[]{uin, 0});
				if(userMap != null){
					Long role_id = (Long)userMap.get("role_id");
					long offsetTime = time - TimeTools.getToDayBeginTime();
					logger.error("offsetTime:"+offsetTime);
					List<WorkTime> workTimes = pService.getPOJOList("select * from work_time_tb " +
							" where role_id=? and is_delete=? ", new Object[]{role_id, 0}, WorkTime.class);
					if(workTimes != null && !workTimes.isEmpty()){
						for(WorkTime workTime : workTimes){
							int b_hour = workTime.getB_hour();
							int b_minute = workTime.getB_minute();
							int e_hour = workTime.getE_hour();
							int e_minute = workTime.getE_minute();
							int start = b_hour * 60 * 60 + b_minute * 60;
							int end = e_hour * 60 * 60 + e_minute * 60;
							if(offsetTime > start && offsetTime < end){//�ϰ��ڼ�ǩ��ǩ�������쳣
								return false;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("����ϰ�״̬�쳣", e);
		}
		return true;
	}
	
	/**
	 * ��鼯�������ĳ����̻��Ƿ������˿缯��׷��
	 * @param groupId
	 * @return
	 */
	public boolean pursueInCity(Long groupId){
		try {
			if(groupId != null && groupId > 0){
				Group group = pService.getPOJO("select cityid from org_group_tb where id=? and cityid>? ",
						new Object[]{groupId, 0}, Group.class);
				if(group != null){
					Tenant tenant = pService.getPOJO("select is_group_pursue from org_city_merchants " +
							" where state=? and id=? ", new Object[]{0, group.getCityid()}, Tenant.class);
					if(tenant != null && tenant.getIs_group_pursue() == 1){
						//logger.error("�ü��������ĳ����̻������˿��Կ缯��׷��,groupid:"+groupId);
						return true;
					}
				}
			}
		} catch (Exception e) {
			logger.error("����", e);
		}
		return false;
	}
	
	/**
	 * ��鼯�������ĳ����̻��Ƿ�������ͬһ���ƿɷ��ڳ������ظ��볡
	 * @param groupId
	 * @return
	 */
	public boolean isInparkInCity(Long groupId){
		try {
			if(groupId != null && groupId > 0){
				Group group = pService.getPOJO("select cityid from org_group_tb where id=? and cityid>? ",
						new Object[]{groupId, 0}, Group.class);
				if(group != null){
					Tenant tenant = pService.getPOJO("select is_inpark_incity from org_city_merchants " +
							" where state=? and id=? ", new Object[]{0, group.getCityid()}, Tenant.class);
					if(tenant != null && tenant.getIs_inpark_incity() == 0){
						logger.error("�ü��������ĳ����̻������˿���ͬһ���ƿɷ��ڳ������ظ��볡,groupid:"+groupId);
						return false;
					}
				}
			}
		} catch (Exception e) {
			logger.error("����", e);
		}
		return true;
	}
	
	/**
	 * ���ݳ��ƺ��б��Ƿ��ǿ�Ƭ�û�
	 * @param carNumber
	 * @param groupId ��Ӫ���ű��
	 * @return
	 */
	public boolean cardUser(String carNumber, long groupId){
		try {
			if(carNumber == null || "".equals(carNumber) || groupId <= 0){
				return false;
			}
			Car car = pService.getPOJO("select uin from car_info_tb where car_number=? " +
					" and state=? and uin>? ", new Object[]{carNumber, 1, 0}, Car.class);
			if(car != null){
				long userId = car.getUin();
				Long count = pService.getLong("select count(id) from com_nfc_tb where group_id=? " +
						" and state=? and uin=? and is_delete=? and type=? ",
						new Object[]{groupId, 2, userId, 0, 2});
				if(count > 0){
					return true;
				}
			}
			List<CardCarNumber> cardCarNumbers = pService.getPOJOList("select card_id from " +
					" card_carnumber_tb where is_delete=? and car_number=? ", 
					new Object[]{0, carNumber}, CardCarNumber.class);
			if(cardCarNumbers != null && !cardCarNumbers.isEmpty()){
				List<Object> params = new ArrayList<Object>();
				String preParam = "";
				for(CardCarNumber c : cardCarNumbers){
					params.add(c.getCard_id());
					if("".equals(preParam)){
						preParam = "?";
					}else{
						preParam = ",?";
					}
				}
				params.add(groupId);
				params.add(4);
				params.add(0);
				params.add(2);
				Long count = pService.getCount("select count(id) from com_nfc_tb where id in " +
						" ("+preParam+") and group_id=? and state=? and is_delete=? and type=? ", params);
				if(count > 0){
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * ������Ľӿ�
	 */
	public void requestInitialized(HttpServletRequest request){
		try {
			request.setAttribute("reqInitTime", System.currentTimeMillis()/1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ������Ľӿ�
	 */
	public void requestDestroyed(HttpServletRequest request){
		try {
			String url = request.getServletPath();
			String action = request.getParameter("action");
			url = (action != null) ? (url + "?action=" +action ) : url;
			if(url.contains("collectorrequest.do")){//ֻ����շ�Ա��
				String localAddr = request.getLocalAddr();
				if(request.getAttribute("reqInitTime") == null){
					return;
				}
				long reqInitTime = (Long)request.getAttribute("reqInitTime");
				long reqDestTime = System.currentTimeMillis()/1000;
				long lifeTime = reqDestTime - reqInitTime;
				if(lifeTime > 5&&!"uporderpic".equals(action)){//�ӿڻ���5��,���˵�uporderpic
					if(slowReqMap.get(url) == null){
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("lastTime", reqDestTime);
						map.put("counter", 0);
						slowReqMap.put(url, map);
					}else{
						Map<String, Object> map = slowReqMap.get(url);
						int counter = (Integer)map.get("counter");
						long lastTime = (Long)map.get("lastTime");
						counter ++;
						map.put("counter", counter);
						if(counter >= 10){
							slowReqMap.remove(url);
							if(reqDestTime - lastTime <= 30 * 60){
								alertSlowInterface(url, reqInitTime, reqDestTime, localAddr);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void alertSlowInterface(String url, long startTime, long endTime, String localAddr){
		try {
			if(memcacheUtils.addLock(url, 1 * 60 * 60)){//һ���ӿ�1Сʱֻ��һ��
				publicMethods.sendCardMessage("18201517240", "�ӿڣ�" + url + "����");
				publicMethods.sendCardMessage("18101333937", "�ӿڣ�" + url + "����");
				publicMethods.sendCardMessage("17701081721", "�ӿڣ�" + url + "����");
				int r = daService.update("insert into slow_alert_tb(url,start_time,end_time," +
						"local_host) values(?,?,?,?)", new Object[]{url, startTime, endTime, localAddr});
				logger.error("alertSlowInterface,r:"+r);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ��ȡһ��������¼�ڵĽ���������
	 * @return
	 */
	public int getInOutCar(long workId, int type){
		try {
			if(workId > 0){
				String key = null;
				if(type == 0){
					key = "pos_in_car_number_" + workId; 
				}else if(type == 1){
					key = "pos_out_car_number_" + workId; 
				}
				if(key != null){
					String number = memcacheUtils.getCache(key);
					if(number != null){
						return Integer.valueOf(number);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * ����һ������ʱ���ڵĽ���������
	 * @param workId ������¼���
	 * @param type 0: ���� 1������
	 */
	public boolean updateInOutCar(long workId, int type){
		try {
			if(workId > 0){
				String key = null;
				if(type == 0){
					key = "pos_in_car_number_" + workId; 
				}else if(type == 1){
					key = "pos_out_car_number_" + workId; 
				}
				if(key != null){
					int num = 0;
					String number = memcacheUtils.getCache(key);
					if(number != null){
						num = Integer.valueOf(number);
					}
					num ++;//�ۼ�
					return memcacheUtils.setCache(key, num + "", 24 * 60 * 60);//����һ�죬һ������
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * ��ȡ��λ
	 * @param berthId ��λ���
	 * @return
	 */
	public Berth berth(Long berthId){
		try {
			Berth berth = pService.getPOJO("select * from com_park_tb where id=?" +
					" and is_delete=? ", new Object[]{berthId, 0}, Berth.class);
			return berth;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * ���ݿ�Ƭ���õĿ��ţ���ȡ��Ƭ��Ϣ
	 * @param nfc_uuid
	 * @param groupId
	 * @return
	 */
	public Card card(String nfc_uuid, Long groupId){
		try {
			Card card = daService.getPOJO("select * from com_nfc_tb where nfc_uuid like ?" +
					" and is_delete=? and type=? and state<>? and group_id=? limit ? ", 
					new Object[]{nfc_uuid + "%", 0, 2, 1, groupId, 1}, Card.class);
			//��Ƭ������ɣ���ֹ���
			return card;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * д���������ԭ���ǣ��ͻ��˵��ÿ�Ƭ�Ľӿ�ʱ�������߼������⣬��ʱ���Ǵ��������Ŀ��ţ�
	 * �е�ʱ���Ǵ��Ŀ�Ƭ���õĲ������Ŀ��ţ����ɵ���Ҳ��һ������������������⣬���ԣ�
	 * �ڴ���������������Ŀ��žͽ�ȡ��Ƭ�����ñ�������������ڵĿ��������֣�һ����8λ�ģ�
	 * һ����14λ��
	 * @param nfc_uuid
	 * @return
	 */
	public String getNFCLock(String nfc_uuid){
		String lock = null;
		try {
			if(nfc_uuid != null && !"".equals(nfc_uuid)){
				if(nfc_uuid.length() == 20){
					nfc_uuid = nfc_uuid.substring(0, 14);
					if(nfc_uuid.contains("000000")){
						nfc_uuid = nfc_uuid.substring(0, 8);
					}
				}
				lock = getLock(nfc_uuid);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lock;
	}
	
	/**
	 * ��ȡ������
	 * @param key
	 * @return
	 */
	public String getLock(Object key){
		String lock = null;
		try {
			String className = Thread.currentThread().getStackTrace()[2].getClassName();//��ȡ��ǰ��������һ�������ߵ�����
			String methodName = Thread.currentThread() .getStackTrace()[2].getMethodName();//��ȡ��ǰ��������һ�������ߵķ�����
			lock = className + "-" + methodName + "-" + key;
			return lock;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lock;
	}
	
	/**
	 * ��ȡ�շ�Ա��ǰǩ���Ĺ�����¼
	 * @param parkUserId
	 * @return
	 */
	public WorkRecord getWorkRecord(Long parkUserId){
		try {
			WorkRecord workRecord = pService.getPOJO("select * from parkuser_work_record_tb where " +
					" uid=? and state=? order by id desc limit ? ", 
					new Object[]{parkUserId, 0,  1}, WorkRecord.class);
			return workRecord;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * ���ݳ��ƺŲ�ѯ�ǲ��Ǹ�ͣ�������¿���Ա
	 * @param carNumber
	 * @param comId
	 * @return
	 */
	public boolean isMonthUser(String carNumber, Long comId){
		Integer monthcount = 0;
		Long uin = null;
		Map carMap = pService.getMap("select uin from car_info_Tb where car_number=? and state=? ", new Object[]{carNumber,1});
		if(carMap!=null&&carMap.get("uin")!=null)
			uin = (Long)carMap.get("uin");
		if(uin!=null&&uin>0){
			List rlist= publicMethods.isMonthUser(uin, comId);
			if(rlist!=null&&rlist.size()==2){
				monthcount = Integer.parseInt(rlist.get(1)+"");
				if(monthcount>0)
					return true;
			}
		}
		return false;
	}
	
	/**
	 * ����������վ���յ����ָ�����
	 * @param type 0:������ 1:��վ 2:�յ���
	 * @param uuid �豸Ψһ���
	 * @param heartBeat ����ʱ��
	 */
	public void deviceRecover(int type, String uuid, Long heartBeat){
		try {
			logger.error("type:"+type+",uuid:"+uuid+",heartBeat:"+heartBeat);
			switch (type) {
			case 0://������
				Sensor sensor = pService.getPOJO("select id from dici_tb where did=? and " +
						" is_delete=? limit ? ",new Object[]{uuid, 0, 1}, Sensor.class);
				if(sensor != null){
					int r = daService.update("update device_fault_tb set end_time=? " +
							" where sensor_id=? and end_time is null", 
							new Object[]{heartBeat, sensor.getId()});
					logger.error("snesorId:"+sensor.getId()+",r:"+r);
				}
				break;
			case 1://��վ
				Site site = pService.getPOJO("select id from sites_tb " +
						" where uuid=? and is_delete=? limit ? ", new Object[]{uuid, 0, 1},
						Site.class);
				if(site != null){
					int r = daService.update("update device_fault_tb set end_time=? " +
							" where site_id=? and end_time is null", 
							new Object[]{heartBeat, site.getId()});
					logger.error("siteId:"+site.getId()+",r:"+r);
				}
				break;
			case 2://�յ���
				Induce induce = pService.getPOJO("select id from induce_tb where" +
						" did=? and is_delete=? limit ?", new Object[]{uuid, 0, 1}, Induce.class);
				if(induce != null){
					int r = daService.update("update device_fault_tb set end_time=? " +
							" where induce_id=? and end_time is null", 
							new Object[]{heartBeat, induce.getId()});
					logger.error("induceId:"+induce.getId()+",r:"+r);
				}
				break;
			default:
				break;
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	/**
	 * ���ݳ��ƺźͳ�����ȷ����������
	 * @param carNumber
	 * @param comId
	 * @return
	 */
	public Integer getCarType(String carNumber, Long comId){
		Integer car_type = 0;
		try {
			Map<String, Object> carNumbertType = daService.getMap("select typeid from car_number_type_tb " +
					" where car_number=? and comid=?", new Object[]{carNumber,comId});
			if(carNumbertType != null && carNumbertType.get("typeid") != null){
				car_type = Integer.parseInt(carNumbertType.get("typeid")+"");
			}
			if(car_type == 0){//ȡĬ�ϳ���
				List<Map<String, Object>> allCarTypes = getCarType(comId);
				if(!allCarTypes.isEmpty()){
					car_type = Integer.valueOf(allCarTypes.get(0).get("value_no")+"");
				}
			}
		} catch (Exception e) {
			logger.error("getCarType", e);
		}
		return car_type;
	}
	
	/**
	 * ���ݲ�λ��ȡ��ǰ�ڸڵ��շ�Ա
	 * @param berthId	��λ���
	 * @return
	 */
	public Long getWorkingCollector(Long berthId){
		Long uin = -1L;
		try {
			if(berthId != null && berthId > 0){
				Map<String, Object> berthMap = pService.getMap("select berthsec_id from com_park_tb " +
						" where id=? and is_delete=? limit ?", new Object[]{berthId, 0, 1});
				logger.error("berthId:"+berthId+",berthMap:"+berthMap);
				if(berthMap != null && berthMap.get("berthsec_id") != null){
					Long berthsec_id = (Long)berthMap.get("berthsec_id");
					if(berthsec_id > 0){
						Long count = pService.getLong("select count(id) from work_berthsec_tb " +
								" where berthsec_id=? and state=? and is_delete=? ", new Object[]{berthsec_id, 1, 0});
						logger.error("berthId:"+berthId+",count:"+count);
						if(count > 0){
							Map<String, Object> workRecord = pService.getMap("select uid from parkuser_work_record_tb " +
									" where berthsec_id=? and state=? limit ? ", new Object[]{berthsec_id, 0, 1});
							logger.error("berthId:"+berthId+",workRecord:"+workRecord);
							if(workRecord != null && workRecord.get("uid") != null){
								uin = (Long)workRecord.get("uid");
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("getWorkingCollector", e);
		}
		logger.error("berthId:"+berthId+",uin:"+uin);
		return uin;
	}
	
	/**
	 * ��ȡ�շ�Ա���������
	 * @param startTime
	 * @param endTime
	 * @param uin
	 * @return
	 */
	public Map<String, Object> getIncome(Long startTime, Long endTime, Long uin){
		try {
			List<Object> params = new ArrayList<Object>();
			params.add(uin);
			params.add(0);//ͣ���ѣ���Ԥ��
			params.add(1);//Ԥ��ͣ����
			params.add(2);//Ԥ���˿Ԥ�����
			params.add(3);//Ԥ�����ɣ�Ԥ�����㣩
			params.add(4);//׷��ͣ����
			SqlInfo sqlInfo1 = new SqlInfo(" a.uin =? and a.target in (?,?,?,?,?)", params);
			List<Map<String, Object>> list1 = anlysisMoney(1, startTime, endTime, new String[]{"a.target"}, sqlInfo1, null);
			params.clear();
			params.add(uin);
			params.add(4);//������ͣ���ѣ���Ԥ�������ߴ����շ�Ա
			params.add(5);//׷��ͣ����
			params.add(6);//����Ԥ��ͣ����
			params.add(7);//Ԥ���˿Ԥ�����
			params.add(8);//Ԥ�����ɣ�Ԥ�����㣩
			SqlInfo sqlInfo2 = new SqlInfo(" a.uin =? and a.target in (?,?,?,?,?)", params);
			List<Map<String, Object>> list2 = anlysisMoney(2, startTime, endTime, new String[]{"a.target"}, sqlInfo2, null);
			params.clear();
			params.add(uin);
			params.add(0);//ͣ���ѣ���Ԥ����
			params.add(7);//׷��ͣ����
			params.add(8);//����Ԥ��ͣ����
			params.add(9);//Ԥ���˿Ԥ�����
			params.add(10);//Ԥ�����ɣ�Ԥ�����㣩
			SqlInfo sqlInfo3 = new SqlInfo(" a.uid =? and a.source in (?,?,?,?,?)", params);
			List<Map<String, Object>> list3 = anlysisMoney(3, startTime, endTime, new String[]{"a.source"}, sqlInfo3, null);
			params.clear();
			params.add(uin);
			params.add(0);//ͣ���ѣ���Ԥ����
			params.add(2);//׷��ͣ����
			params.add(3);//Ԥ��ͣ����
			params.add(4);//Ԥ���˿Ԥ����
			params.add(5);//Ԥ�����ɣ�Ԥ�����㣩
			SqlInfo sqlInfo4 = new SqlInfo(" a.uid =? and a.source in (?,?,?,?,?)", params);
			List<Map<String, Object>> list4 = anlysisMoney(4, startTime, endTime, new String[]{"a.source"}, sqlInfo4, null);
			params.clear();
			params.add(uin);
			params.add(0);//ͣ���ѣ���Ԥ����
			params.add(2);//׷��ͣ����
			params.add(3);//Ԥ��ͣ����
			params.add(4);//Ԥ���˿Ԥ����
			params.add(5);//Ԥ�����ɣ�Ԥ�����㣩
			SqlInfo sqlInfo5 = new SqlInfo(" a.uid =? and a.source in (?,?,?,?,?)", params);
			List<Map<String, Object>> list5 = anlysisMoney(5, startTime, endTime, new String[]{"a.source"}, sqlInfo5, null);
			params.clear();
			params.add(uin);
			SqlInfo sqlInfo6 = new SqlInfo(" uid =? ", params);
			List<Map<String, Object>> list6 = anlysisMoney(6, startTime, endTime, new String[]{}, sqlInfo6, null);
			params.clear();
			params.add(uin);
			SqlInfo sqlInfo8 = new SqlInfo(" o.uid =? ", params);
			List<Map<String, Object>> list8 = anlysisMoney(8, startTime, endTime, new String[]{}, sqlInfo8, null);
			params.clear();
			params.add(uin);
			SqlInfo sqlInfo9 = new SqlInfo(" o.uid =? ", params);
			List<Map<String, Object>> list9 = anlysisMoney(9, startTime, endTime, new String[]{}, sqlInfo9, null);
			params.clear();
			params.add(uin);
			SqlInfo sqlInfo10 = new SqlInfo(" o.uid =? ", params);
			List<Map<String, Object>> list10 = anlysisMoney(10, startTime, endTime, new String[]{}, sqlInfo10, null);
			params.clear();
			params.add(uin);
			SqlInfo sqlInfo11 = new SqlInfo(" o.uid =? ", params);
			List<Map<String, Object>> list11 = anlysisMoney(11, startTime, endTime, new String[]{}, sqlInfo11, null);
			params.clear();
			params.add(uin);
			SqlInfo sqlInfo12 = new SqlInfo(" o.uid =? ", params);
			List<Map<String, Object>> list12 = anlysisMoney(12, startTime, endTime, new String[]{}, sqlInfo12, null);
			params.clear();
			params.add(uin);
			SqlInfo sqlInfo14 = new SqlInfo(" o.uid =? ", params);
			List<Map<String, Object>> list14 = anlysisMoney(14, startTime, endTime, new String[]{}, sqlInfo14, null);
			params.clear();
			params.add(uin);
			params.add(4);//charge_type -- ��ֵ��ʽ��4��Ԥ֧���˿�
			params.add(0);//consume_type --���ѷ�ʽ 0��֧��ͣ���ѣ���Ԥ����
			params.add(1);//consume_type --���ѷ�ʽ 1��Ԥ��ͣ����
			params.add(2);//consume_type --���ѷ�ʽ 2������ͣ����
			params.add(3);//consume_type --���ѷ�ʽ3��׷��ͣ����
			SqlInfo sqlInfo13 = new SqlInfo(" a.uid =? and (a.charge_type in (?) or a.consume_type in (?,?,?,?)) ", params);
			List<Map<String, Object>> list13 = anlysisMoney(13, startTime, endTime, 
					new String[]{"a.charge_type,a.consume_type"}, sqlInfo13, null);
			Map<String, Object> infoMap = new HashMap<String, Object>();
			infoMap.put("prepay_cash", 0d);//�ֽ�Ԥ��
			infoMap.put("add_cash", 0d);//�ֽ𲹽�
			infoMap.put("refund_cash", 0d);//�ֽ��˿�
			infoMap.put("pursue_cash", 0d);//�ֽ�׷��
			infoMap.put("pfee_cash", 0d);//�ֽ�ͣ���ѣ���Ԥ����
			infoMap.put("prepay_epay", 0d);//����Ԥ֧��
			infoMap.put("add_epay", 0d);//���Ӳ���
			infoMap.put("refund_epay", 0d);//�����˿�
			infoMap.put("pfee_epay", 0d);//����ͣ���ѣ���Ԥ����
			infoMap.put("pursue_epay", 0d);//����׷��
			infoMap.put("escape", 0d);//�ӵ�δ׷�ɵ�ͣ����
			infoMap.put("prepay_escape", 0d);//�ӵ�δ׷�ɵĶ�����Ԥ�ɵĽ��
			infoMap.put("prepay_card", 0d);//ˢ��Ԥ��
			infoMap.put("add_card", 0d);//ˢ������
			infoMap.put("refund_card", 0d);//ˢ���˿�
			infoMap.put("pursue_card", 0d);//ˢ��׷��
			infoMap.put("pfee_card", 0d);//ˢ��ͣ���ѣ���Ԥ����
			mergeIncome(1, list1, infoMap);
			mergeIncome(2, list2, infoMap);
			mergeIncome(3, list3, infoMap);
			mergeIncome(4, list4, infoMap);
			mergeIncome(5, list5, infoMap);
			mergeIncome(6, list6, infoMap);
			mergeIncome(8, list8, infoMap);
			mergeIncome(9, list9, infoMap);
			mergeIncome(10, list10, infoMap);
			mergeIncome(11, list11, infoMap);
			mergeIncome(12, list12, infoMap);
			mergeIncome(13, list13, infoMap);
			mergeIncome(14, list14, infoMap);
			logger.error("startTime:"+startTime+",endTime:"+endTime+",uin:"+uin+",map:"+infoMap);
			return infoMap;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
	
	private Map<String, Object> mergeIncome(int type, List<Map<String, Object>> list, Map<String, Object> infoMap){
		try {
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
			Double prepay_card = Double.valueOf(infoMap.get("prepay_card") + "");//ˢ��Ԥ֧��
			Double add_card = Double.valueOf(infoMap.get("add_card") + "");//ˢ������
			Double refund_card = Double.valueOf(infoMap.get("refund_card") + "");//ˢ���˿�
			Double pursue_card = Double.valueOf(infoMap.get("pursue_card") + "");//ˢ��׷��
			Double pfee_card = Double.valueOf(infoMap.get("pfee_card") + "");//ˢ��ͣ���ѣ���Ԥ����
			if(list != null && !list.isEmpty()){
				for(Map<String, Object> map : list){
					Integer target = null;
					if(map.get("target") != null){
						target = (Integer)map.get("target");
					}else if(map.get("source") != null){
						target = (Integer)map.get("source");
					}
					Double summoney = 0d;
					if(map.get("summoney") != null){
						summoney = Double.valueOf(map.get("summoney") + "");
					}
					switch (type) {
					case 1:
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
						break;
					case 2:
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
						break;
					case 3:
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
						break;
					case 4:
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
						break;
					case 5:
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
						break;
					case 6:
						escape += summoney;
						break;
					case 8:
					case 9:
					case 10:
					case 11:
					case 12:
					case 14:
						prepay_escape += summoney;
						break;
					case 13:
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
						break;
					default:
						break;
					}
				}
			}
			if(prepay_cash < 0) prepay_cash = 0d;
			if(add_cash < 0) add_cash = 0d;
			if(refund_cash < 0) refund_cash = 0d;
			if(pursue_cash < 0) pursue_cash = 0d;
			if(pfee_cash < 0) pfee_cash = 0d;
			if(prepay_epay < 0) prepay_epay = 0d;
			if(add_epay < 0) add_epay = 0d;
			if(refund_epay < 0) refund_epay = 0d;
			if(pursue_epay < 0) pursue_epay = 0d;
			if(pfee_epay < 0) pfee_epay = 0d;
			if(escape < 0) escape = 0d;
			if(prepay_escape < 0) prepay_escape = 0d;
			if(prepay_card < 0) prepay_card = 0d;
			if(add_card < 0) add_card = 0d;
			if(refund_card < 0) refund_card = 0d;
			if(pursue_card < 0) pursue_card = 0d;
			if(pfee_card < 0) pfee_card = 0d;
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
			infoMap.put("prepay_card", prepay_card);
			infoMap.put("add_card", add_card);
			infoMap.put("refund_card", refund_card);
			infoMap.put("pursue_card", pursue_card);
			infoMap.put("pfee_card", pfee_card);
			return infoMap;
		} catch (Exception e) {
			logger.error("mergeIncome", e);
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
				result = pService.getAllMap(sql, params);
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
	
	/**
	 * ��Ϊ�ӵ�
	 * @param orderid	POS���������
	 * @param uid	��Ϊ�ӵ����շ�Ա���
	 * @param money	�ܽ��
	 * @param brethorderid	�������������
	 * @return
	 */
	public boolean escape(Long orderid, Long uid, Double money, Long endtime){
		logger.error("orderid:"+orderid+",uid:"+uid+",money:"+money+",endtime:"+endtime);
		//�Ȳ�ѯ�Ƿ��Ѿ����ӵ�
		Long count = daService.getLong("select count(*) from no_payment_tb where order_id=? ", new Object[]{orderid});
		if(count>0)//�Ѿ�д���ӵ���
			return true;
		long comId = -1;
		long workId = -1L;//������¼���
		boolean result = false;//������¼���ɽ��
		String lock = null;
		try {
			//----------------------------�ֲ�ʽ��--------------------------------//
			lock = getLock(orderid);
			if(!memcacheUtils.addLock(lock)){
				logger.error("lock:"+lock);
				return false;
			}
			Long curTime = System.currentTimeMillis()/1000;
			Order order = daService.getPOJO("select * from order_tb where id=? and state=? ",
					new Object[]{orderid, 0}, Order.class);
			if(order != null){
				logger.error("order:" + order);
				if(endtime == null || endtime <= 0){
					endtime = curTime;
				}
				if(order.getPrepaid() >= money){
					logger.error("Ԥ�����ڵ���ͣ�����,������Ϊ�ӵ�");
					return false;
				}
				if(order.getState() == 2){
					logger.error("�Ѿ���Ϊ�ӵ�");
					return true;
				}
				//------------------------�󶨵ĳ���������ʱ��----------------------------//
				Long brethorderid = getBerthOrderId(orderid);
				Long end_time = getSensorTime(brethorderid, 1, uid, endtime);
				//logger.error("brethorderid:"+brethorderid+",end_time:"+end_time);
				//------------------------��ȡ��������----------------------------//
				comId = order.getComid();
				Long berthId = order.getBerthnumber();
				Long berthSegId = order.getBerthsec_id();
				Long groupId = order.getGroupid();
				Double prepay = order.getPrepaid();
				Long create_time = order.getCreate_time();
				String car_number = order.getCar_number();
				Long uin = order.getUin();
				//------------------------��ȡ������¼----------------------------//
				if(uid != null && uid > 0){//�еĶ������Զ���Ϊ�ӵ��ģ�����û�г����շ�Ա
					WorkRecord workRecord = getWorkRecord(uid);
					if(workRecord != null){
						workId = workRecord.getId();
					}
				}
				//logger.error("workId:"+workId);
				//------------------------�����߼�----------------------------//
				List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
				//���¶���״̬���շѳɹ�
				Map<String, Object> escapeSqlMap = new HashMap<String, Object>();
				Map<String, Object> berthSqlMap = new HashMap<String, Object>();
				Map<String, Object> orderSqlMap = new HashMap<String, Object>();
				escapeSqlMap.put("sql", "insert into no_payment_tb (create_time,end_time,order_id,total,car_number," +
						"uin,comid,uid,berthseg_id,berth_id,groupid,prepay) values(?,?,?,?,?,?,?,?,?,?,?,?)");
				escapeSqlMap.put("values", new Object[]{create_time, end_time, orderid, money, car_number, uin,
						comId, uid, berthSegId, berthId, groupId, prepay});
				bathSql.add(escapeSqlMap);
				if(berthId != null && berthId >0){
					berthSqlMap.put("sql", "update com_park_tb set state=?,order_id=? where id =? and order_id=? ");
					berthSqlMap.put("values", new Object[]{0, null, berthId, orderid});
					bathSql.add(berthSqlMap);
				}
				orderSqlMap.put("sql", "update order_tb set state=?,total=?,end_time=?,out_uid=? where id =?");
				orderSqlMap.put("values", new Object[]{2, money, end_time, uid, orderid});
				bathSql.add(orderSqlMap);
				result = daService.bathUpdate2(bathSql);
				logger.error("orderid:"+orderid+",uid:"+uid+",money:"+money+"(update com_park_tb orderid) bathsql result:"+result);
				return result;
			}
		} catch (Exception e) {
			logger.error("escapeorderid:"+orderid, e);
		} finally {
			boolean b = memcacheUtils.delLock(lock);
			//logger.error("b:"+b);
			if(result && workId > 0){//��������ɹ������³�������
				//logger.error("workId:"+workId);
				boolean b1 = updateInOutCar(workId, 1);
				//logger.error("���³�������,b1:"+b1);
			}
			updateRemainBerth(comId, 1);
		}
		return false;
	}
	
	/**
	 * ��ȡ��������ʱ��
	 * @param orderId �������
	 * @param type 0������ 1������
	 * @param uid �շ�Ա���
	 * @param curtime ��ǰϵͳʱ��
	 * @return
	 */
	public Long getOrderEndTime(Long brethOrderId, Long uid, Long curtime){
		Long sensortime = curtime;
		try {
			sensortime = getSensorTime(brethOrderId, 1, uid, curtime);
		} catch (Exception e) {
			logger.error("getOrderTime", e);
		}
		return sensortime;
	}
	
	/**
	 * ��ȡ������ʼʱ��
	 * @param orderId �������
	 * @param type 0������ 1������
	 * @param uid �շ�Ա���
	 * @param curtime ��ǰϵͳʱ��
	 * @return
	 */
	public Long getOrderStartTime(Long brethOrderId, Long uid, Long curtime){
		Long sensortime = curtime;
		try {
			sensortime = getSensorTime(brethOrderId, 0, uid, curtime);
		} catch (Exception e) {
			logger.error("getOrderTime", e);
		}
		return sensortime;
	}
	
	/**
	 * ��ȡ��������������ΪPOS���������볡ʱ��
	 * @param berthOrderId �������������
	 * @param type 0������ 1������
	 * @param uid �շ�Ա���
	 * @param curtime ��ǰϵͳʱ��
	 * @return
	 */
	private Long getSensorTime(Long berthOrderId, Integer type, Long uid, Long curtime){
		//logger.error("getSensorTime>>>berthOrderId:"+berthOrderId+",type:"+type+",uid:"+uid+",curtime:"+curtime);
		Long sensortime = curtime;
		try {
			if(uid != null && uid > 0){
				Map<String, Object> setMap = pService.getMap("select is_sensortime from collector_set_tb s,user_info_tb u " +
						" where s.role_id=u.role_id and u.id=? ", new Object[]{uid});
				if(setMap != null){
					Integer is_sensortime = (Integer)setMap.get("is_sensortime");
					//logger.error("getSensorTime>>>berthOrderId:"+berthOrderId+",is_sensortime:"+is_sensortime);
					if(is_sensortime == 1){
						return sensortime;
					}
				}
				if(berthOrderId != null && berthOrderId > 0){
					Map<String, Object> map = pService.getMap("select * from berth_order_tb " +
							" where id=? ", new Object[]{berthOrderId});
					logger.error("getSensorTime>>>berthOrderId:"+berthOrderId+",map:"+map);
					if(map != null){
						if(type == 0){
							if(map.get("in_time") != null){
								Long in_time = (Long)map.get("in_time");
								if(curtime > in_time && (curtime - in_time < 10 * 60)){
									sensortime = in_time;
								}else{
									logger.error("berthOrderId:"+berthOrderId+",uid:"+uid+",curtime:"+curtime+",in_time:"+in_time);
								}
							}
						}else if(type == 1){
							if(map.get("out_time") != null){
								Long out_time = (Long)map.get("out_time");
								if(curtime > out_time && (curtime - out_time < 10 * 60)){
									sensortime = out_time;
								}else{
									logger.error("berthOrderId:"+berthOrderId+",uid:"+uid+",curtime:"+curtime+",out_time:"+out_time);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("getSensorTime>>>berthOrderId:"+berthOrderId,e);
		}
		return sensortime;
	}
	
	/**
	 * ����POS��������ȡ������������ţ�����鵽�а󶨳�����������ȡ�󶨵ĳ������������
	 * @param orderId	POS���������
	 * @param berthOrderId	�������������
	 * @return
	 */
	public Long getBerthOrderId(Long orderId){
		Long berthOrderId = -1L;
		try {
			//logger.error("getBerthOrderId>>>orderId:"+orderId);
			if(orderId != null && orderId > 0){
				Map<String, Object> berthOrderMap = pService.getMap("select id from berth_order_tb " +
						" where orderid=? order by in_time desc limit ? ", new Object[]{orderId, 1});
				if(berthOrderMap != null){
					berthOrderId = (Long)berthOrderMap.get("id");
				}
			}
			logger.error("getBerthOrderId>>>orderId:"+orderId+",berthOrderId:"+berthOrderId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return berthOrderId;
	}
	
	/**
	 * ȡ�ò�λ���µ�һ����Ч�ĳ���������
	 * @param berthId
	 * @return
	 */
	public Long getPreBerthOrderId(Long berthId){
		Long berthOrderId = -1L;
		try {
			if(berthId != null && berthId > 0){
				Long count = pService.getLong("select count(d.id) from com_park_tb p,dici_tb d where p.dici_id=d.id " +
						" and p.id=? and d.state=? and d.is_delete=? ", new Object[]{berthId, 1, 0});
				logger.error("getPreBerthOrderId>>>berthId:"+berthId+",count:"+count);
				if(count > 0){
					Map<String, Object> map = pService.getMap("select id,state,orderid,bind_flag from " +
							" berth_order_tb where id=(select max(id) as maxid from berth_order_tb where dici_id=?) ", 
							new Object[]{berthId});
					logger.error("getPreBerthOrderId>>>berthId:"+berthId+",map:"+map);
					if(map != null){
						Integer state = (Integer)map.get("state");
						Integer bind_flag = (Integer)map.get("bind_flag");
						if(state == 0 && bind_flag == 1){
							Long orderid = (Long)map.get("orderid");
							if(orderid < 0){
								berthOrderId = (Long)map.get("id");
							}
						}
					}
				}
			}
			logger.error("getPreBerthOrderId>>>berthId:"+berthId+",berthOrderId:"+berthOrderId);
		} catch (Exception e) {
			logger.error("getPreBerthOrderId", e);
		}
		return berthOrderId;
	}
	
	public void writeToMongodb(String dbName,Map<String, String> paramMap){
		WriteResult result =null;
		try {
			DB mydb = MongoClientFactory.getInstance().getMongoDBBuilder("zld");
			mydb.requestStart();
			DBCollection collection = mydb.getCollection(dbName);
			BasicDBObject object = new BasicDBObject();
			for(String key : paramMap.keySet()){
				object.put(key, paramMap.get(key));
			}
			object.put("ctime", System.currentTimeMillis()/1000);
			mydb.requestStart();
			result = collection.insert(object);
			  //��������
			mydb.requestDone();
		} catch (Exception e) {
			logger.error("write to monbodb error...."+result);
			e.printStackTrace();
		}
	}
	
	/**
	 * ��ȡ��λʣ����
	 * @param comid
	 * @param type 0���볡 1������
	 * @return
	 */
	public Long updateRemainBerth(Long comid, Integer type){
		Long remain = 0L;
		try {
			logger.error("remain berth>>>comid:"+comid+",type:"+type);
			Long ntime = System.currentTimeMillis()/1000;
			Map<String, Object> comMap = pService.getMap("select parking_total,share_number,invalid_order from com_info_tb where id=? and parking_type<>? and etc=? ", 
					new Object[]{comid, 2, 2});//parking_type -- ��λ���ͣ�0���棬1���£�2ռ�� 3���� 4���� 5������
			if(comMap == null){
				return null;
			}
			Integer parking_total = 0;
			Integer share_number = 0;
			Long invalid_order = 0L;//δ���������������
			if(comMap.get("parking_total") != null){
				parking_total = (Integer)comMap.get("parking_total");
			}
			if(comMap.get("share_number") != null){
				share_number = (Integer)comMap.get("share_number");
			}
			if(comMap.get("invalid_order") != null){
				invalid_order = (Long)comMap.get("invalid_order");
			}
			if(share_number == 0){
				share_number = parking_total;
			}
			logger.error("park info>>>comid:"+comid+",parking_total:"+parking_total+",share_number:"+share_number+",invalid_order:"+invalid_order);
			Map<String, Object> remainMap = daService.getMap("select amount from remain_berth_tb where comid=? and state=? and berthseg_id<? limit ?", 
					new Object[]{comid, 0, 0, 1});
			
			if(remainMap != null){
				remain = (Long)remainMap.get("amount");
				if(type == 0){//�볡
					remain--;
				}else if(type == 1){
					remain++;
				}
			}else{//��ճ���,��ʼ����λ��
				remain = getValidUseCount(comid, share_number, invalid_order);
			}
			if(remain < 0){
				remain = 0L;
			}
			logger.error("remain berth >>>comid:"+comid+",remain:"+remain);
			if(remainMap == null){
				int ret = daService.update("insert into remain_berth_tb(comid,amount,update_time) values(?,?,?) ", 
						new Object[]{comid, remain, ntime});
				logger.error("update remain berth >>>comid:"+comid+",remain:"+remain+",ret:"+ret);
			}else{
				int ret = daService.update("update remain_berth_tb set amount=?,update_time=? where comid=? and berthseg_id<? ", 
						new Object[]{remain, ntime, comid, 0});
				logger.error("update remain berth >>>comid:"+comid+",remain:"+remain+",ret:"+ret);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return remain;
	}
	
	public Long getValidUseCount(Long comid, Integer share_number, Long invalid_order){
		Long ntime = System.currentTimeMillis()/1000;
		long time2 = ntime - 2*24*60*60;
		long time16 = ntime - 16*24*60*60;
		Long month_used_count = 0L;
		Long time_used_count = 0L;
		String sql = "select count(ID) ucount,c_type from order_tb where comid=? and create_time>? and state=? group by c_type ";
		List<Map<String, Object>> allList = pService.getAll(sql, new Object[]{comid,time2,0});
		if(allList != null && !allList.isEmpty()){
			for(Map<String, Object> map : allList){
				Integer c_type = Integer.valueOf(String.valueOf(map.get("c_type")));
				Long ucount = (Long)map.get("ucount");
				if(c_type == 5 || c_type == 7 || c_type == 8){//�¿���λռ����
					month_used_count += ucount;
				}else{//ʱ�⳵λռ����
					time_used_count += ucount;
				}
			}
		}
		
		Long invmonth_used_count = 0L;
		Long invtime_used_count = 0L;
		String sql1 = "select count(ID) ucount,c_type from order_tb where comid=? and create_time>? and create_time<? and state=? group by c_type ";
		List<Map<String, Object>> invList = pService.getAll(sql1, new Object[]{comid,time16,time2,0});
		if(invList != null && !invList.isEmpty()){
			for(Map<String, Object> map : invList){
				Integer c_type = Integer.valueOf(String.valueOf(map.get("c_type")));
				Long ucount = (Long)map.get("ucount");
				if(c_type == 5 || c_type == 7 || c_type == 8){//�¿���λռ����
					invmonth_used_count += ucount;
				}else{//ʱ�⳵λռ����
					invtime_used_count += ucount;
				}
			}
		}
		//******************���������ܹ�����������������*****************************//
		int inv_month = (int) (invmonth_used_count*2/14);
		int inv_time = (int) (invtime_used_count*2/14);
		
		if(month_used_count >= inv_month){
			month_used_count -= inv_month;
		}else{
			month_used_count = 0L;
		}
		
		if(time_used_count >= inv_time){
			time_used_count -= inv_time;
		}else{
			time_used_count = 0L;
		}
		//********************��ȥƫ����*********************************//
		double rate = 0;
		if(month_used_count + time_used_count > 0){
			rate = (double)month_used_count/(month_used_count + time_used_count);//ƫ��������
		}
		Long month_offset = Math.round(invalid_order * rate);//�¿���λռ��ƫ����
		month_used_count = month_used_count - month_offset;//�¿���ȥ��ƫ�������ռ�ó�λ��
		time_used_count = time_used_count - (invalid_order - month_offset);//ȥ��ƫ�������ʱ�⳵ռ�ó�λ��
		if(month_used_count < 0){
			month_used_count = 0L;
		}
		if(time_used_count < 0){
			time_used_count = 0L;
		}
		Long useCount = month_used_count + time_used_count;
		if(useCount > share_number){
			share_number = useCount.intValue();
		}
		Long remain = share_number - useCount;
		logger.error("getValidUseCount>>>comid:"+comid+",useCount:"+useCount+",share_number:"+share_number);
		return remain;
	}
	
	/**
	 * @param orderid �������
	 * @param state ֧��״̬1�ɹ��������ɹ�
	 * @param money ֧�����
	 * @return 1�ɹ���-1֧�����ɹ� 0������Ų�����
	 */
	public String sendOrderState2Baohe(Long orderid,int state,Double money,Integer paytype){
		HttpProxy hProxy = new HttpProxy();
		//http://www.bouwa.org/api/services/p4/Business/GetOnlinePayStatus?
		String url = "http://www.bouwa.org/api/services/p4/Business/GetOnlinePayStatus?orderid="+orderid+"&status="+state+"&money="+money+"&paytype="+paytype;
		String result ="0";
		try {
			String ret = hProxy.doGet(url);
			if (ret != null) {
				JSONObject object = new JSONObject(ret);
				//{"Success":true,"Result":"1","Error":null,"UnAuthorizedRequest":false,"rows":null,"total":0}
				//Result:1�ɹ���-1֧�����ɹ� 0������Ų�����
				result = object.getString("Result");
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return result;
	}
	
	public String sendPrepay2Baohe(Long orderid,int state,Double money,Integer paytype){
		HttpProxy hProxy = new HttpProxy();
		//http://www.bouwa.org/api/services/p4/Business/GetOnlinePayStatus?
		String url = "http://www.bouwa.org/api/services/p4/Business/GetOnLinePrepaidPayStatus?orderid="+orderid+"&status="+state+"&money="+money+"&paytype="+paytype;
		String result ="0";
		try {
			String ret = hProxy.doGet(url);
			System.out.println(ret);
			if (ret != null) {
				JSONObject object = new JSONObject(ret);
				//{"Success":true,"Result":"1","Error":null,"UnAuthorizedRequest":false,"rows":null,"total":0}
				//Result:1�ɹ���-1֧�����ɹ� 0������Ų�����
				result = object.getString("Result");
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return result;
	}
	
	public Integer isHd(Long comId) {
		Map comMap = daService.getMap("select order_per from com_info_tb where id =? ", new Object[]{comId});
		Integer per = (Integer)comMap.get("order_per");
		if(per!=null&&per>0){
			Integer i = new Random().nextInt(100);
			if(i<per)
				return 1;
		}
		return 0;
	}
	
	
	//���³�λ��Ϣ�������ѽ��㶩����ռ�ó�λ
//	public void updateParkInfo(Long comId) {
//		int r =daService.update("update com_park_tb set state =?,order_id=? where order_id in " +
//				"(select id from order_tb where state in(?,?) and id in(select order_id from com_park_tb where comid=?)) ",
//				new Object[]{0,null,1,2,comId});
//		logger.error(comId+"��������"+r+"����λ��Ϣ");
//	}
	
	
	//��ѯ���
	public boolean checkBonus(String mobile,Long uin){
		List bList = daService.getAll("select * from bonus_record_tb where mobile=? and state=? ",new Object[]{mobile,0});
		String tsql = "insert into ticket_tb (create_time,limit_day,money,state,uin,type) values(?,?,?,?,?,?) ";
		List<Object[]> values = new ArrayList<Object[]>();
		if(bList!=null&&bList.size()>0){
			Long bid = null;
			for(int i=0;i<bList.size();i++){
				Map map = (Map)bList.get(i);
				Long _bid = (Long)map.get("bid");
				if(_bid!=null&&_bid>0)
					bid = _bid;
				Integer money = (Integer)map.get("amount");
				
				Integer type = (Integer)map.get("type");
				Long ctime = TimeTools.getToDayBeginTime();//(Long)map.get("ctime");
				Long etime = ctime+6*24*60*60-1;
				
				if(type==1){//΢�Ŵ���ȯ
					values.add(new Object[]{ctime,etime,money,0,uin,2});
				}else {//��ͨͣ��ȯ
					if(money==30||money==100){//3��10Ԫȯ
						if(money==30){
							values.add(new Object[]{ctime,etime,4,0,uin,0});
							values.add(new Object[]{ctime,etime,4,0,uin,0});
							values.add(new Object[]{ctime,etime,1,0,uin,0});
							values.add(new Object[]{ctime,etime,1,0,uin,0});
							values.add(new Object[]{ctime,etime,3,0,uin,0});
							values.add(new Object[]{ctime,etime,3,0,uin,0});
							values.add(new Object[]{ctime,etime,2,0,uin,0});
							values.add(new Object[]{ctime,etime,2,0,uin,0});
							values.add(new Object[]{ctime,etime,4,0,uin,0});
							values.add(new Object[]{ctime,etime,1,0,uin,0});
							values.add(new Object[]{ctime,etime,3,0,uin,0});
							values.add(new Object[]{ctime,etime,2,0,uin,0});
						}else {
							int end = 10;
							for(int j=0;j<end;j++){
								values.add(new Object[]{ctime,etime,10,0,uin,0});
							}
						}
					}else if(money==10){//1��10Ԫȯ
						values.add(new Object[]{ctime,etime,4,0,uin,0});
						values.add(new Object[]{ctime,etime,1,0,uin,0});
						values.add(new Object[]{ctime,etime,3,0,uin,0});
						values.add(new Object[]{ctime,etime,2,0,uin,0});
					}else {
						Object[] v1 = new Object[]{ctime,etime,money,0,uin,0};
						values.add(v1);
					}
				}
			}
			if(values.size()>0){
				int ret= daService.bathInsert(tsql, values, new int[]{4,4,4,4,4,4});
				logger.error("�˻�:"+uin+",�ֻ���"+mobile+",�û���¼ ��д����ͣ��ȯ"+ret+"��");
				logger.error(">>>>�û�������ȯ�����º����¼��"+daService.update("update bonus_record_tb set state=? where mobile=?", new Object[]{1,mobile}));
				if(ret>0){
					//���³���ע��ý����Դ 0������ע�ᣬ1-997�Ƕ��ƺ����1����ͷ���������������2�������,3���պ��.4.����ͷ������أ�����998ֱ�����,999���շ�Ա�Ƽ���1000�����ǳ������������
					if(bid!=null&&bid>0){
						Integer media = 0;
						if(bid>999){//1000���ϵı���ǳ������������������Ϊ���ƺ������д���û���
							media=1000;
						}else {
							media = bid.intValue();
						}
						if(media>0){//����ý����Դ
							daService.update("update user_info_tb set media=? where id=? ", new Object[]{media,uin});
						}
					}
					return true;
				}
			}
		}else {
			logger.error("�˻�:"+uin+",�ֻ���"+mobile+",û�к��....");
		}
		return false;
	}
	/**
	 * ȡ����ͣ��ȯ��δ��֤�������ʹ��3Ԫȯ��
	 * 	 * 9Ԫ��ͣ���ѣ� Ҳ����ʹ��18Ԫ��ͣ��ȯ����ֻ�ֿܵ�8Ԫ��  
		���8����Ƕ�̬�ķ�������ȡ����Ϊ�п���ѹ�������������Ż�ȯֻ�ֿۣܵ�ͣ����-2����8�ͱ�Ϊ7�ˡ�
	 * @param uin
	 * @param fee
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getTickets(Long uin,Double fee,Long comId,Long uid){
		//������п��õ�ȯ
		//Long ntime = System.currentTimeMillis()/1000;
		Integer limit = CustomDefind.getUseMoney(fee,0);
		Double splimit = StringUtils.formatDouble(CustomDefind.getValue("TICKET_LIMIT"));
		boolean blackuser = isBlackUser(uin);
		boolean blackparkuser =false;
		if(comId!=null)
			blackparkuser=publicMethods.isBlackParkUser(comId, false);
		boolean isauth = publicMethods.isAuthUser(uin);
		if(!isauth){
			if(blackuser||blackparkuser){
				if(blackuser){
					logger.error("�����ں�������uin:"+uin+",fee:"+fee+",comid:"+comId);
				}
				if(blackparkuser){
					logger.error("�����ں�������uin:"+uin+",fee:"+fee+",comid:"+comId);
				}
				return null;
			}
		}else{
			logger.error("����uin:"+uin+"����֤��������ȯ���ж��Ƿ��Ǻ������������Ƿ��������");
		}
		List<Map<String, Object>> list = null;
		double ticketquota=-1;
		if(uid!=-1){
			Map usrMap =daService.getMap("select ticketquota from user_info_Tb where id =? and ticketquota<>?", new Object[]{uid,-1});
			if(usrMap!=null){
				ticketquota = Double.parseDouble(usrMap.get("ticketquota")+"");
			}
		}
		logger.error("���շ�Ա:"+uid+"����ȯ����ǣ�"+ticketquota+"��(-1����û����)");
		if(!isauth){//δ��֤�������ʹ��2Ԫȯ��
			double noAuth = 1;//δ��֤�����������noAuth(2)Ԫȯ,�Ժ�Ķ����ֵ��ok
			if(ticketquota>=0&&ticketquota<=noAuth){
//				ticketquota = ticketquota+1;
			}else{
				ticketquota=noAuth;
			}
			list=	daService.getAll("select * from ticket_tb where uin = ? " +
					"and state=? and limit_day>=? and type<? and money<?  order by limit_day",
					new Object[]{uin,0,TimeTools.getToDayBeginTime(),2,ticketquota+1});

		}else {
			list  = daService.getAll("select * from ticket_tb where uin = ? " +
					"and state=? and limit_day>=? and type<=?  order by limit_day",
					new Object[]{uin,0,TimeTools.getToDayBeginTime(),2});
		}
		logger.error("uin:"+uin+",fee:"+fee+",comid:"+comId+",today:"+TimeTools.getToDayBeginTime());
		if(list!=null&&!list.isEmpty()){
			List<String> _over3day_moneys = new ArrayList<String>();
			int i=0;
			for(Map<String, Object> map : list){
				Integer money = (Integer)map.get("money");
				//Long limit_day = (Long)map.get("limit_day");
				Long tcomid = (Long)map.get("comid");
				Integer type = (Integer)map.get("type");
//				logger.error("ticket>>>uin:"+uin+",comId:"+comId+",tcomid:"+tcomid+",type:"+type+",ticketid:"+map.get("id"));
				if(comId!=null&&comId!=-1&&tcomid!=null&&type == 1){
					if(comId.intValue()!=tcomid.intValue()){
						logger.error(">>>>get ticket:�������������ͣ��ȯ��������....comId:"+comId+",tcomid:"+tcomid+",uin:"+uin);
						i++;
						continue;
					}
				}
				Integer res = (Integer)map.get("resources");
				if(limit==0&&res==0&&type==0){//֧�����С��3Ԫ��������ͨȯ
					i++;					
					continue;
				}
				if(type==1||res==1){
					limit=Double.valueOf((fee-splimit)).intValue();
				}else {
					limit= CustomDefind.getUseMoney(fee,0);
				}
				map.put("isbuy", res);
				if(money==limit){//ȯֵ+1Ԫ ���� ֧�����ʱֱ�ӷ���
					return map;
				}
				//�ж� �Ƿ� �� ���Ǹó�����ר��ȯ
				
				map.remove("comid");
//				map.remove("limit_day");
				_over3day_moneys.add(i+"_"+Math.abs(limit-money));
				i++;
			}
			if(_over3day_moneys.size()>0){//ͣ��ȯ��ͣ���ѵľ���ֵ���� ��ȡ����ֵ��С��
				int sk = 0;//����index
				double sv=0;//������Сֵ
				int index = 0;
				for(String s : _over3day_moneys){
					int k = Integer.valueOf(s.split("_")[0]);
					double v = Double.valueOf(s.split("_")[1]);
					if(index==0){
						sk=k;
						sv = v;
					}else {
						if(sv>v){
							sk=k;
							sv = v;
						}
					}
					index++;
				}
				logger.error("uin:"+uin+",comid:"+comId+",sk:"+sk);
				return list.get(sk);
			}
		}else{
			logger.error("δѡ��ȯuin:"+uin+",comid:"+comId+",fee:"+fee);
		}
		return null;
	}
	
	/**�Ƿ��ں�����*/
	public boolean isBlackUser(Long uin){
		List<Long> blackUserList = memcacheUtils.doListLongCache("zld_black_users", null, null);
		boolean isBlack = true;
		if(blackUserList==null||!blackUserList.contains(uin))//���ں������п��Դ����Ƽ�����
			isBlack=false;
		return isBlack;
	}
	
	/**
	 * ����openid��ȡ�û���Ϣ
	 * @param openid
	 * @return
	 */
	public Map<String, Object> getUserByOpenid(String openid){
		Map<String, Object> userMap = daService.getMap("select * from user_info_tb where wxp_openid=? limit ? ",
				new Object[] { openid, 1 });
		return userMap;
	}
	
	/**
	 * ����openid��ȡ�û�����Ϣ
	 * @param openid
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getUserinfoByOpenid(String openid){
		Map<String, Object> map = new HashMap<String, Object>();
		Integer bindflag = 0;
		Long uin = -1L;
		String mobile = "";
		Double balance = 0d;
		Map<String, Object> userMap = daService.getMap("select * from user_info_tb where wxp_openid=? limit ? ",
				new Object[] { openid, 1 });
		if(userMap != null){
			bindflag = 1;
			uin = (Long)userMap.get("id");
			mobile = (String)userMap.get("mobile");
			balance = Double.valueOf(userMap.get("balance") + "");
		}else{
			userMap = daService.getMap("select * from wxp_user_tb where openid=? limit ? ", new Object[]{openid, 1});
			if(userMap == null){
				uin = daService.getLong("SELECT nextval('seq_user_info_tb'::REGCLASS) AS newid",null);
				int r = daService.update("insert into wxp_user_tb(openid,create_time,uin) values(?,?,?) ",
								new Object[] { openid, System.currentTimeMillis() / 1000, uin});
				logger.error("û����ʱ�˻�������һ��uin:"+uin+",openid:"+openid+",r:"+r);
			}else{
				uin = (Long)userMap.get("uin");
				balance = Double.valueOf(userMap.get("balance") + "");
			}
		}
		map.put("bindflag", bindflag);
		map.put("uin", uin);
		map.put("mobile", mobile);
		map.put("balance", balance);
		logger.error("add car:map:"+map);
		return map;
	}
	
	/**
	 * ��ȡ������һ�����ƺ�
	 * @param uin
	 * @param bindflag
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String getCarnumber(Long uin, Integer bindflag){
		String carnumber = "";
		if(bindflag == 0){//��ʱ�˻�
			Map<String, Object> carMap = daService.getMap("select car_number from wxp_user_tb where uin= ? limit ? ", 
					new Object[]{uin, 1});
			if(carMap != null && carMap.get("car_number") != null){
				carnumber = (String)carMap.get("car_number");
			}
		}else if(bindflag == 1){
			Map<String, Object> carMap = daService.getMap("select car_number from car_info_tb where uin=? and state=? limit ?",
					new Object[] { uin, 1, 1 });
			if(carMap != null && carMap.get("car_number") != null){
				carnumber = (String)carMap.get("car_number");
			}
		}
		return carnumber;
	}
	
	/**
	 * ɨ����ȯ����ȡ����ǰ���ͣ���ѽ��
	 * @param orderMap
	 * @param shopTicketMap
	 * @param delaytime Ԥ֧����ʱʱ��
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> getPrice(Long orderId, Long shopTicketId, Long end_time){
		try {
			Map<String, Object> orderMap = daService.getMap("select * from order_tb where id=? ", 
					new Object[]{orderId});
			Map<String, Object> shopTicketMap = daService.getMap("select * from ticket_tb where id=? ", 
					new Object[]{shopTicketId});
			Map<String, Object> map = new HashMap<String, Object>();
			Integer state = (Integer)orderMap.get("state");
			Long comid = (Long)orderMap.get("comid");
			Double beforetotal = 0d;//ʹ�ü���ȯ֮ǰ��ͣ���ѽ��
			Double aftertotal = 0d;//ʹ�ü���ȯ֮���ͣ���ѽ��
			Double distotal = 0d;//����ȯ�ֿ۵Ľ��
			Integer distime = 0;//�ֿ۵�ʱ��
			//Integer car_type = (Integer)orderMap.get("car_type");//0��ͨ�ã�1��С����2����
			//String car_type = (String) orderMap.get("car_type");
			Integer pid = (Integer)orderMap.get("pid");
			Long create_time = (Long)orderMap.get("create_time");
			if(state == 0){//δ���㶩��
				beforetotal = getPrice(null, pid, comid, create_time, end_time,orderId);
			}else if(orderMap.get("total") != null){//�ѽ�������ӵ�
				beforetotal = Double.valueOf(orderMap.get("total") + "");
			}
			if(shopTicketMap != null){
				Integer type = (Integer)shopTicketMap.get("type");
				if(type == 3){//��ʱȯ
					Integer time = (Integer)shopTicketMap.get("money");
					if(end_time > create_time + time *60 *60){
						aftertotal = getPrice(null, pid, comid, create_time, end_time - time * 60 *60,orderId);
						distime = time * 60 * 60;
					}else if(end_time > create_time){
						distime = (end_time.intValue() - create_time.intValue());
					}
				}else if(type == 4){//���ȯ
					if(end_time > create_time){
						distime = (end_time.intValue() - create_time.intValue());
					}
				}
			}else{
				aftertotal = beforetotal;
			}
			
			if(beforetotal > aftertotal){
				distotal = StringUtils.formatDouble(beforetotal - aftertotal);
			} else {//���ǵ��ѽ�������ӵ���������ڼ��п��ܼ۸�䶯�����¼����ļ۸񷴶��ȼ���ǰ�Ļ���
				aftertotal = beforetotal;
			}
			List<Map<String , Object>> sqlMaps = new ArrayList<Map<String,Object>>();
			if(shopTicketMap != null){
				Integer need_sync = -1;//Ĭ��-1
				if(publicMethods.isEtcPark(comid)){//˫����������������ɨ�����ȯ�����  0��Ҫͬ����ȥ
					need_sync = 0;
				}
				Map<String, Object> ticketsqlMap = new HashMap<String, Object>();
				ticketsqlMap.put("sql", "update ticket_tb set umoney=?,bmoney=?,need_sync=? where id=? ");
				ticketsqlMap.put("values", new Object[]{distotal, Double.valueOf(distime)/(60*60), need_sync, shopTicketId});
				sqlMaps.add(ticketsqlMap);
			}
			boolean b = daService.bathUpdate2(sqlMaps);
			logger.error("orderid:"+orderId+",b:"+b);
			map.put("beforetotal", beforetotal);
			map.put("aftertotal", aftertotal);
			map.put("distotal", distotal);
			return map;
		} catch (Exception e) {
			logger.error("getPrice", e);
		}
		return null;
	}
	
	/**
	 * ���ݶ�����Ϣ��ȡ���ѽ��
	 * @param car_type
	 * @param pid
	 * @param comid
	 * @param create_time
	 * @param end_time
	 * @return
	 */
	public Double getPrice(Integer car_type, Integer pid, Long comid, Long create_time, Long end_time, Long orderid){
		Double total = 0d;
		if(pid>-1){
			total = Double.valueOf(publicMethods.getCustomPrice(create_time, end_time, pid));
		}else {
			Map orderMap = daService.getMap("select * from order_tb where id = ?", new Object[]{orderid});
			//ģ���ѽ���
			orderMap.put("end_time", end_time);
			orderMap.put("state", 1);
			String result = "";
			try {
				result = publicMethods.getOrderPrice(comid, orderMap, end_time);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//getPrice(create_time, end_time, comid, car_type));
			net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(result);
			if(jsonObject.get("collect")!=null){
				total = StringUtils.formatDouble(jsonObject.get("collect"));
			}
		}
		return total;
	}
	
	/**
	 * ��ȡ������Ϣ
	 * @param orderId
	 * @param shopTicketId
	 * @param end_time
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getOrderInfo(Long orderId, Long shopTicketId, Long end_time){
		try {
			Map<String, Object> orderMap = daService.getMap("select * from order_tb where id=? ", 
					new Object[]{orderId});
			if(orderMap == null){
				return null;
			}
			Integer state = (Integer)orderMap.get("state");
			if(state == 1 || state == 2){//��������ѽ�������ӵ���ʹ�ö�������ʱ��
				if(orderMap.get("end_time") != null){
					end_time = (Long)orderMap.get("end_time");
				}
			}
			Long create_time = (Long)orderMap.get("create_time");
			Integer tickettype = 3;//����ȯ���ͣ�Ĭ�ϼ�ʱȯ
			Integer tickettime = 0;//��ʱȯ��ʱ��
			Integer ticketstate = 0;//����ȯ��״̬��0�������� 1:����
			Map<String, Object> shopTicketMap = daService.getMap("select * from ticket_tb where orderid=? " +
					" and (type=? or type=?) ", new Object[]{orderId, 3, 4});
			if(shopTicketMap == null){//���û�а󶨼���ȯ���Զ��ļ����û�п��õļ���ȯ
				if(shopTicketId != null && shopTicketId > 0){
					shopTicketMap = daService.getMap("select * from ticket_tb where id=? and (orderid=? or orderid=?) " +
							" and state=? and (type=? or type=?) and limit_day>? ", 
							new Object[]{shopTicketId, -1, orderId, 0, 3, 4, end_time});
				}
			}else{
				shopTicketId = (Long)shopTicketMap.get("id");
			}
			if(shopTicketMap != null){
				int r = daService.update("update ticket_tb set orderid=? where id=? ", 
						new Object[]{orderId, shopTicketId});
				tickettype = (Integer)shopTicketMap.get("type");
				tickettime = (Integer)shopTicketMap.get("money");//����ȯ�����(XXСʱ)
				ticketstate = 1;//�ü���ȯ����
				logger.error("orderId:"+orderId+",r:"+r);
			}
			Map<String, Object> map2 = getPrice(orderId, shopTicketId, end_time);
			map2.putAll(orderMap);
			map2.put("ticketstate", ticketstate);
			map2.put("tickettype", tickettype);
			map2.put("tickettime", tickettime);
			map2.put("shopticketid", shopTicketId);
			map2.put("starttime", TimeTools.getTime_yyyyMMdd_HHmm(create_time * 1000));
			map2.put("parktime", StringUtils.getTimeString(create_time, end_time));
			map2.put("pretotal", orderMap.get("prepaid"));
			map2.put("end_time", end_time);//��¼��ǰʱ��
			return map2;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
	
	/**
	 * �����û�ID��ȡ����ʱ�˻�������ʽ�˻�
	 * @param uin
	 * @return
	 */
	public Integer getBindflag(Long uin){
		Long count = daService.getLong("select count(1) from user_info_tb where id=? and auth_flag=? ", new Object[]{uin,4});
		return count.intValue();
	}
	
	public Integer addCarnumber(Long uin, String carnumber){
		Long curTime = System.currentTimeMillis()/1000;
		Integer bindflag = getBindflag(uin);
		if(bindflag == 1){
			Long count = daService.getLong("select count(*) from car_info_tb where uin!=? and car_number=? ",
					new Object[] { uin, carnumber });
			if(count > 0){//�ó��ƺ��ѱ�����ע��
				//ɾ���ó���
				int del = daService.update("delete from car_info_tb where uin!=? and car_number = ?", new Object[]{uin,carnumber});
				logger.error("addCarnumber>>>ɾ������:(�ɹ�1/ʧ��0)"+del);
				/*if(del>0){
					publicMethods.syncUserPlateNumberDelete(uin, carnumber);
				}*/
				//return -1;
			}
			count = daService.getLong("select count(*) from car_info_tb where uin=? and car_number=? ",
					new Object[] { uin, carnumber});
			if(count > 0){//�ó����Ѿ�ע����ó��ƺ�
				//����ʱ��
				int update = daService.update("update car_info_tb set create_time = ? where uin = ? and car_number = ?", new Object[]{curTime,uin,carnumber});
				if(update>0){
					logger.error("addCarnumber>>>����ʱ��"+update);
					
					return 1;
				}
			}else{
				/*count = daService.getLong("select count(*) from car_info_tb where uin=? ",
						new Object[] { uin });
				if(count >= 3){//�ó���ע��ĳ��ƺŵĸ���
					return -3;
				}*/
				int r=daService.update("insert into car_info_Tb (uin,car_number,create_time) values(?,?,?)", 
						new Object[]{uin, carnumber, curTime});
				logger.error("addCarnumber>>>add userinfotb:"+r);
				if(r > 0){
					publicMethods.syncUserAddPlateNumber(uin, carnumber, "");
					return 1;
				}
			}
		}else if(bindflag == 0){
			//�޸ı��β����û��ĳ��ƺ�,��ͬ��������
			int update = daService.update("update wxp_user_tb set car_number = ? where car_number = ? ", new Object[]{"",carnumber});
			logger.error("��ͬ��΢���û���������"+update);
			Map map = daService.getMap("select car_number from wxp_user_tb where uin = ?", new Object[]{uin});
			String oldPlateNumber = "";
			if(map!=null){
				oldPlateNumber = (String) map.get("car_number");
			}
			int r = daService.update("update wxp_user_tb set car_number=? where uin=? ", 
					new Object[]{carnumber, uin});
			logger.error("addCarnumber>>>add wxpuserinfo:"+r);
			if(r > 0){
				//ȥ�������³���
				if(!StringUtils.isNotNull(oldPlateNumber)){
					publicMethods.syncUserAddPlateNumber(uin, carnumber, "");//���
				}else{
					publicMethods.syncUserAddPlateNumber(uin, oldPlateNumber, carnumber);//����
				}
				return 1;
			}
		}
		return -4;
	}
	
	public Integer checkProdExp(Long prodid, Long starttime, Integer months){
		Integer result = 0;//���������û�г�����Ʒ��Ч��
		if(prodid != null && prodid> 0){
			Map pMap = daService.getMap("select limitday,price from product_package_tb where id=? ", 
					new Object[]{prodid});
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(starttime*1000);
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH)+months);
			Long etime = calendar.getTimeInMillis()/1000;
					
			Long limitDay = null;//pMap.get("limitday");
			if(pMap!=null&&pMap.get("limitday")!=null){
				limitDay = (Long)pMap.get("limitday");
			}
			if(limitDay!=null){
				if(limitDay<etime){//������Ч��
					result = 1;
				}
			}
		}
		return result;
	}
	
	/**
	 * @param uin          �����˻�
	 * @param total        �������
	 * @return             ����ͣ��ȯ�б�
	 */
	public List<Map<String,Object>> getUseTickets(Long uin,Double total){
		Long time = System.currentTimeMillis()/1000;
		List<Map<String,Object>> ticketList=daService.getAll("select id,limit_day as limitday,money,resources," +
				"comid,type from ticket_tb where uin = ?" +
				" and limit_day >= ? and state=? and type<?  order by type desc,money,limit_day ",
				new Object[]{uin,time,0,2});
		
		Integer limit = CustomDefind.getUseMoney(total, 0);//��ͨȯ�ֿ۽��
		Integer sysLimit = Integer.valueOf(CustomDefind.getValue("TICKET_LIMIT"));//ר��ȯ������ȯ����붩���Ĳ��
		if(ticketList!=null&&!ticketList.isEmpty()){
			for(Map<String,Object> map:ticketList){
				Integer money = (Integer)map.get("money");
				Integer res = (Integer)map.get("resources");
				Integer topMoney = CustomDefind.getUseMoney(money.doubleValue(), 1);
				Integer type=(Integer)map.get("type");
				if(res==1||type==1){//����ר��ȯ����ȯ
					topMoney = money+sysLimit;
					limit = total.intValue()-sysLimit;
				}else {
					limit = CustomDefind.getUseMoney(total, 0);
					
				}
				if(topMoney<total){//����޶�С��֧�����
					limit=money;
				}
				map.put("limit", limit);
			}
		}
		//logger.error(ticketList);
		return ticketList;
	}
	
	//----------------����ȯѡȯ�߼�begin--------------------//
	/**
	 * ѡ�����ȯ
	 * @param uin
	 * @param uid
	 * @param total
	 * @return
	 */
	public Map<String, Object> chooseDistotalTicket(Long uin, Long uid, Double total){
		double firstorderquota = 8.0;//Ĭ�϶��
		double ditotal = 0d;//���۶��
		double disquota = StringUtils.formatDouble(firstorderquota * ditotal);//�����ۺ�ĵֿ۽��
		
		logger.error("ѡ�ۿ�ȯuin:"+uin+",uid:"+uid+",disquota:"+disquota+",firstorderquota:"+firstorderquota+",total:"+total);
		Map<String, Object> userMap2 = daService.getMap("select comid,firstorderquota from user_info_tb where id = ? ", new Object[]{uid});
		if(userMap2!=null){
			firstorderquota = Double.valueOf(userMap2.get("firstorderquota") + "");
			disquota = StringUtils.formatDouble(firstorderquota * ditotal);
		}
		logger.error("ѡ�ۿ�ȯuin:"+uin+",uid:"+uid+",firstorderquota:"+firstorderquota+",disquota:"+disquota);
		Map<String, Object> ticketMap = new HashMap<String, Object>();
		ticketMap.put("id", -100);
		Double ticket_money = Double.valueOf(StringUtils.formatDouble(total*ditotal));
		if(ticket_money > disquota){
			ticket_money =disquota;
		}
		ticketMap.put("money", ticket_money);
		logger.error("uin:"+uin+",total:"+total+",ticketMap:"+ticketMap);
		return ticketMap;
	}
	
	/**
	 * ������²�Ʒ����
	 * @param prodId ���²�Ʒ���
	 * @param months ��������
	 * @return
	 */
	public Double getProdSum(Long prodId, Integer months){
		Double total = 0d;
		if(prodId != null && prodId > 0 && months != null && months > 0){
			Double price = 0d;
			Map<String, Object> pMap = daService.getMap("select limitday,price from product_package_tb where id=? ", 
					new Object[]{prodId});
			if(pMap!=null&&pMap.get("limitday")!=null){
				price = Double.valueOf(pMap.get("price")+"");
			}
			total = months*price;
		}
		return total;
	}
	//----------------����ȯѡȯ�߼�end--------------------//
	
	//----------------����ȯѡȯ�߼�begin--------------------//
	/**
	 * 
	 * @param uin
	 * @param total
	 * @param utype 0��ͨѡȯ��Ĭ�ϣ�1���ô������ֿ۽���ͣ��ȯ
	 * @param uid
	 * @param isAuth
	 * @param ptype 0�˻���ֵ��1���²�Ʒ��2ͣ���ѽ��㣻3ֱ��;4���� 5����ͣ��ȯ
	 * @param parkId
	 * @param source 0:���Կͻ���ѡȯ 1�����Թ��ں�ѡȯ
	 * @return
	 */
	public List<Map<String, Object>> chooseTicket(Long uin, Double total, Integer utype, Long uid, boolean isAuth, Integer ptype, Long parkId, Long orderId, Integer source){
		List<Map<String, Object>> list = null;
		if(ptype == 4){//����ѡȯ
			list = chooseRewardTicket(uin, total, isAuth, uid, utype, ptype, parkId, orderId, source);
		}else if(ptype == -1 || ptype == 2 || ptype == 3){
			list = chooseParkingTicket(uin, total, utype, uid, isAuth, ptype, parkId, orderId, source);
		}
		return list;
	}
	
	/**
	 * ͣ������ѡȯ
	 * @param uin
	 * @param total ͣ���ѽ��
	 * @param utype 0��ͨѡȯ��Ĭ�ϣ�1���ô������ֿ۽���ͣ��ȯ
	 * @param uid
	 * @param isAuth
	 * @param source 0:���Կͻ���ѡȯ 1�����Թ��ں�ѡȯ
	 * @return
	 */
	public List<Map<String, Object>> chooseParkingTicket(Long uin, Double total, Integer utype, Long uid, boolean isAuth, Integer ptype, Long parkId, Long orderId, Integer source){
		List<Map<String, Object>> list = null;
		boolean isCanUserTicket = memcacheUtils.readUseTicketCache(uin);
		logger.error("choose parking pay ticket>>>uin:"+uin+",total:"+total+",utype:"+utype+",uid:"+uid+",isAuth:"+isAuth+",isCanUserTicket:"+isCanUserTicket);
		if(isCanUserTicket){
			Double moneylimit = 9999d;//ѡȯ������
			Map<String, Object> uidMap =daService.getMap("select ticketquota from user_info_Tb where id =? and ticketquota<>?", new Object[]{uid,-1});
			if(uidMap != null){
				moneylimit = Double.parseDouble(uidMap.get("ticketquota")+"");
			}
			logger.error("uin:"+uin+",uid:"+uid+",moneylimit:"+moneylimit+",isAuth:"+isAuth);
			Integer tickettype = 2;//ѡȯ����
			if(!isAuth){
				if(source == 0){
					moneylimit = 0d;
				}else if(source == 1){
					moneylimit = 0d;
				}
			}
			logger.error("uin:"+uin+",uid:"+uid+",moneylimit:"+moneylimit+",isAuth:"+isAuth+",tickettype:"+tickettype);
			list = getLimitTickets(moneylimit, tickettype, uin, utype, ptype, uid, total, parkId, orderId);
		}
		return list;
	}
	
	/**
	 * ѡ����ȯ
	 * @param uin
	 * @param total
	 * @param isAuth
	 * @param uid
	 * @param utype 0��ͨѡȯ��Ĭ�ϣ�1���ô������ֿ۽���ͣ��ȯ
	 * @param ptype 0�˻���ֵ��1���²�Ʒ��2ͣ���ѽ��㣻3ֱ��;4���� 5����ͣ��ȯ
	 * @param source 0:���Կͻ���ѡȯ 1�����Թ��ں�ѡȯ
	 * @return
	 */
	public List<Map<String, Object>> chooseRewardTicket(Long uin, Double total, boolean isAuth, Long uid, Integer utype, Integer ptype, Long parkId, Long orderId, Integer source){
		List<Map<String, Object>> list = null;
		Map<Long, Long> tcacheMap =memcacheUtils.doMapLongLongCache("reward_userticket_cache", null, null);
		boolean isCanUserTicket=true;
		if(tcacheMap!=null){
			Long time = tcacheMap.get(uin);
			if(time!=null&&time.equals(TimeTools.getToDayBeginTime())){
				isCanUserTicket=false;
			}
			logger.error("today reward cache:"+tcacheMap.size()+",uin:"+uin+",uid:"+uid+",time:"+time+",todaybegintime:"+TimeTools.getToDayBeginTime());
		}
		logger.error("choose reward ticket:uin:"+uin+",uid:"+uid+",isCanUserTicket:"+isCanUserTicket+",isAuth:"+isAuth+",total:"+total);
		
		if(isCanUserTicket){
			Double moneylimit = 9999d;//ѡȯ������
			Integer tickettype = 1;//ѡȯ����
			if(!isAuth){
				if(source == 0){
					moneylimit = 0d;
				}else if(source == 1){
					moneylimit = 0d;
				}
			}
			list = getLimitTickets(moneylimit, tickettype, uin, utype, ptype, uid, total, parkId, orderId);
		}
		return list;
	}
	
	/**
	 * ����ͣ��ȯ�������ƺ�ͣ��ȯ�������ȡͣ��ȯ�б�
	 * @param moneylimit ͣ��ȯ�������
	 * @param tickettype ͣ��ȯ��������
	 * @param uin
	 * @param utype 0��ͨѡȯ��Ĭ�ϣ�1���ô������ֿ۽���ͣ��ȯ
	 * @param ptype 0�˻���ֵ��1���²�Ʒ��2ͣ���ѽ��㣻3ֱ��;4���� 5����ͣ��ȯ
	 * @param uid
	 * @param total ���
	 * @return
	 */
	private List<Map<String, Object>> getLimitTickets(Double moneylimit, Integer tickettype, Long uin, Integer utype, Integer ptype, Long uid, Double total, Long parkId, Long orderId){
		Integer resource = 1;//ֻ���ù���ȯ
		if(readAllowCache(parkId)){
			logger.error("already uplimit of allowance everyday>>>uin:"+uin+",orderid:"+orderId);
			resource = 1;
		}
		List<Map<String, Object>> list = daService.getAll("select * from ticket_tb where uin = ? and state=? and limit_day>=? and type<? and money<=? and resources>=?  order by money ",
				new Object[] { uin, 0, TimeTools.getToDayBeginTime(), tickettype, moneylimit, resource });
		list = chooseTicketByLevel(list, ptype, uid, total, utype, parkId, orderId);
		return list;
	}
	
	private boolean readAllowCache(Long comid){
		Double limit = memcacheUtils.readAllowLimitCacheByPark(comid);
		logger.error("comid:"+comid+",limit:"+limit);
		if(limit != null){//�л���
			Double allowmoney = memcacheUtils.readAllowCacheByPark(comid);
			logger.error("comid:"+comid+",allowmoney:"+allowmoney);
			Map<String, Object> comMap = daService.getMap(
					"select allowance from com_info_tb where id=? ",
					new Object[] { comid });
			if(comMap != null && comMap.get("allowance") != null){
				Double allowance = Double.valueOf(comMap.get("allowance") + "");
				logger.error("comid:"+comid+",allowance:"+allowance);
				if(allowance > 0){
					if(allowmoney >= allowance){
						return true;
					}
				}
			}
			if(allowmoney >= limit){//�鿴�Ƿ񳬹�ÿ�ղ�������
				return true;
			}
		}else{//û�а�������������Ĳ���,��ʱ�����ܵ���������
			Double allallowmoney = memcacheUtils.readAllowanceCache();
			if(CustomDefind.getValue("ALLOWANCE") != null){
				Double uplimit = Double.valueOf(CustomDefind.getValue("ALLOWANCE") + "");
				Double toDaylimit = getAllowance(TimeTools.getToDayBeginTime(), uplimit);
//				if(toDaylimit<1000||toDaylimit>uplimit)
//					toDaylimit=1000d;
				logger.error("���ղ����ܶ� ��allallowmoney:"+allallowmoney+",uplimit:"+uplimit+",toDaylimit:"+toDaylimit);
				if(allallowmoney >= toDaylimit){//���ղ����ܶ��Ѿ�����������
					return true;
				}
			}
		}
		return false;
	}
	
	//2015-11-05 ��ʼ��ÿ���100,��0ֹͣ
	private Double getAllowance(Long time,Double limit) {
		Long baseTime = 1446652800L;//2015-11-05
		Long abs = time-baseTime;
		Long t  = abs/(24*60*60);
		logger.error(">>>>>��2015-11-03��ʼ�������ݼ�100�ı�����"+t);
		if(t>0){
			Double retDouble= limit-t*100;
			if(retDouble<0d)
				retDouble=0d;
			return retDouble;
		}
		return limit;
	}
	
	/**
	 * @param ptype 0�˻���ֵ��1���²�Ʒ��2ͣ���ѽ��㣻3ֱ��;4���� 5����ͣ��ȯ
	 * @param uid   �շ�Ա���
	 * @param total ���ѽ��
	 * @param type  0�����ݽ�����ȯ�ֿ۽�� 1������ȯ���������������ѽ���ȫ��ֿ�
	 * @param utype 0��ͨѡȯ��Ĭ�ϣ�1���ô������ֿ۽���ͣ��ȯ
	 * @return
	 */
	private Map<String, Object> getDistotalLimit(Integer ptype,Long uid, Double total, Integer type, Integer utype, Long orderId){
//		logger.error("getDistotalLimit>>>ptype:"+ptype+",uid:"+uid+",total:"+total+",utype:"+utype);
		Map<String, Object> map = new HashMap<String, Object>();
		Double climit = 0d;
		Double blimit = 0d;
		Double slimit = 0d;
		if(ptype == 4){//����ѡȯ
			Double rewardquota = 3.0;//�ֿ�����
			Map<String, Object> userMap = daService.getMap("select rewardquota from user_info_tb where id = ?", new Object[]{uid});
			if(userMap != null && userMap.get("rewardquota") != null){
				rewardquota =StringUtils.formatDouble(userMap.get("rewardquota"));
			}
			if(type == 0){
				if(orderId != null && orderId > 0){
					Map<String, Object> orderMap = daService.getMap("select total from order_tb where id=? ", new Object[]{orderId});
					if(orderMap != null && orderMap.get("total") != null){
						Double fee = Double.valueOf(orderMap.get("total") + "");//ͣ���ѽ��
						
						//��ͨȯ  X��֧�����ѽ���� (fee) Y������ȯ�ֿ۽�� (climit) �㷨��X=6Y-2 ������rewardquota
						climit = Math.floor((fee+2)*(1.0/6));//����ȡ��
						if(climit < 0){
							climit = 0d;
						}
						if(climit > total){
							climit = total;
						}
						if(climit > rewardquota){
							climit = rewardquota;
						}
						//����ȯ   X��֧�����ѽ���� (fee) Y������ȯ�ֿ۽�� (blimit) �㷨��X=Y������rewardquota
						blimit = Math.floor(fee);//����ȡ��
						if(blimit < 0){
							blimit = 0d;
						}
						if(blimit > total){
							blimit = total;
						}
						if(blimit > rewardquota){
							blimit = rewardquota;
						}
						//ר��ȯ   X��֧�����ѽ���� (fee) Y������ȯ�ֿ۽�� (slimit) �㷨��X=6Y-2 ������rewardquota
						slimit = Math.floor((fee+2)*(1.0/6));//����ȡ��
						if(slimit < 0){
							slimit = 0d;
						}
						if(slimit > total){
							slimit = total;
						}
						if(slimit > rewardquota){
							slimit = rewardquota;
						}
					}
				}
				logger.error("getDistotalLimit>>>uid:"+uid+",climit:"+climit+",blimit:"+blimit+",slimit:"+slimit+",total:"+total+",ptype:"+ptype+",utype:"+utype+",type:"+type);
				
			}else if(type == 1){
				if(total > rewardquota){
					total = rewardquota;
				}
				//��ͨȯ  X��֧�����ѽ���� (climit) Y������ȯ�ֿ۽�� (total) �㷨��X=6Y-2 ������rewardquota
				climit = Math.ceil(total*6 - 2);
				//����ȯ  X��֧�����ѽ���� (blimit) Y������ȯ�ֿ۽�� (total) �㷨��X=Y ������rewardquota
				blimit = Math.ceil(total);
				//ר��ȯ  X��֧�����ѽ���� (slimit) Y������ȯ�ֿ۽�� (total) �㷨��X=6Y-2 ������rewardquota
				slimit = Math.ceil(total*6 - 2);
				
				map.put("distotal", total);//ʵ����ߵֿ۽��
//					logger.error("getDistotalLimit>>>uid:"+uid+",climit:"+climit+",blimit:"+blimit+",slimit:"+slimit+",rewardquota:"+rewardquota+",total:"+total+",ptype:"+ptype+",utype:"+utype+",type:"+type+",distotal:"+total);
			}
			
		}else if(ptype == -1 || ptype == 2 || ptype == 3){
			Double uplimit = 9999d;//�ֿ�����
			if(type == 0){
				//��ͨȯ  X�����ѽ���� (total) Y������ȯ�ֿ۽�� (climit) �㷨��X=6Y - 2 ������uplimit
				climit = Math.floor((total + 2)*(1.0/6));//����ȡ��
				if(climit < 0){
					climit = 0d;
				}
				if(climit > uplimit){
					climit = uplimit;
				}
				//����ȯ  X�����ѽ���� (total) Y������ȯ�ֿ۽�� (climit) �㷨��X=Y ������uplimit
				blimit = Math.floor(total);//����ȡ��
				if(blimit < 0){
					blimit = 0d;
				}
				if(blimit > uplimit){
					blimit = uplimit;
				}
				//ר��ȯ  X�����ѽ���� (total) Y������ȯ�ֿ۽�� (climit) �㷨��X=3Y+1 ������uplimit
				slimit = Math.floor((total - 1)*(1.0/3));//����ȡ��
				if(slimit < 0){
					slimit = 0d;
				}
				if(slimit > uplimit){
					slimit = uplimit;
				}
				logger.error("getDistotalLimit>>>uid:"+uid+",climit:"+climit+",blimit:"+blimit+",slimit:"+slimit+",uplimit:"+uplimit+",total:"+total+",ptype:"+ptype+",utype:"+utype+",type:"+type);
			}else if(type == 1){
				if(total > uplimit){
					total = uplimit;
				}
				//��ͨȯ  X��֧������� (climit) Y������ȯ�ֿ۽�� (total) �㷨��X=Y+1+Y/1 ������uplimit
				climit = Math.ceil(total*6 - 2);
				//����ȯ  X��֧������� (blimit) Y������ȯ�ֿ۽�� (total) �㷨��X=Y ������uplimit
				blimit = Math.ceil(total);
				//ר��ȯ  X��֧������� (slimit) Y������ȯ�ֿ۽�� (total) �㷨��X=Y+1 ������uplimit
				slimit = Math.ceil(total*3 + 1);
				map.put("distotal", total);//ʵ����ߵֿ۽��
//				logger.error("getDistotalLimit>>>uid:"+uid+",climit:"+climit+",blimit:"+blimit+",slimit:"+slimit+",uplimit:"+uplimit+",total:"+total+",ptype:"+ptype+",utype:"+utype+",type:"+type+",distotal:"+total);
			}
			
		}
		map.put("climit", climit);
		map.put("blimit", blimit);
		map.put("slimit", slimit);
//		logger.error("uid:"+uid+",map:"+map);
		setDistotalByUtype(map, utype, type);
		return map;
	}
	
	/**
	 * ��Ҫ�����Ͽͻ���utype=1������£�ȡ���ֵֿ��㷨�еֿ���С��һ����Ϊ�ֿۣ��Ͽͻ�����ѡ��ͬ��ȯ��ͬһ��limit���������Է�ֹ�û��ֶ�ѡȯʱ�ֿ۴���
	 * @param map
	 * @param utype 0��ͨѡȯ��Ĭ�ϣ�1���ô������ֿ۽���ͣ��ȯ
	 * @param type 0�����ݽ�����ȯ�ֿ۽�� 1������ȯ���������������ѽ���ȫ��ֿ�
	 * @return
	 */
	private Map<String, Object> setDistotalByUtype(Map<String, Object> map, Integer utype, Integer type){
//			logger.error("setDistotalByUtype>>>map:"+map+",utype:"+utype+",type:"+type);
		if(map != null && utype == 1 && type == 0){
			List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
			for(String key : map.keySet()){
				Map<String, Object> dMap = new HashMap<String, Object>();
				dMap.put("dlimit", map.get(key));
				list.add(dMap);
			}
			//���մ�С��������
			Collections.sort(list, new ListSort6());
			Double dlimit = Double.valueOf(list.get(0).get("dlimit") + "");
//			logger.error("setDistotalByUtype>>>list:"+list+",utype:"+utype+",type:"+type);
			for(String key : map.keySet()){
				map.put(key, dlimit);
			}
//			logger.error("setDistotalByUtype>>>map:"+map+",utype:"+utype+",type:"+type);
		}
		return map;
	}
	
	public List<Map<String, Object>> getTicketInfo(List<Map<String, Object>> list, Integer ptype,Long uid, Integer utype){
		if(list != null && !list.isEmpty()){
			for(Map<String, Object> map : list){
				Integer type=(Integer)map.get("type");
				Integer money = (Integer)map.get("money");
				Integer resources = (Integer)map.get("resources");
				Long limitDay = (Long)map.get("limit_day");
				Double backmoney = StringUtils.formatDouble(map.get("pmoney"));
				Long btime =TimeTools.getToDayBeginTime();
				//==========��ȡ������Ԫ��ȫ��ֿ�begin=============//
				Map<String, Object> fullMap = getDistotalLimit(2, uid, Double.valueOf(money + ""), 1, utype, -1L);
				Double climit = Double.valueOf(fullMap.get("climit") + "");
				Double blimit = Double.valueOf(fullMap.get("blimit") + "");
				Double slimit = Double.valueOf(fullMap.get("slimit") + "");
				Double distotal = Double.valueOf(fullMap.get("distotal") + "");
				map.put("distotal", distotal);
				if(type == 1){
					map.put("full", slimit);
				}
				if(type == 0 && resources == 0){
					map.put("full", climit);
				}
				if(type == 0 && resources == 1){
					map.put("full", blimit);
				}
				//==========��ȡ������Ԫ��ȫ��ֿ�end=============//
				if(btime >limitDay)
					map.put("exp", 0);
				else {
					map.put("exp", 1);
				}
				map.put("isbuy",resources);
				if(resources == 1){//�����ȯ
//					map.put("desc", "��"+map.get("full")+"Ԫ���Եֿ�ȫ��,���ں��˻�"+backmoney+"Ԫ�������˻�");
					map.put("desc", " ");
				}else{
//					map.put("desc", "��"+map.get("full")+"Ԫ���Եֿ�ȫ��");
					map.put("desc", " ");
				}
				map.put("cname", "");
				if(type == 1 && map.get("comid") != null){
					map.put("cname", getParkNameByComid((Long)map.get("comid")));
				}
				map.put("limitday", limitDay);
			}
		}
		return list;
	}
	
	/**
	 * 
	 * @param comid
	 * @return
	 */
	public String getParkNameByComid(Long comid){
		Map<String, Object> comMap = daService.getMap("select company_name from com_info_tb where id =? ",new Object[]{comid});
		if(comMap!=null){
			return (String)comMap.get("company_name");
		}
		return "";		
	}
	
	/**
	 * @param list ȯ�б�
	 * @param ptype 0�˻���ֵ��1���²�Ʒ��2ͣ���ѽ��㣻3ֱ��;4���� 5����ͣ��ȯ
	 * @param uid 
	 * @param total ���ѽ��
	 * @param utype  0��ͨѡȯ��Ĭ�ϣ�1���ô������ֿ۽���ͣ��ȯ
	 * @return
	 */
	public List<Map<String, Object>> chooseTicketByLevel(List<Map<String, Object>> list, Integer ptype,Long uid, Double total, Integer utype, Long parkId, Long orderId){
		list = null;//2016-09-07�������ص����в���������ͬ��
		
		//�ֿ��㷨
		Map<String, Object> distotalMap = getDistotalLimit(ptype, uid, total, 0, utype, orderId);
		Double climit = Double.valueOf(distotalMap.get("climit") + "");
		Double blimit = Double.valueOf(distotalMap.get("blimit") + "");
		Double slimit = Double.valueOf(distotalMap.get("slimit") + "");
		logger.error("the up limit of distotal>>>uid:"+uid+",map:"+distotalMap+",ptype:"+ptype+",total:"+total);
		if(list != null && !list.isEmpty()){
			for(int i=0; i<list.size();i++){
				Map<String, Object> map = list.get(i);
				Integer iscanuse = 1;//0:������ 1������
				Double limit = 0d;//��ͣ��ȯ�ɵֿ۽��
				Integer type=(Integer)map.get("type");
				Integer money = (Integer)map.get("money");
				Integer resources = (Integer)map.get("resources");
				if(type == 1){//ר��ͣ��ȯ
					if(map.get("comid") != null){
						Long comid = (Long)map.get("comid");
						if(comid.intValue() != parkId.intValue()){//���Ǹó���ר��ȯ������
							iscanuse = 0;
						}
					}else{
						iscanuse = 0;
					}
					
					if(slimit >= money){
						limit = Double.valueOf(money + "");
					}else{
						limit = slimit;
						if(utype == 0){//��ѡ��������ֿ۽���ȯ
							iscanuse = 0;
						}
					}
					map.put("limit", limit);//�ֿ۽��
					map.put("level", 3);//ר��ȯ���ȼ����
				}
				if(type == 0 && resources == 0){//�ǹ���ͣ��ȯ
					if(climit >= money){
						limit = Double.valueOf(money + "");
					}else{
						limit = climit;
						if(utype == 0){//��ѡ��������ֿ۽���ȯ
							iscanuse = 0;
						}
					}
					map.put("limit", limit);//�ֿ۽��
					map.put("level", 2);//��ͨ�ǹ���ȯ����Ȩ���
				}
				if(type == 0 && resources == 1){//����ͣ��ȯ
					if(blimit >= money){
						limit = Double.valueOf(money + "");
					}else{
						iscanuse = 0;//С��˵����ȯ����ѡ 
					}
					map.put("limit", limit);//�ֿ۽��
					map.put("level", 1);//����ȯ���ȼ����
				}
				if(limit == 0){//�ֿ�0������
					iscanuse = 0;
				}
				map.put("offset",  Math.abs(limit-money));//��ֵ����ֵ
				map.put("iscanuse", iscanuse);//�Ƿ���ô������ֿ�
			}
			Collections.sort(list, new ListSort());//����iscanuse�ɴ�С����
			Collections.sort(list, new ListSort1());//��ͬ��iscanuse���յֿ۽��limit�ɴ�С����
			Collections.sort(list, new ListSort2());//��ͬ��iscanuse��limit����offset��С��������
			Collections.sort(list, new ListSort3());//��ͬ��iscanuse��limit��offset����money��С��������
			Collections.sort(list, new ListSort4());//��ͬiscanuse��limit��offset��money����level�ɴ�С����
			Collections.sort(list, new ListSort5());//��ͬiscanuse��limit��offset��money��level��ͬ����limit_day��С��������
			
			getTicketInfo(list, ptype, uid, utype);//����ͣ��ȯ������Ԫ�ɴ����ֿ۶�
			
		}
		return list;
	}
	
	class ListSort implements Comparator<Map<String, Object>>{
		public int compare(Map<String, Object> o1, Map<String, Object> o2) {
			// TODO Auto-generated method stub
			Map map = getParams(o1, o2);
			Integer c1 = (Integer)map.get("c1");
			Integer c2 = (Integer)map.get("c2");
			
			return c2.compareTo(c1);
		}
		
	}
	
	class ListSort1 implements Comparator<Map<String, Object>>{
		public int compare(Map<String, Object> o1, Map<String, Object> o2) {
			// TODO Auto-generated method stub
			Map map = getParams(o1, o2);
			Integer c1 = (Integer)map.get("c1");
			Integer c2 = (Integer)map.get("c2");
			
			BigDecimal b1 = (BigDecimal)map.get("b1");
			BigDecimal b2 = (BigDecimal)map.get("b2");
			if(c2.compareTo(c1) == 0){
				return b2.compareTo(b1);
			}else{
				return 0;
			}
		}
		
	}
	
	class ListSort2 implements Comparator<Map<String, Object>>{
		public int compare(Map<String, Object> o1, Map<String, Object> o2) {
			// TODO Auto-generated method stub
			Map map = getParams(o1, o2);
			Integer c1 = (Integer)map.get("c1");
			Integer c2 = (Integer)map.get("c2");
			
			BigDecimal b1 = (BigDecimal)map.get("b1");
			BigDecimal b2 = (BigDecimal)map.get("b2");
			
			BigDecimal l1 = (BigDecimal)map.get("l1");
			BigDecimal l2 = (BigDecimal)map.get("l2");
			if(c2.compareTo(c1) == 0 && b2.compareTo(b1) == 0){
				return l1.compareTo(l2);
			}else{
				return 0;
			}
		}
		
	}
	
	class ListSort3 implements Comparator<Map<String, Object>>{

		public int compare(Map<String, Object> o1, Map<String, Object> o2) {
			// TODO Auto-generated method stub
			Map map = getParams(o1, o2);
			Integer c1 = (Integer)map.get("c1");
			Integer c2 = (Integer)map.get("c2");
			
			BigDecimal b1 = (BigDecimal)map.get("b1");
			BigDecimal b2 = (BigDecimal)map.get("b2");
			
			BigDecimal l1 = (BigDecimal)map.get("l1");
			BigDecimal l2 = (BigDecimal)map.get("l2");
			
			Integer m1 = (Integer)map.get("m1");
			Integer m2 = (Integer)map.get("m2");
			
			if(c2.compareTo(c1) == 0 && b2.compareTo(b1) == 0 && l2.compareTo(l1) == 0){
				return m1.compareTo(m2);
			}else{
				return 0;
			}
		}
		
	}
	
	class ListSort4 implements Comparator<Map<String, Object>>{

		public int compare(Map<String, Object> o1, Map<String, Object> o2) {
			// TODO Auto-generated method stub
			Map map = getParams(o1, o2);
			Integer c1 = (Integer)map.get("c1");
			Integer c2 = (Integer)map.get("c2");
			
			BigDecimal b1 = (BigDecimal)map.get("b1");
			BigDecimal b2 = (BigDecimal)map.get("b2");
			
			BigDecimal l1 = (BigDecimal)map.get("l1");
			BigDecimal l2 = (BigDecimal)map.get("l2");
			
			Integer m1 = (Integer)map.get("m1");
			Integer m2 = (Integer)map.get("m2");
			
			Integer e1 = (Integer)map.get("e1");
			Integer e2 = (Integer)map.get("e2");
			
			if(c2.compareTo(c1) == 0 && b2.compareTo(b1) == 0 && l2.compareTo(l1) == 0 && m2.compareTo(m1) == 0){
				return e2.compareTo(e1);
			}else{
				return 0;
			}
		}
		
	}
	
	class ListSort5 implements Comparator<Map<String, Object>>{

		public int compare(Map<String, Object> o1, Map<String, Object> o2) {
			// TODO Auto-generated method stub
			Map map = getParams(o1, o2);
			Integer c1 = (Integer)map.get("c1");
			Integer c2 = (Integer)map.get("c2");
			
			BigDecimal b1 = (BigDecimal)map.get("b1");
			BigDecimal b2 = (BigDecimal)map.get("b2");
			
			BigDecimal l1 = (BigDecimal)map.get("l1");
			BigDecimal l2 = (BigDecimal)map.get("l2");
			
			Integer m1 = (Integer)map.get("m1");
			Integer m2 = (Integer)map.get("m2");
			
			Integer e1 = (Integer)map.get("e1");
			Integer e2 = (Integer)map.get("e2");
			
			Long d1 = (Long)map.get("d1");
			Long d2 = (Long)map.get("d2");
			
			if(c2.compareTo(c1) == 0 && b2.compareTo(b1) == 0 && l2.compareTo(l1) == 0 && m2.compareTo(m1) == 0 && e2.compareTo(e1) == 0){
				return d1.compareTo(d2);
			}else{
				return 0;
			}
		}
		
	}
	
	class ListSort6 implements Comparator<Map<String, Object>>{
		public int compare(Map<String, Object> o1, Map<String, Object> o2) {
			// TODO Auto-generated method stub
			BigDecimal b1 = new BigDecimal(0);
			BigDecimal b2 = new BigDecimal(0);
			if(o1.get("dlimit") != null){
				if(o1.get("dlimit") instanceof Double){
					Double ctotal = (Double)o1.get("dlimit");
					b1 = b1.valueOf(ctotal);
				}else{
					b1 = (BigDecimal)o1.get("dlimit");
				}
			}
			if(o2.get("dlimit") != null){
				if(o2.get("dlimit") instanceof Double){
					Double ctotal = (Double)o2.get("dlimit");
					b2 = b2.valueOf(ctotal);
				}else{
					b2 = (BigDecimal)o2.get("dlimit");
				}
			}
			return b1.compareTo(b2);
		}
		
	}
	
	public List<Map<String, Object>> getCarType(Long comid){
		List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
		try {
			Map<String, Object> map = daService.getMap("select car_type from com_info_tb where id=? ", new Object[]{comid});
			if(map != null){
				Integer car_type = (Integer)map.get("car_type");
				if(car_type != 0){
					List<Map<String, Object>> list = daService.getAll("select id as value_no,name as value_name from car_type_tb where comid=? order by sort , id desc ", new Object[]{comid});
					if(!list.isEmpty()){
						result.addAll(list);
					}else {
						Map<String, Object> bigMap = new HashMap<String, Object>();
						bigMap.put("value_name","С��");
						bigMap.put("value_no", 1);
						Map<String, Object> smallMap = new HashMap<String, Object>();
						smallMap.put("value_name","��");
						smallMap.put("value_no", 2);
						result.add(bigMap);
						result.add(smallMap);
					}
				}else {
					Map<String, Object> firtstMap = new HashMap<String, Object>();
					firtstMap.put("value_name","ͨ��");
					firtstMap.put("value_no", 0);
					result.add(firtstMap);
				}
			}
			
		} catch (Exception e) {
			logger.error("getCarType", e);
		}
		return result;
	}
	private Map<String, Object> getParams(Map<String, Object> o1, Map<String, Object> o2){
		Map<String, Object> map = new HashMap<String, Object>();
		Integer c1 = (Integer)o1.get("iscanuse");
		if(c1 == null) c1 = 0;
		Integer c2 = (Integer)o2.get("iscanuse");
		if(c2 == null) c2 = 0;
		map.put("c1", c1);
		map.put("c2", c2);
		
		BigDecimal b1 = new BigDecimal(0);
		BigDecimal b2 = new BigDecimal(0);
		if(o1.get("limit") != null){
			if(o1.get("limit") instanceof Double){
				Double ctotal = (Double)o1.get("limit");
				b1 = b1.valueOf(ctotal);
			}else{
				b1 = (BigDecimal)o1.get("limit");
			}
		}
		if(o2.get("limit") != null){
			if(o2.get("limit") instanceof Double){
				Double ctotal = (Double)o2.get("limit");
				b2 = b2.valueOf(ctotal);
			}else{
				b2 = (BigDecimal)o2.get("limit");
			}
		}
		map.put("b1", b1);
		map.put("b2", b2);

		BigDecimal l1 = new BigDecimal(0);
		BigDecimal l2 = new BigDecimal(0);
		if(o1.get("offset") != null){
			if(o1.get("offset") instanceof Double){
				Double ctotal = (Double)o1.get("offset");
				l1 = l1.valueOf(ctotal);
			}else{
				l1 = (BigDecimal)o1.get("offset");
			}
		}
		if(o2.get("offset") != null){
			if(o2.get("offset") instanceof Double){
				Double ctotal = (Double)o2.get("offset");
				l2 = l2.valueOf(ctotal);
			}else{
				l2 = (BigDecimal)o2.get("offset");
			}
		}
		map.put("l1", l1);
		map.put("l2", l2);
		
		Integer m1 = (Integer)o1.get("money");
		if(m1 == null) m1 = 0;
		Integer m2 = (Integer)o2.get("money");
		if(m2 == null) m2 = 0;
		map.put("m1", m1);
		map.put("m2", m2);
		
		Integer e1 = (Integer)o1.get("level");
		if(e1 == null) e1 = 0;
		Integer e2 = (Integer)o2.get("level");
		if(e2 == null) e2 = 0;
		map.put("e1", e1);
		map.put("e2", e2);
		
		Long d1 = (Long)o1.get("limit_day");
		if(d1 == null) d1 = 0L;
		Long d2 = (Long)o2.get("limit_day");
		if(d2 == null) d2 = 0L;
		map.put("d1", d1);
		map.put("d2", d2);
		
		return map;
	}
	/**
	 * ��ȡ���е��µĳ���
	 * @param cityid
	 * @return
	 */
	public List<Object> getparks(Long cityid){
		List<Object> parks = new ArrayList<Object>();
		try {
			List<Object> params = new ArrayList<Object>();
			String sql = "select id from com_info_tb where state<>? " ;
			params.add(1);
			List<Object> groups = getGroups(cityid);//��ѯ�ó�����Ͻ����Ӫ����
			if(groups != null && !groups.isEmpty()){
				String preParams  ="";
				for(Object grouid : groups){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				sql += " and groupid in ("+preParams+") ";
				params.addAll(groups);
				List<Map<String, Object>> list = pService.getAllMap(sql, params);
				if(list != null && !list.isEmpty()){
					for(Map<String, Object> map : list){
						parks.add(map.get("id"));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return parks;
	}
	
	/**
	 * ��ȡ���е��µ���Ӫ���ű��
	 * @param cityid
	 * @return
	 */
	public List<Object> getGroups(Long cityid){//��ѯ������Ͻ����Ӫ����
		List<Object> groups = new ArrayList<Object>();
		List<Map<String, Object>> list = pService.getAll("select id from org_group_tb" +
				" where cityid=? and state=? ", 
				new Object[]{cityid, 0});
		if(list != null && !list.isEmpty()){
			for(Map<String, Object> map : list){
				groups.add(map.get("id"));
			}
		}
		return groups;
	}
	
	/**
	 * ��ȡ��Ӫ����Ͻ�µĳ���������Ӫ����Ͻ�µ�������µĳ���
	 * @param groupid
	 * @return
	 */
	public List<Object> getParks(Long groupid){
		List<Object> parks = new ArrayList<Object>();
		List<Object> params = new ArrayList<Object>();
		String sql = "select id from com_info_tb where state<>? and (groupid=? " ;
		params.add(1);
		params.add(groupid);
		String preParams  ="";
		List<Object> groups = new ArrayList<Object>();
		groups.add(groupid);
		List<Object> areas = getAreas(groups);//��ѯ����ֱϽ������ͳ�����Ͻ����Ӫ������Ͻ������
		if(areas != null && !areas.isEmpty()){
			preParams = "";
			for(Object area : areas){
				if(preParams.equals(""))
					preParams ="?";
				else
					preParams += ",?";
			}
			sql += " or areaid in ("+preParams+") ";
			params.addAll(areas);
		}
		sql += " )";
		List<Map<String, Object>> list = pService.getAllMap(sql,params);
		if(list != null && !list.isEmpty()){
			for(Map<String, Object> map : list){
				parks.add(map.get("id"));
			}
		}
		return parks;
	}
	/**
	 * ��ȡ��Ӫ���ŵ��µ�����
	 * @param groups
	 * @return
	 */
	public List<Object> getAreas(List<Object> groups){//��ѯ����ֱ������ͳ�����Ͻ�������µ�����
		List<Object> areas = new ArrayList<Object>();
		List<Object> params = new ArrayList<Object>();
		String sql = "select id from org_area_tb where state=? ";
		params.add(0);
		if(groups != null && !groups.isEmpty()){
			String preParams  ="";
			for(Object grouid : groups){
				if(preParams.equals(""))
					preParams ="?";
				else
					preParams += ",?";
			}
			sql += " and groupid in ("+preParams+")";
			params.addAll(groups);
			List<Map<String, Object>> list = pService.getAllMap(sql, params);
			if(list != null && !list.isEmpty()){
				for(Map<String, Object> map : list){
					areas.add(map.get("id"));
				}
			}
		}
		return areas;
	}
	//----------------ѡȯ�߼�end--------------------//
	
	/**
	 * ��ȡ�û����г��ƺ�
	 * @param uin
	 * @return
	 */
	public List<String> getAllCarnumbers(Long uin){
		List<String> carnumbers = new ArrayList<String>();
		List<Map> all = daService.getAll("select car_number from car_info_tb where uin = ?", new Object[]{uin});
		if(all!=null){
			for (Map map : all) {
				String carnum = (String) map.get("car_number");
				carnumbers.add(carnum);
			}
		}
		return carnumbers;
	}
	
	/**
	 * ��ӳ��ƺ�
	 * ����:���������ʱ�û�,��ɾ����ʱ�û�,�����ƺ��¿�ת����ǰ���û�;����������û�,�򽫳��ƺ����м�¼ɾ��,�����û����һ��
	 */
	public int addCarnum(Long uin, String carnumber){
		//1.�Ƿ��Ѿ����Լ��ĳ���
		Map carMap = daService.getMap("select uin,is_auth,state from car_info_tb where car_number = ? order by id desc", new Object[]{carnumber});
		if(carMap!=null){
			Long cuin = (Long) carMap.get("uin");
			Integer state = (Integer) carMap.get("state");
			if(cuin.intValue()==uin&&state!=0){
				logger.error("�����ѱ��Լ�ע��");
				return -3;
			}
		}
		//2.�󶨸���
		Long count = daService.getLong("select count(id) from car_info_tb where uin=? and state=? ", new Object[]{uin, 1});
		if(count > 3){
			logger.error("�����Ѿ����� ������uin:"+uin+",carnumber:"+carnumber+",count:"+count);
			return -4;
		}
		
		//û���������ڳ�����,�Ҳ����¿���ʵ�û�
		Long curorder = daService.getLong("select count(id) from order_tb where car_number = ? and state = ? and islocked = ? ", 
				new Object[]{carnumber,0,1});
		if(curorder.intValue()<1){
			//���԰�
			int bindCarnumber = bindCarnumber(uin, carnumber);
			return bindCarnumber;
		}else{
			//�������е��ڳ�����,���ɰ�
			return -2;
		}
		/*//3.���ƶ�Ӧ�¿��Ƿ�ȫ������
		Map<String, Object> ret = isProdBeOverdue(carnumber,uin);
		Integer overdued = (Integer)ret.get("overdued");
		if(overdued==1){
			//ȫ����,�������û�uin,ֱ�Ӱ�,����uin
			logger.error("ȫ����");
			List<Long> hasuser = (List<Long>) ret.get("hasuser");
			//ɾ��������usercar����,�¼ӳ���,�û�Ϊuin,
			return bindCarnumber(hasuser, uin, carnumber,false);
		}else if(overdued==2){
			//��δ����,����δ���ڵ�,���uin��-1,���԰�:�����uin��-1 ��
			logger.error("��δ����");
			List<Long> nouser = (List<Long>) ret.get("nouser");
			return bindCarnumber(nouser, uin, carnumber,false);
		}else{
			logger.error("���¿�,ֱ�Ӱ�");
			return bindCarnumber(null, uin, carnumber, true);
		}*/
	}
	
	//�󶨳���
	private int bindCarnumber(Long uin,String carnumber){
		//���԰�:ɾ��֮ǰ����,�����¿��û���Ϣ,�¼ӳ���
		List<Map<String , Object>> sqlMaps = new ArrayList<Map<String,Object>>();
		Map map = daService.getMap("select * from car_info_tb where car_number = ? order by create_time desc limit ?", new Object[]{carnumber,1});
		Long cuin = -1L;
		if(map!=null){
			Map<String, Object> delMap = new HashMap<String, Object>();
			String delSql = "delete from car_info_tb where car_number = ?";
			delMap.put("sql", delSql);
			delMap.put("values", new Object[]{carnumber});
			sqlMaps.add(delMap);
			cuin = (Long) map.get("uin");
		}
		
		//�鿴�ó����ڳ������б�,�����,����uin
		Long curcount = daService.getLong("select count(id) from order_tb where car_number = ?", new Object[]{carnumber});
		if(curcount.intValue()>0){
			int update = daService.update("update order_tb set uin = ? where car_number = ?", new Object[]{uin,carnumber});
			if(update>0){
				logger.error("�����ڳ������û���Ϣ�ɹ�!");
			}else{
				logger.error("�����ڳ������û���Ϣʧ��!");
			}
		}
		//��ó����¿��б�,�����¿�
		String sql = "select id from carower_product where car_number like ? ";
		List<Map> prodlist = daService.getAll(sql, new Object[]{"%"+carnumber+"%"});
		if(prodlist!=null&&prodlist.size()>0){
			for (Map prod : prodlist) {
				Long prodid = (Long) prod.get("id");
				Map<String, Object> updateMap = new HashMap<String, Object>();
				String updateSql = "update carower_product set uin = ? where id = ?";
				updateMap.put("sql", updateSql);
				updateMap.put("values", new Object[]{uin,prodid});
				sqlMaps.add(updateMap);
			}
			logger.error("�����¿��ɹ�!");
		}
		//��ӳ���
		Map<String, Object> addMap = new HashMap<String, Object>();
		String addSql = "insert into car_info_tb(uin,car_number,create_time) values(?,?,?)";
		addMap.put("sql", addSql);
		addMap.put("values", new Object[]{uin,carnumber,System.currentTimeMillis()/1000});
		sqlMaps.add(addMap);
		boolean bathUpdate = daService.bathUpdate(sqlMaps);
		if(bathUpdate){
			publicMethods.syncUserAddPlateNumber(uin, carnumber, "");
			/*if(cuin!=-1){
				publicMethods.syncUserPlateNumberDelete(cuin+"", carnumber);
			}*/
			return 1;//�󶨳ɹ�
		}else{
			logger.error("��ʧ��");
			return -1;//��ʧ��
		}
	}
	
	/*//�󶨳���
	private int bindCarnumber(List<Long> prodlist,Long uin,String carnumber,boolean noprod){
		if(prodlist!=null&&prodlist.size()>0||noprod){
			//��Щ�¿�Ϊ���û�,δ�����¿�,���԰�:��������,�����¿��û���Ϣ
			List<Map<String , Object>> sqlMaps = new ArrayList<Map<String,Object>>();
			Long count = daService.getLong("select count(id) from car_info_tb where car_number = ?", new Object[]{carnumber});
			if(count>0){
				Map<String, Object> delMap = new HashMap<String, Object>();
				String delSql = "delete from car_info_tb where car_number = ?";
				delMap.put("sql", delSql);
				delMap.put("values", new Object[]{carnumber});
				sqlMaps.add(delMap);
			}
			
			if(prodlist!=null&&prodlist.size()>0){
				for (Long prodid : prodlist) {
					Map<String, Object> updateMap = new HashMap<String, Object>();
					String updateSql = "update carower_product set uin = ? where id = ?";
					updateMap.put("sql", updateSql);
					updateMap.put("values", new Object[]{uin,prodid});
					sqlMaps.add(updateMap);
				}
			}
			//��ӳ���
			Map<String, Object> addMap = new HashMap<String, Object>();
			String addSql = "insert into car_info_tb(uin,car_number,create_time) values(?,?,?)";
			addMap.put("sql", addSql);
			addMap.put("values", new Object[]{uin,carnumber,System.currentTimeMillis()/1000});
			sqlMaps.add(addMap);
			boolean bathUpdate = daService.bathUpdate(sqlMaps);
			if(bathUpdate){
				return 1;//�󶨳ɹ�
			}else{
				logger.error("��ʧ��");
				return -1;//��ʧ��
			}
		}
		logger.error("���ɰ�");
		return -2;
	}*/
	
	/**
	 * �жϸó��ƶ�Ӧ�¿��Ƿ�ȫ������
	 * ����:��δ�����¿�(���¿�ȥ�¿���,���ݳ��Ʋ�ʱ���Ƿ����)
	 * @param carnumber
	 * @return
	 */
	public Map<String, Object> isProdBeOverdue(String carnumber,Long cuin){
		int ret = 1;
		List<Long> hasuser = new ArrayList<Long>();
		List<Long> nouser = new ArrayList<Long>();
		String sql = "select id,e_time,uin from carower_product where car_number like ? ";
		Long toDayBeginTime = TimeTools.getToDayBeginTime();
		List<Map> all = daService.getAll(sql, new Object[]{"%"+carnumber+"%"});
		logger.error("���ݳ���"+carnumber+"����������¿�: "+all);
		if(all!=null&&all.size()>0){
			for (Map map : all) {
				Long etime = (Long) map.get("e_time");
				Long uin = (Long) map.get("uin");
				Long id = (Long) map.get("id");
				if(etime>toDayBeginTime){
					//δ����
					ret = 2;
					logger.error(uin+"~~"+cuin);
					if(uin==-1||uin.intValue()==cuin.intValue()){
						//δ����,���û�
						nouser.add(id);
					}
				}else{
					//����,�����¿�
					hasuser.add(id);
				}
			}
		}else{
			//���¿�
			ret = -1;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("overdued", ret);
		map.put("hasuser", hasuser);
		map.put("nouser", nouser);
		return map;
	}
}
