package com.zldpark.schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.zldpark.impl.CommonMethods;
import com.zldpark.service.DataBaseService;
import com.zldpark.service.PgOnlyReadService;
import com.zldpark.utils.ExecutorsUtil;
import com.zldpark.utils.MemcacheUtils;
import com.zldpark.utils.TimeTools;

public class TaskPeriod1h implements Runnable {
	private DataBaseService dataBaseService;
	private PgOnlyReadService pgOnlyReadService;
	private MemcacheUtils memcacheUtils;
	private CommonMethods commonMethods;
	
	public TaskPeriod1h(DataBaseService dataBaseService, PgOnlyReadService pgOnlyReadService,
			MemcacheUtils memcacheUtils, CommonMethods commonMethods){
		this.dataBaseService = dataBaseService;
		this.pgOnlyReadService = pgOnlyReadService;
		this.memcacheUtils = memcacheUtils;
		this.commonMethods = commonMethods;
	}

	private static Logger log = Logger.getLogger(TaskPeriod1h.class);
	
	@Override
	public void run() {
		log.error("********************��ʼ1Сʱһ�εĶ�ʱ����***********************");
		try {
			Long curTime = System.currentTimeMillis()/1000;
			ExecutorService pool = ExecutorsUtil.getExecutorService();
			ExeTask callable0 = new ExeTask(curTime, 0);
			ExeTask callable1 = new ExeTask(curTime, 1);
			ExeTask callable2 = new ExeTask(curTime, 2);
			Future<Object> future0 = pool.submit(callable0);
			Future<Object> future1 = pool.submit(callable1);
			Future<Object> future2 = pool.submit(callable2);
			future0.get();
			future1.get();
			future2.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.error("********************����1Сʱһ�εĶ�ʱ����***********************");
	}
	
	class ExeTask implements Callable<Object>{
		private long curTime;
		private int queue;
		
		public ExeTask(long curTime, int queue) {
			this.curTime = curTime;
			this.queue = queue;
		}
		@Override
		public Object call() throws Exception {
			try {
				switch (queue) {
				case 0:
					anlyPark(curTime);
					break;
				case 1:
					anlyRoadPark(curTime);
					break;
				case 2:
					anlyOnline(curTime);
					break;
				default:
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	//ÿСʱͳ��һ�γ�����λ�������
	private void anlyPark(Long ntime){//ͳ�Ʒ�ճ�����λ��Ϣ
		Long begintime = TimeTools.getToDayBeginTime();
		log.error("======��ʼͳ�ƿ��в�λ=======");
		try {
			//��ѯһ��֮���ж����ĳ��� 
			List<Map<String, Object>> comList = pgOnlyReadService.getAll("select id,parking_total,share_number,invalid_order from com_info_tb where id " +
					" in (select distinct(comid) as cid from order_tb where create_time>?) and parking_type<>? ", new Object[]{begintime-24*60*60, 2});
			if(comList!=null&&!comList.isEmpty()){
				log.error("========��Ҫͳ�Ƶĳ�����:"+comList.size());
				List<Object> idList = new ArrayList<Object>();
				//ÿһ��������������ǰ��14��ǰ����Ч��������������в�λ 
				String inserSql = "insert into park_anlysis_tb (create_time,comid,share_count,used_count,month_used_count,time_used_count)" +
						" values(?,?,?,?,?,?)";
				String remainSql = "insert into remain_berth_tb(comid,amount,total,update_time) values(?,?,?,?) ";
				List<Object[]> anlyList = new ArrayList<Object[]>();
				List<Object[]> remainList = new ArrayList<Object[]>();
				for(Map<String, Object> cMap: comList){
					Long comId =(Long) cMap.get("id");//�������
					idList.add(comId);
					Integer parking_total = 0;
					Integer share_number = 0;
					Long invalid_order = 0L;//δ���������������
					if(cMap.get("parking_total") != null){
						parking_total = (Integer)cMap.get("parking_total");
					}
					if(cMap.get("share_number") != null){
						share_number = (Integer)cMap.get("share_number");
					}
					if(cMap.get("invalid_order") != null){
						invalid_order = (Long)cMap.get("invalid_order");
					}
					if(share_number == 0){
						share_number = parking_total;
					}
					Long useCount = 0L;
					if(comId != null && comId > 0){
						long time2 = ntime-2*24*60*60;
						long time16 = ntime-16*24*60*60;
						Long month_used_count = 0L;
						Long time_used_count = 0L;
						String sql = "select count(ID) ucount,c_type from order_tb where comid=? and create_time>? and state=? group by c_type ";
						List<Map<String, Object>> allList = pgOnlyReadService.getAll(sql, new Object[]{comId,time2,0});
						if(allList != null && !allList.isEmpty()){
							for(Map<String, Object> map : allList){
								Integer c_type = (Integer)map.get("c_type");
								Long ucount = (Long)map.get("ucount");
								if(c_type == 5 || c_type == 7 || c_type == 8){//�¿���λռ����
									month_used_count = ucount;
								}else{//ʱ�Ⲵλռ����
									time_used_count += ucount;
								}
							}
						}
						
						Long invmonth_used_count = 0L;
						Long invtime_used_count = 0L;
						String sql1 = "select count(ID) ucount,c_type from order_tb where comid=? and create_time>? and create_time<? and state=? group by c_type ";
						List<Map<String, Object>> invList = pgOnlyReadService.getAll(sql1, new Object[]{comId,time16,time2,0});
						if(invList != null && !invList.isEmpty()){
							for(Map<String, Object> map : invList){
								Integer c_type = (Integer)map.get("c_type");
								Long ucount = (Long)map.get("ucount");
								if(c_type == 5 || c_type == 7 || c_type == 8){//�¿���λռ����
									invmonth_used_count = ucount;
								}else{//ʱ�Ⲵλռ����
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
						month_used_count = month_used_count - month_offset;//�¿���ȥ��ƫ�������ռ�ò�λ��
						time_used_count = time_used_count - (invalid_order - month_offset);//ȥ��ƫ�������ʱ�⳵ռ�ò�λ��
						if(month_used_count < 0){
							month_used_count = 0L;
						}
						if(time_used_count < 0){
							time_used_count = 0L;
						}
						useCount = month_used_count + time_used_count;
						if(useCount > share_number){
							share_number = useCount.intValue();
						}
						Object[] values = new Object[]{ntime,comId,share_number,useCount.intValue(),month_used_count.intValue(),time_used_count.intValue()};
						anlyList.add(values);
						Long remain = share_number - useCount;
						Object[] values2 = new Object[]{comId,remain.intValue(),share_number,ntime};
						remainList.add(values2);
					}
				}
				log.error("======��Ҫд�����ݿ���м�¼����"+anlyList.size());
				if(!anlyList.isEmpty()){
					int ret = dataBaseService.bathInsert(inserSql, anlyList, new int[]{4,4,4,4,4,4});
					log.error("=======д����ɣ�����:"+ret);
				}
				if(!remainList.isEmpty()){
					String preParams  ="";
					for(Object id : idList){
						if(preParams.equals(""))
							preParams ="?";
						else
							preParams += ",?";
					}
					int ret = dataBaseService.updateParamList("delete from remain_berth_tb where comid in ("+preParams+")", idList);
					ret = dataBaseService.bathInsert(remainSql, remainList, new int[]{4,4,4,4});
					log.error("д����λ��===ret��"+ret);
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			log.error("anlyPark error",e);
		}
	}
	
	private void anlyRoadPark(Long ntime){//��·ͣ������λ��Ϣ
		log.error("======��ʼͳ�Ƶ�·ͣ�������в�λ=======");
		try {
			Long begintime = TimeTools.getToDayBeginTime();
			//��ѯһ��֮���ж����ĳ��� 
			List<Map<String, Object>> comList = pgOnlyReadService.getAll("select id from com_info_tb where id " +
					" in (select distinct(comid) as cid from order_tb where create_time>?) and parking_type=? ", new Object[]{begintime-24*60*60, 2});
			List<Object> paramList = new ArrayList<Object>();
			List<Object> paramList2 = new ArrayList<Object>();
			List<Object> idList = new ArrayList<Object>();
			if(comList != null && !comList.isEmpty()){
				String preParams  ="";
				for(Map<String, Object> map : comList){
					Long comId = (Long)map.get("id");//�������
					idList.add(comId);
					
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				updateRoadBerth(idList, ntime);//һ��Сʱͳ��һ�ε�·ͣ���Ĳ�λ��,Ҫ��У��ǰ��
				
				paramList.addAll(idList);
				paramList.add(0);
				paramList.add(0);
				paramList.add(0);
				List<Map<String, Object>> list = pgOnlyReadService.getAllMap("select count(distinct p.id) amount,p.comid,p.berthsec_id from com_park_tb p left join dici_tb d on p.dici_id=d.id where " +
						" p.comid in ("+preParams+") and (p.order_id is null or p.order_id<?) and (d.state=? or d.state is null) and p.is_delete=? group by p.comid,p.berthsec_id ", paramList);
				paramList2.addAll(idList);
				paramList2.add(0);
				List<Map<String, Object>> berthlist = pgOnlyReadService.getAllMap("select count(id) total,comid,berthsec_id from com_park_tb where " +
						"comid in ("+preParams+") and is_delete=? group by comid,berthsec_id ", paramList2);
				log.error("size:"+list.size());
				if(list != null && !list.isEmpty()){
					String remainSql = "insert into remain_berth_tb(comid,amount,berthseg_id,total,update_time) values(?,?,?,?,?) ";
					List<Object[]> anlyList = new ArrayList<Object[]>();
					for(Map<String, Object> map : list){
						Long comid = (Long)map.get("comid");
						Long amount = (Long)map.get("amount");
						Long berthsec_id = (Long)map.get("berthsec_id");
						Long total = 0L;
						for(Map<String, Object> map2 : berthlist){
							Long cid = (Long)map2.get("comid");
							Long bid = (Long)map2.get("berthsec_id");
							if(cid.intValue() == comid.intValue() && bid.intValue() == berthsec_id.intValue()){
								total = (Long)map2.get("total");
								break;
							}
						}
						map.put("total", total);
						Object[] values2 = new Object[]{comid,amount.intValue(),berthsec_id,total.intValue(),ntime};
						anlyList.add(values2);
					}
					
					if(!anlyList.isEmpty()){
						int ret = dataBaseService.updateParamList("delete from remain_berth_tb where comid in ("+preParams+") ", idList);
						ret = dataBaseService.bathInsert(remainSql, anlyList, new int[]{4,4,4,4,4});
						log.error("�������ݣ�ret��"+ret);
					}
				}
			}
		} catch (Exception e) {
			log.error("anlyRoadPark error",e);
		}
	}
	
	private void updateRoadBerth(List<Object> idList, Long ntime){
		try {
			if(idList != null && !idList.isEmpty()){
				String preParams  ="";
				for(Object o : idList){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				List<Object> params = new ArrayList<Object>();
				params.addAll(idList);
				params.add(0);
				List<Map<String, Object>> list = pgOnlyReadService.getAllMap("select sum(total) psum,sum(amount) rsum,comid from remain_berth_tb " +
						" where comid in ("+preParams+") and state=? group by comid ", params);
				String inserSql = "insert into park_anlysis_tb (create_time,comid,share_count,used_count,month_used_count,time_used_count)" +
						" values(?,?,?,?,?,?)";
				List<Object[]> anlyList = new ArrayList<Object[]>();
				if(list != null && !list.isEmpty()){
					for(Map<String, Object> map : list){
						Long comId = (Long)map.get("comid");
						Long psum = Long.valueOf(map.get("psum") + "");//�ܲ�λ��
						Long rsum = Long.valueOf(map.get("rsum") + "");//ʣ�೵λ��
						Long usum = 0L;//ռ�õĲ�λ��
						Long msum = 0L;//�¿���ռ����
						Long tsum = 0L;//ʱ�⳵ռ����
						if(psum > rsum){
							usum = psum - rsum;
						}
						List<Map<String, Object>> orderList = pgOnlyReadService.getAll("select count(o.id) ccount,o.c_type from order_tb o,com_park_tb p " +
								"where o.id=p.order_id and p.comid=? and p.is_delete=? and p.order_id>? group by o.c_type ", new Object[]{comId, 0, 0});
						if(orderList != null && !orderList.isEmpty()){
							for(Map<String, Object> map2 : orderList){
								Integer c_type = (Integer)map2.get("c_type");
								Long ccount = (Long)map2.get("ccount");
								if(c_type == 5 || c_type == 7 || c_type == 8){
									msum += ccount;
								}
							}
						}
						if(msum > usum){
							msum = usum;
						}
						tsum = usum - msum;
						Object[] values = new Object[]{ntime,comId,psum.intValue(),usum.intValue(),msum.intValue(),tsum.intValue()};
						anlyList.add(values);
					}
					log.error("updateRoadBerth��size:"+anlyList.size());
					if(!anlyList.isEmpty()){
						int ret = dataBaseService.bathInsert(inserSql, anlyList, new int[]{4,4,4,4,4,4});
						log.error("updateRoadBerth������:"+ret);
					}
				}
			}
		} catch (Exception e) {
			log.error("updateRoadBerth",e);
		}
	}
	
	private void anlyOnline(Long ntime){
		try {
			Calendar c = Calendar.getInstance();
			c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
			long time = (c.getTime().getTime() / 1000);
			
			String sql1 = "select count(id) ocount,comid,auth_flag from user_info_tb where auth_flag in (?,?,?) and state<>? and online_flag=? and comid>? and exists(select 1 from order_tb where uid = u.id) group by comid,auth_flag ";
			String sql2 = "select count(id) ocount,groupid,auth_flag from user_info_tb where auth_flag in (?,?,?) and state<>? and groupid>? and " +
					" id in (select uid from parkuser_work_record_tb where state=?) and exists(select 1 from order_tb where uid = u.id and create_time > ?) group by groupid,auth_flag ";
			String sql3 = "select count(id) ocount,cityid,auth_flag from user_info_tb as u where auth_flag in (?,?,?) and state<>? and online_flag=? and cityid>? and exists(select 1 from order_tb where uid = u.id and create_time > ?) group by cityid,auth_flag ";
			List<Map<String, Object>> list1 = pgOnlyReadService.getAll(sql1, new Object[]{1,2,16,1,22,0});
			List<Map<String, Object>> list2 = pgOnlyReadService.getAll(sql2, new Object[]{1,2,16,1,0,0, time});
			List<Map<String, Object>> list3 = pgOnlyReadService.getAll(sql3, new Object[]{1,2,16,1,22,0, time});
			String sql = "insert into online_anlysis_tb(comid,groupid,cityid,collector_online,inspector_online,create_time) values(?,?,?,?,?,?) ";
			if(list1 != null && !list1.isEmpty()){
				List<Map<String, Object>> comList = new ArrayList<Map<String,Object>>();
				List<Object> comidList = new ArrayList<Object>();
				List<Object[]> anlyList = new ArrayList<Object[]>();
				for(Map<String, Object> map : list1){
					Long comid = (Long)map.get("comid");
					Long auth_flag = (Long)map.get("auth_flag");
					Long ocount = (Long)map.get("ocount");
					if(comidList.contains(comid)){
						for(Map<String, Object> map2 : comList){
							Long id = (Long)map2.get("comid");
							if(comid.intValue() == id.intValue()){
								Long collector_online = (Long)map2.get("collector_online");
								Long inspector_online = (Long)map2.get("inspector_online");
								if(auth_flag == 1 || auth_flag == 2){//�շ�Ա
									map2.put("collector_online", collector_online + ocount);
								}else if(auth_flag == 16){//Ѳ��Ա
									map2.put("inspector_online", inspector_online + ocount);
								}
							}
						}
					}else{
						Map<String, Object> map2 = new HashMap<String, Object>();
						map2.put("comid", comid);
						map2.put("collector_online", 0L);
						map2.put("inspector_online", 0L);
						if(auth_flag == 1 || auth_flag == 2){//�շ�Ա
							map2.put("collector_online", ocount);
						}else if(auth_flag == 16){//Ѳ��Ա
							map2.put("inspector_online", ocount);
						}
						comList.add(map2);
						comidList.add(comid);
					}
					
				}
				
				for(Map<String, Object> map : comList){
					Long comid = (Long)map.get("comid");
					Long collector_online = (Long)map.get("collector_online");
					Long inspector_online = (Long)map.get("inspector_online");
					Object[] values2 = new Object[]{comid,-1L,-1L,collector_online,inspector_online,ntime};
					anlyList.add(values2);
				}
				
				if(!anlyList.isEmpty()){
					int ret = dataBaseService.bathInsert(sql, anlyList, new int[]{4,4,4,4,4,4});
					log.error("�������ݣ�ret��"+ret);
				}
			}
			
			if(list2 != null && !list2.isEmpty()){
				List<Map<String, Object>> groupList = new ArrayList<Map<String,Object>>();
				List<Object> groupidList = new ArrayList<Object>();
				List<Object[]> anlyList = new ArrayList<Object[]>();
				for(Map<String, Object> map : list2){
					Long groupid = (Long)map.get("groupid");
					Long auth_flag = (Long)map.get("auth_flag");
					Long ocount = (Long)map.get("ocount");
					if(groupidList.contains(groupid)){
						for(Map<String, Object> map2 : groupList){
							Long id = (Long)map2.get("groupid");
							if(groupid.intValue() == id.intValue()){
								Long collector_online = (Long)map2.get("collector_online");
								Long inspector_online = (Long)map2.get("inspector_online");
								if(auth_flag == 1 || auth_flag == 2){//�շ�Ա
									map2.put("collector_online", collector_online + ocount);
								}else if(auth_flag == 16){//Ѳ��Ա
									map2.put("inspector_online", inspector_online + ocount);
								}
							}
						}
					}else{
						Map<String, Object> map2 = new HashMap<String, Object>();
						map2.put("groupid", groupid);
						map2.put("collector_online", 0L);
						map2.put("inspector_online", 0L);
						if(auth_flag == 1 || auth_flag == 2){//�շ�Ա
							map2.put("collector_online", ocount);
						}else if(auth_flag == 16){//Ѳ��Ա
							map2.put("inspector_online", ocount);
						}
						groupList.add(map2);
						groupidList.add(groupid);
					}
					
				}
				
				for(Map<String, Object> map : groupList){
					Long groupid = (Long)map.get("groupid");
					Long collector_online = (Long)map.get("collector_online");
					Long inspector_online = (Long)map.get("inspector_online");
					Object[] values2 = new Object[]{-1L,groupid,-1L,collector_online,inspector_online,ntime};
					anlyList.add(values2);
				}
				
				if(!anlyList.isEmpty()){
					int ret = dataBaseService.bathInsert(sql, anlyList, new int[]{4,4,4,4,4,4});
					log.error("�������ݣ�ret��"+ret);
				}
			}
			
			if(list3 != null && !list3.isEmpty()){
				List<Map<String, Object>> cityList = new ArrayList<Map<String,Object>>();
				List<Object> cityidList = new ArrayList<Object>();
				List<Object[]> anlyList = new ArrayList<Object[]>();
				for(Map<String, Object> map : list3){
					Long cityid = (Long)map.get("cityid");
					Long auth_flag = (Long)map.get("auth_flag");
					Long ocount = (Long)map.get("ocount");
					if(cityidList.contains(cityid)){
						for(Map<String, Object> map2 : cityList){
							Long id = (Long)map2.get("cityid");
							if(cityid.intValue() == id.intValue()){
								Long collector_online = (Long)map2.get("collector_online");
								Long inspector_online = (Long)map2.get("inspector_online");
								if(auth_flag == 1 || auth_flag == 2){//�շ�Ա
									map2.put("collector_online", collector_online + ocount);
								}else if(auth_flag == 16){//Ѳ��Ա
									map2.put("inspector_online", inspector_online + ocount);
								}
							}
						}
					}else{
						Map<String, Object> map2 = new HashMap<String, Object>();
						map2.put("cityid", cityid);
						map2.put("collector_online", 0L);
						map2.put("inspector_online", 0L);
						if(auth_flag == 1 || auth_flag == 2){//�շ�Ա
							map2.put("collector_online", ocount);
						}else if(auth_flag == 16){//Ѳ��Ա
							map2.put("inspector_online", ocount);
						}
						cityList.add(map2);
						cityidList.add(cityid);
					}
					
				}
				
				for(Map<String, Object> map : cityList){
					Long cityid = (Long)map.get("cityid");
					Long collector_online = (Long)map.get("collector_online");
					Long inspector_online = (Long)map.get("inspector_online");
					Object[] values2 = new Object[]{-1L,-1L,cityid,collector_online,inspector_online,ntime};
					anlyList.add(values2);
				}
				
				if(!anlyList.isEmpty()){
					int ret = dataBaseService.bathInsert(sql, anlyList, new int[]{4,4,4,4,4,4});
					log.error("�������ݣ�ret��"+ret);
				}
			}
		} catch (Exception e) {
			log.error("anlyOnline", e);
		}
	}
}
