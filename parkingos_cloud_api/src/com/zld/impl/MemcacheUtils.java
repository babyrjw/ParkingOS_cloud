package com.zld.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.zld.utils.CacheXMemcache;
import com.zld.utils.CachedTask;
import com.zld.utils.TimeTools;


/**
 * memcached���ߣ�������²�Ʒ��֧����������ѯ���ƺ��� 
 * @author Administrator
 *
 */
@SuppressWarnings("unchecked")
@Repository
public class MemcacheUtils {

	
	private Logger logger = Logger.getLogger(MemcacheUtils.class);
	
	@SuppressWarnings("rawtypes")
	@Autowired
	private CacheXMemcache cacheXMemcache;

	/**
	 * ��ӷֲ�ʽ��
	 * add�������ɹ�����true���������Ѿ�����ʱ������false��Ӧ���ڷֲ�ʽ��
	 * @param key ����־
	 * @return
	 */
	public boolean addLock(String key){//���5����
		try {
			byte[] b = new byte[0];
			return cacheXMemcache.addCached(key, b, 300);
		} catch (Exception e) {
			logger.error(e);
		}
		return true;//�������׳��쳣ʱ������true����ʹ�÷ֲ�ʽ��
	}
	/**
	 * ��ӷֲ�ʽ��
	 * @param key ����־
	 * @param exp ��������
	 * @return
	 */
	public boolean addLock(String key, int exp){
		try {
			byte[] b = new byte[0];
			return cacheXMemcache.addCached(key, b, exp);
		} catch (Exception e) {
			logger.error(e);
		}
		return true;//�������׳��쳣ʱ������true����ʹ�÷ֲ�ʽ��
	}
	/**
	 * ɾ���ֲ�ʽ������Ϊ��������������������5���ӣ�ǿ�ҽ�����finally�����
	 * @param key
	 * @return
	 */
	public boolean delLock(String key){
		try {
			if(key == null || "".equals(key)){
				return true;
			}
			return cacheXMemcache.delCached(key);
		} catch (Exception e) {
			logger.error(e);
		}
		return false;
	}
	
