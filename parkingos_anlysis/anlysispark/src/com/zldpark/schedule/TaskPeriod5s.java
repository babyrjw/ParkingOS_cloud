package com.zldpark.schedule;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import com.zldpark.service.PgOnlyReadService;
import com.zldpark.utils.ExecutorsUtil;
import com.zldpark.utils.HttpProxy;
import com.zldpark.utils.TimeTools;

/**
 * 10分钟执行一次
 * @author whx
 *
 */
public class TaskPeriod5s implements Runnable {
	
	private PgOnlyReadService pgOnlyReadService;
	
	public TaskPeriod5s(PgOnlyReadService pgOnlyReadService){
		this.pgOnlyReadService = pgOnlyReadService;
	}

	private static Logger log = Logger.getLogger(TaskPeriod5s.class);
	
	@Override
	public void run() {
		log.error("********************开始2分钟一次上传黄岩车场数据的定时任务***********************");
		//坐标已从百度转为高德系
		String [] parks1 ={"20368","黄岩影城停车场","浙江省台州市黄岩区青年东路12","121.2650637","28.646038","72","72","TRUE","0:00-24:00"};
		String [] parks2 ={"20429","横街东路","浙江省台州市黄岩区横街38","121.268887","28.642576","43","43","TRUE","08:00-21:00"};
		String [] parks3 ={"20428","青年中路","浙江省台州市黄岩区青年东路100号","121.266435","28.646231","19","19","TRUE","08:00-21:00"};
		String [] parks4 ={"20411","天长北路（南）","浙江省台州市黄岩区天长北路8号","121.266776","28.646656","27","27","TRUE","08:00-21:00"};
		String [] parks5 ={"20412","天长北路（北）","浙江省台州市黄岩区天长北路101号","121.266632","28.648679","76","76","TRUE","08:00-21:00"};
		String [] parks6 ={"20414","天长南路（南）","浙江省台州市黄岩区天长南路203号","121.267192","28.639902","73","66","TRUE","08:00-21:00"};
		String [] parks7 ={"20415","天长南路（北）","浙江省台州市黄岩区天长南路85号","121.266951","28.642981","15","15","TRUE","08:00-21:00"};
		String [] parks8 ={"20426","县前街","浙江省台州市黄岩区天长北路99号","121.266487","28.648561","105","105","TRUE","08:00-18:00"};
		String [] parks9 ={"20427","青年东路","浙江省台州市黄岩区青年东路25号","121.269961","28.645809","58","46","TRUE","08:00-21:00"};
		String [] parks10 ={"21012","黄岩金尚停车场","浙江省台州市黄岩区引泉路190","121.26001","28.634775","115","108","TRUE","0:00-24:00"};
		String [] parks11 ={"20418","劳动北路","浙江省台州市黄岩区劳动北路71号","121.263252","28.648025","26","12","TRUE","08:00-21:00"};
		String [] parks12 ={"20419","劳动南路","浙江省台州市黄岩区劳动南路89号","121.263468","28.643084","70","70","TRUE","08:00-21:00"};
		String [] parks13 ={"20420","北门广场","浙江省台州市黄岩区县前街113号","121.264081","28.648402","126","89","TRUE","08:00-18:00"};
		String [] parks14={"20421","青年西路","浙江省台州市黄岩区青年西路6号","121.262884","28.646116","80","45","TRUE","08:00-21:00"};
		String [] parks15 ={"20422","横街西路","浙江省台州市黄岩区横街路242号","121.263028","28.643186","124","58","TRUE","08:00-21:00"};
		final List<String[]> parks = new ArrayList<String[]>();
		parks.add(parks1);
		parks.add(parks2);
		parks.add(parks3);
		parks.add(parks4);
		parks.add(parks5);
		parks.add(parks6);
		parks.add(parks7);
		parks.add(parks8);
		parks.add(parks9);
		parks.add(parks10);
		parks.add(parks11);
		parks.add(parks12);
		parks.add(parks13);
		parks.add(parks14);
		parks.add(parks15);
		try {
			ExecutorService pool = ExecutorsUtil.getExecutorService();
			pool.execute(new Runnable() {
				@Override
				public void run() {
					//台州项目5秒上传一次车场余位数
					List<Map<String, Object>> parkEmptys = pgOnlyReadService.getAll("select sum(total) as total,comid from remain_berth_tb where comid in(select id from com_info_tb where" +
							" groupid in(select id from org_group_tb where cityid =?)) group by comid", new Object[]{2L});
					log.error("查询黄岩车场数据："+parkEmptys);
					if(parkEmptys!=null&&!parkEmptys.isEmpty()){
						for(Map<String, Object> map:parkEmptys){
							Long cid = (Long)map.get("comid");
							for(String[] park:parks){
								Long _cid = Long.valueOf(park[0]);
								if(cid.equals(_cid)){
									park[6]=map.get("total")+"";
									break;
								}
							}
						}
					}
					/*
					 * 	 * @param in2 parkid
						 * @param in1 name
						 * @param in4 address
						 * @param in3 lat
						 * @param in0 lng
						 * @param in9 empty_plot
						 * @param in10 total_plot
						 * @param in5 updatetime
						 * @param in6 isfee
						 * @param in7 opentime
					 */
					try {
						List<Map<String, Object>> data = new ArrayList<Map<String,Object>>();
						for(String[] park:parks){
							Map<String, Object> map = new HashMap<String, Object>();
							map.put("parkid",park[0]);
							map.put("name",park[1]);
							map.put("address",park[2]);
							map.put("lat",park[3]);
							map.put("lng",park[4]);
							map.put("total_plot",park[5]);
							map.put("empty_plot",park[6]);
							map.put("isfee",park[7]);
							map.put("opentime",park[8]);
							map.put("updatetime",TimeTools.gettime());
							data.add(map);
						}
						String jsonData="";
						String da = createJson(data);
						//log.error(da);
						jsonData = URLEncoder.encode(da,"UTF-8");
						//jsonData =createJson(data);
						HttpProxy httpProxy = new HttpProxy();
						log.error("开始调用发送黄岩车场数据接口，数据长度："+jsonData.length());
						Map<String, String> params = new HashMap<String, String>();
						params.put("data", jsonData);
						String result = httpProxy.doPost("http://127.0.0.1/taizhou/servlet/SendParkData",params);
						log.error("调用发送黄岩车场数据接口，返回："+result);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.error("********************结束5秒钟一次的定时任务***********************");
	}
	
	public String createJson(List<Map<String, Object >> info){
		String json = "[";
		int i=0;
		int j=0;
		if(info!=null&&info.size()>0){
			for(Map<String, Object > map : info){
				if(i!=0)
					json +=",";
				json+="{";
				for(String key : map.keySet()){
					if(j!=0)
						json +=",";
					Object v = map.get(key);
					if(v!=null)
						v = v.toString().trim();
					json +="\""+key+"\":\""+v+"\"";
					j++;
				}
				json+="}";
				i++;
				j=0;
			}
			
		}
		json +="]";
		return json;
	}
	/*
	//百度转高德坐标系
	public static Double[] baidu2gaode(Double lng,Double lat){
	    double x = lng - 0.0065, y = lat - 0.00588;
	    double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * Math.PI);
	    double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * Math.PI);
	    Double ggLng = z * Math.cos(theta);
	    Double ggLat = z * Math.sin(theta);
	    return new Double[]{ggLng,ggLat};
	}
	
	public static void main(String[] args) {
		Double lng = 121.269604;
		Double lat = 28.648824;
		Double[] ret = baidu2gaode(lng, lat);
		log.error(ret[0]+","+ret[1]);
		//121.266435008888,28.646111438852433
	}*/
}