	/**
	 * set��������Լ���ӣ����set��key�Ѿ����ڣ���������Ը��¸�key����Ӧ��ԭ�������ݣ�Ҳ����ʵ�ָ��µ����á�
	 * @param key
	 * @param value
	 * @param exp
	 * @return
	 */
	public boolean setCache(String key, String value, int exp){
		try {
			return cacheXMemcache.setCached(key, value, exp);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public String getCache(String key){
		try {
			return (String)cacheXMemcache.getCached(key);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String doStringCache(String key,
			String map,String updateFlag) {
		final String map2 = map;
		return (String) cacheXMemcache.doCachedTask(new CachedTask<String>(key, updateFlag) {
			public String run() {
				return map2;
			}
		});
	}
	
	
	public   Map<Long ,Long> doMapLongLongCache(String key,
			Map<Long ,Long> map,String updateFlag) {
		final Map<Long ,Long> map2 = map;
		return (Map<Long ,Long>) cacheXMemcache.doCachedTask(new CachedTask<Map<Long ,Long>>(key, updateFlag) {
			public Map<Long ,Long> run() {
				return map2;
			}
		});
	}
	/*֧��ʹ��ȯ����  ,key = usetickets_times**/
	public   Map<Long ,String> doMapLongStringCache(String key,
			Map<Long ,String> map,String updateFlag) {
		final Map<Long ,String> map2 = map;
		return (Map<Long ,String>) cacheXMemcache.doCachedTask(new CachedTask<Map<Long ,String>>(key, updateFlag) {
			public Map<Long ,String> run() {
				return map2;
			}
		});
	}
	
	/**
	 * @param д���շ�Աtoken
	 * @return
	 */
	public  Map<String,String > doMapStringStringCache(String key,Map<String,String > value,String updateFlag) {
		final Map<String,String > value1 = value;
		return (Map<String,String >) cacheXMemcache.doCachedTask(new CachedTask<Map<String,String >>(key, updateFlag) {
			public Map<String,String > run() {
				return value1;
			}
		});
	}

	

	/**
	 * @param д����һ���������
	 * @return
	 */
	public  Integer doIntegerCache(String key,Integer value,String updateFlag) {
		final Integer value1 = value;
		return (Integer) cacheXMemcache.doCachedTask(new CachedTask<Integer>(key, updateFlag) {
			public Integer run() {
				return value1;
			}
		});
	}
	
	/**
	 * @param 
	 * @return
	 */
	public  List<String> doListStringCache(String key,final List<String> value,String updateFlag) {
		return (List<String>) cacheXMemcache.doCachedTask(new CachedTask<List<String>>(key, updateFlag) {
			public List<String> run() {
				return value;
			}
		});
	}
	
	/*֧�����ֻ��棬ͬһ������ͬһ����ÿ��ֻ��һ������3��  ,key = backmoney_times**/
	/*@SuppressWarnings("unchecked")
	public   Map<String ,String> doBackMoneyCache(String key,
			Map<String ,String> map,String updateFlag) {
		final Map map2 = map;
		return (Map<String ,String>) cacheXMemcache.doCachedTask(new CachedTask<Map<String ,String>>(key, updateFlag) {
			public Map<String ,String> run() {
				return map2;
			}
		});
	}*/

	/**
	 * @param ������
	 * @return
	 */
	/*@SuppressWarnings("unchecked")
	public  List<Long> doBlackCache(String key,List<Long> value,String updateFlag) {
		final List<Long> value1 = value;
		return (List<Long>) cacheXMemcache.doCachedTask(new CachedTask<List<Long>>(key, updateFlag) {
			public List<Long> run() {
				return value1;
			}
		});
	}*/
	
	
	/**
	 * @param ������
	 * @return
	 */
	public  List<Long> doListLongCache(String key,List<Long> value,String updateFlag) {
		final List<Long> value1 = value;
		return (List<Long>) cacheXMemcache.doCachedTask(new CachedTask<List<Long>>(key, updateFlag) {
			public List<Long> run() {
				return value1;
			}
		});
	}
	/**
	 * @param �շ�Ա��Ϣ����
	 * @return
	 */
	/*@SuppressWarnings("unchecked")
	public  String doCollectorMessageCache(String key,String value,String updateFlag) {
		final String value1 = value;
		return (String) cacheXMemcache.doCachedTask(new CachedTask<String>(key, updateFlag) {
			public String run() {
				return value1;
			}
		});
	}*/
	/**
	 * @param ͣ�������ֻ��� �����ϳ���10%����
	 * @return
	 */
	public  Map<Long, Integer> doMapLongIntegerCache(String key,Map<Long, Integer> map,String updateFlag) {
		final Map<Long, Integer> map2 = map;
		return (Map<Long, Integer>) cacheXMemcache.doCachedTask(new CachedTask<Map<Long, Integer>>(key, updateFlag) {
			public Map<Long, Integer> run() {
				return map2;
			}
		});
	}

	public  Map<String, Long> doMapStringLongCache(String key,Map<String, Long> map,String updateFlag) {
		final Map<String, Long> map2 = map;
		return (Map<String, Long>) cacheXMemcache.doCachedTask(new CachedTask<Map<String, Long>>(key, updateFlag) {
			public Map<String, Long> run() {
				return map2;
			}
		});
	}
	
	public  Map<Long, List<Map<String, Object>>> doMapLongListCache(String key,Map<Long, List<Map<String, Object>>> map,String updateFlag) {
		final Map<Long, List<Map<String, Object>>> map2 = map;
		return (Map<Long, List<Map<String, Object>>>) cacheXMemcache.doCachedTask(new CachedTask<Map<Long, List<Map<String, Object>>>>(key, updateFlag) {
			public Map<Long, List<Map<String, Object>>> run() {
				return map2;
			}
		});
	}
	
	
	
	/**
	 * @param ��ȡ��һ���������
	 * @return
	 */
	public Integer readGetHBonusCache(){
		Integer values = doIntegerCache("hbonus_index", null, null);
		logger.error(">>>>hbonus_index:"+values);
		return values;
	}
	
	/**
	 * @param uin�������˻�.һ��ֻ����һ��ȯ
	 * @return
	 */
	public boolean readUseTicketCache(Long uin){
		Long todayLong = TimeTools.getToDayBeginTime();
		Map<Long ,String> map = doMapLongStringCache("usetickets_times", null, null);
		//logger.error(">>>>read>>> uin:"+uin+",map:"+ map);
		if(map!=null&&map.get(uin)!=null){
			String values = map.get(uin);
			logger.error(">>>ͣ��ȯʹ�ô��� ��cache value:"+values+",uin:"+uin+",today:"+todayLong);
			Long tLong = Long.valueOf(values.split("_")[0]);
			Integer times = Integer.valueOf(values.split("_")[1]);
			if(todayLong.intValue()==tLong.intValue()&&times>0)
				return false;
		}
		return true;
	}
	/**
	 * @param uin�������˻�
	 * @return
	 */
	public void updateUseTicketCache(Long uin){
		Map<Long ,String> map = doMapLongStringCache("usetickets_times", null, null);
		Long todayLong = TimeTools.getToDayBeginTime();
		//logger.error(">>>>update>>> uin:"+uin+",map:"+ map);
		if(map!=null){
			if(map.get(uin)!=null){
				String values = map.get(uin);
				
				Long tLong = Long.valueOf(values.split("_")[0]);
				Integer times = Integer.valueOf(values.split("_")[1]);
				if(tLong.intValue()==todayLong.intValue())
					map.put(uin, todayLong+"_"+(times+1));
				else {
					map.put(uin, todayLong+"_"+1);
				}
				logger.error(">>>����ͣ��ȯʹ�ô�������  ��cache value:"+map.get(uin));
			}else {
				map.put(uin, todayLong+"_"+1);
				logger.error(">>>ͣ��ȯ�����״�ʹ�ã�"+uin);
			}
			//logger.error(">>>ͣ��ȯ������� ��"+map);
		}else {
			map = new HashMap<Long, String>();
			map.put(uin, todayLong+"_"+1);
			logger.error(">>>ͣ��ȯ�����״�ʹ�ã�"+uin);
		}
		doMapLongStringCache("usetickets_times", map, "update");
	}
	
	/**
	 * ÿ����ߵĲ��������3000
	 * @return
	 */
	public Double readAllowanceCache(){
		Long today = TimeTools.getToDayBeginTime();
		Double allmoney = 0d;
		Map<Long ,String> map = doMapLongStringCache("allowance_money", null, null);
		if( map != null && map.get(today) != null){
			allmoney = Double.valueOf(map.get(today) + "");
		}
		return allmoney;
	}
	
	/**
	 * ����ÿ�ղ������޻���
	 * @param money
	 */
	public void updateAllowanceCache(Double money){
		Map<Long ,String> map = doMapLongStringCache("allowance_money", null, null);
		Long today = TimeTools.getToDayBeginTime();
		if(map != null){
			if(map.get(today) != null){
				Double allowance = Double.valueOf(map.get(today) + "");
				allowance += money;
				map.put(today, allowance + "");
				logger.error(">>>����ÿ�ղ������޻���  ��cache value:"+allowance+",today:"+today+",money:"+money);
			}else {
				map.put(today, money+"");
				logger.error(">>>�����״λ��棺"+money+",today:"+today);
			}
			//logger.error(">>>ͣ��ȯ������� ��"+map);
		}else {
			map = new HashMap<Long, String>();
			map.put(today, money + "");
			logger.error("�����������޻��棺"+money+",today:"+today);
		}
		doMapLongStringCache("allowance_money", map, "update");
	}
	
	/**
	 * ����ÿ����������������ÿ�ղ�������
	 * @param money
	 */
	public void updateAllowCacheByPark(Long comid,Double money){
		Map<Long ,String> map = doMapLongStringCache("allow_park_money", null, null);
		Long today = TimeTools.getToDayBeginTime();
		logger.error("updateAllowCacheByPark>>>comid:"+comid+",money:"+money+",today:"+today);
		if(map != null){
			if(map.get(comid) != null){
				String info = map.get(comid);
				Long time = Long.valueOf(info.split("_")[0]);//ʱ��
				if(time.intValue() == today.intValue()){
					Double allow = Double.valueOf(info.split("_")[1]);//���ոó�������
					allow += money;
					map.put(comid, today + "_" + allow);
				}else{
					map.put(comid, today + "_" + money);
				}
				logger.error("����������ÿ�ղ�������time:"+time+"comid:"+comid);
			}else{
				map.put(comid, today + "_" + money);
				logger.error("���������������״�ʹ��"+"comid:"+comid);
			}
		}else{
			map = new HashMap<Long, String>();
			map.put(comid, today + "_" + money);
		}
		doMapLongStringCache("allow_park_money", map, "update");
	}
	
	/**
	 * ��ȡÿ�������Ĳ������
	 * @return
	 */
	public Double readAllowCacheByPark(Long comid){
		Double allow = 0d;
		Long today = TimeTools.getToDayBeginTime();
		Map<Long ,String> map = doMapLongStringCache("allow_park_money", null, null);
		if( map != null && map.get(comid) != null){
			String info = map.get(comid);
			Long time = Long.valueOf(info.split("_")[0]);//ʱ��
			if(time.intValue() == today.intValue()){
				allow = Double.valueOf(info.split("_")[1]);//���ոó�������
			}
		}
		return allow;
	}
	
	/**
	 * ��ȡÿ�������Ĳ������
	 * @return
	 */
	public Double readAllowLimitCacheByPark(Long comid){
		Double limit = null;//��ʼֵ��Ϊnull�����������޻���Ͳ�������Ϊ0
		Map<Long ,String> map = doMapLongStringCache("allow_park_limit", null, null);
		if( map != null && !map.isEmpty()){
			if(map.get(comid) != null){
				limit = Double.valueOf(map.get(comid) + "");
			}else{
				limit = 0d;
			}
		}
		return limit;
	}
	
	/*֧����ȯ���棬ͬһ����ÿ��ֻ�ܷ�3��  ,key = backtickets_times**/
	/*@SuppressWarnings("unchecked")
	public   Map<Long ,String> doBackTicketCache(String key,
			Map<Long ,String> map,String updateFlag) {
		final Map map2 = map;
		return (Map<Long ,String>) cacheXMemcache.doCachedTask(new CachedTask<Map<Long ,String>>(key, updateFlag) {
			public Map<Long ,String> run() {
				return map2;
			}
		});
	}*/
	/**
	 * @param uin�������˻�
	 * @return
	 */
	public boolean readBackTicketCache(Long uin){
		Long todayLong = TimeTools.getToDayBeginTime();
		Map<Long ,String> map = doMapLongStringCache("backtickets_times", null, null);
		//logger.error(">>>>read>>> uin:"+uin+",map:"+ map);
		if(map!=null&&map.get(uin)!=null){
			String values = map.get(uin);
			logger.error(">>>ͣ��ȯ��ȯ����  ��cache value:"+values+",uin:"+uin+",today:"+todayLong);
			Long tLong = Long.valueOf(values.split("_")[0]);
			Integer times = Integer.valueOf(values.split("_")[1]);
			if(todayLong.intValue()==tLong.intValue()&&times>0)
				return false;
		}
		return true;
	}
	
	/**
	 * @param uin�������˻�
	 * @return
	 */
	public void updateBackTicketCache(Long uin){
		Long todayLong = TimeTools.getToDayBeginTime();
		Map<Long ,String> map = doMapLongStringCache("backtickets_times", null, null);
		//logger.error(">>>>update>>> uin:"+uin+",map:"+ map);
		if(map!=null){
			if(map.get(uin)!=null){
				String values = map.get(uin);
				
				Long tLong = Long.valueOf(values.split("_")[0]);
				Integer times = Integer.valueOf(values.split("_")[1]);
				if(tLong.intValue()==todayLong.intValue())
					map.put(uin, todayLong+"_"+(times+1));
				else {
					map.put(uin, todayLong+"_"+1);
				}
				logger.error(">>>����ͣ��ȯ��ȯ��������  ��"+map.get(uin));
			}else {
				map.put(uin, todayLong+"_"+1);
				logger.error(">>>ͣ��ȯ����ȯ���״�ʹ�ã�"+uin);
			}
			//logger.error(">>>ͣ��ȯ��ȯ�������汣�棺"+map);
		}else {
			map = new HashMap<Long, String>();
			map.put(uin, todayLong+"_"+1);
			logger.error(">>>ͣ��ȯ����ȯ���״�ʹ�ã�"+uin);
		}
		doMapLongStringCache("backtickets_times", map, "update");
	}

	/**
	 * @param park_uin������_�����˻�
	 * @return
	 */
	public boolean readBackMoneyCache(String park_uin){
		Long todayLong = TimeTools.getToDayBeginTime();
		Map<String,String> map = doMapStringStringCache("backmoney_times", null, null);
		//logger.error(">>>>read>>> park_uin:"+park_uin+",map:"+ map);
		if(map!=null&&map.get(park_uin)!=null){
			String values = map.get(park_uin);
			logger.error(">>>ͣ�����ִ���  ��cache value:"+values+",park_uin:"+park_uin+",today:"+todayLong);
			Long tLong = Long.valueOf(values.split("_")[0]);
			Integer times = Integer.valueOf(values.split("_")[1]);
			//if(todayLong.intValue()==tLong.intValue()&&times>2)
			if(todayLong.intValue()==tLong.intValue()&&times>0)//ͬһ����ͬһ����ֻ��һ����Ԫ
				return false;
		}
		return true;
	}
	/**
	 * @param park_uin������_�����˻�
	 * @return
	 */
	public void updateBackMoneyCache(String park_uin){
		Long todayLong = TimeTools.getToDayBeginTime();
		Map<String,String> map = doMapStringStringCache("backmoney_times", null, null);
		//logger.error(">>>>update>>> park_uin:"+park_uin+",map:"+ map);
		if(map!=null){
			if(map.get(park_uin)!=null){
				String values = map.get(park_uin);
				logger.error(">>>ͣ�����ִ���  ��cache value:"+values+",park_uin:"+park_uin+",today:"+todayLong);
				Long tLong = Long.valueOf(values.split("_")[0]);
				Integer times = Integer.valueOf(values.split("_")[1]);
				if(tLong.intValue()==todayLong.intValue())
					map.put(park_uin, todayLong+"_"+(times+1));
				else {
					map.put(park_uin, todayLong+"_"+1);
				}
				logger.error(">>>����ͣ�����ִ�������  ��cache value:"+map.get(park_uin));
			}else {
				map.put(park_uin, todayLong+"_"+1);
				logger.error(">>>ͣ������ȯ���״�ʹ�ã�"+park_uin);
			}
			//logger.error(">>>ͣ������ȯ���汣�棺"+map);
		}else {
			map = new HashMap<String, String>();
			map.put(park_uin, todayLong+"_"+1);
			logger.error(">>>ͣ������ȯ���״�ʹ�ã�"+park_uin);
		}
		doMapStringStringCache("backmoney_times", map, "update");
	}

	public String getWeixinToken(){
		String weixinToken = doStringCache("zld_weixin_token", null, null);
		if(weixinToken!=null){
			String [] time_token = weixinToken.split("_");
			Long time = Long.valueOf(time_token[0]);
			Long nTime = System.currentTimeMillis()/1000;
			logger.error("weixin token times :"+(nTime-time));
			if(nTime-time<120*60){
				return weixinToken.substring(weixinToken.indexOf("_")+1);
			}
		}
		return "notoken";
	}
	
	public String setWeixinToken(String token){
		return doStringCache("zld_weixin_token", (System.currentTimeMillis()/1000)+"_"+token, "update");
	}
	
	public String getWXPublicToken(){
		String weixinToken = doStringCache("zld_wxpublic_token", null, null);
		if(weixinToken!=null){
			String [] time_token = weixinToken.split("_");
			Long time = Long.valueOf(time_token[0]);
			Long nTime = System.currentTimeMillis()/1000;
			logger.error("wxpublic token times :"+(nTime-time));
			if(nTime-time<120*60){
				return weixinToken.substring(weixinToken.indexOf("_")+1);
			}
		}
		return "notoken";
	}
	
	public String setWXPublicToken(String token){
		return doStringCache("zld_wxpublic_token", (System.currentTimeMillis()/1000)+"_"+token, "update");
	}
	
	public String getJsapi_ticket(){
		String jsapi_ticket = doStringCache("zld_wxpublic_jsapi_ticket", null, null);
		if(jsapi_ticket!=null){
			String [] time_token = jsapi_ticket.split("_");
			Long time = Long.valueOf(time_token[0]);
			Long nTime = System.currentTimeMillis()/1000;
			logger.error("wxpublic jsapi_ticket times :"+(nTime-time));
			if(nTime-time<120*60){
				return jsapi_ticket.substring(jsapi_ticket.indexOf("_")+1);
			}
		}
		return "no_jsapi_ticket";
	}
	
	public String setJsapi_ticket(String ticket){
		return doStringCache("zld_wxpublic_jsapi_ticket", (System.currentTimeMillis()/1000)+"_"+ticket, "update");
	}
	
	public Long getUinUuid(String uuid){
		Map<String,Long> uinUuidMap = doMapStringLongCache("uuid_uin_map", null, null);
		if(uinUuidMap!=null){
			System.err.println(">>>>>>>>>>>>��ͨ���û�����"+uinUuidMap.size());
			return uinUuidMap.get(uuid);
		}else {
			return -1L;
		}
	}
	
	public void setUinUuid(Map<String,Long> uinUuidMap){
		doMapStringLongCache("uuid_uin_map", uinUuidMap, "update");
	}
	
	
	/**
	 * @param ��ȡ���պ������
	 * @return
	 */
	public String readHBonusCache(){
		String values = doStringCache("hbonus_swith", null, null);
		logger.error(">>>>hbonus_swith:"+values);
		return values;
	}
	

	/**
	 * @param д����պ������
	 * @return
	 */
	/*@SuppressWarnings("unchecked")
	public  String doHBonusCache(String key,String value,String updateFlag) {
		final String value1 = value;
		return (String) cacheXMemcache.doCachedTask(new CachedTask<String>(key, updateFlag) {
			public String run() {
				return value1;
			}
		});
	}*/
	

	
}
