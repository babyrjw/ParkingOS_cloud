package com.zldpark.schedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.zldpark.impl.CommonMethods;
import com.zldpark.service.DataBaseService;
import com.zldpark.service.PgOnlyReadService;
import com.zldpark.utils.Constants;
import com.zldpark.utils.HttpProxy;
import com.zldpark.utils.MemcacheUtils;
import com.zldpark.utils.StringUtils;
import com.zldpark.utils.TimeTools;

public class ParkSchedule implements Runnable {
	
	private DataBaseService dataBaseService;
	private PgOnlyReadService pgOnlyReadService;
	private MemcacheUtils memcacheUtils;
	
	public ParkSchedule(DataBaseService dataBaseService, PgOnlyReadService pgOnlyReadService,
			MemcacheUtils memcacheUtils, CommonMethods commonMethods){
		this.dataBaseService = dataBaseService;
		this.pgOnlyReadService = pgOnlyReadService;
		this.memcacheUtils = memcacheUtils;
	}

	private static Logger log = Logger.getLogger(ParkSchedule.class);

	@Override
	public void run() {
		// TODO Auto-generated method stub

		log.error("********************��ʼ1��һ�εĶ�ʱ����***********************");
		//һ��ͳ��һ��
		//��ȡ����Ŀ�ʼʱ��
		Long todaybeigintime = TimeTools.getToDayBeginTime();
		try {
			mobilePayStart(todaybeigintime);
			//ͳ��ע����
			registerStart(todaybeigintime);
			//�ֻ���������ն���ͳ��
			order3Start(todaybeigintime);
			//ֱ��ͳ��
			directPayStart(todaybeigintime);
			//ͳ�����
			anlysisBonus(todaybeigintime);
			//΢��֧�������������û�ͳ��
			consumeStart(todaybeigintime);
			//�����ͣ��ȯ�����˿�
			backTicketMoney();
			//���ݳ���ÿ��ó��������㲹����
			allowanceByPark(todaybeigintime);
			
			anlyCharge(todaybeigintime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.error("********************����1��һ�εĶ�ʱ����***********************");
	}
	
	
	
	private void allowanceByPark(Long time){
		try {
			log.error("allowanceByPark>>>��ʼ���ݶ������������쳵��������>>>");
			Map<Long, String> limitMap = new HashMap<Long, String>();
			Long allcount = 0L;
			List<Map<String, Object>> list = pgOnlyReadService
					.getAll("select count(id) ocount,comid from order_tb where state=? and pay_type=? and comid>? and end_time between ? and ? group by comid ",
							new Object[] { 1, 2, 0, time - 24*60*60, time });
			if(list != null && !list.isEmpty()){
				for(Map<String, Object> map : list){
					Long count = (Long)map.get("ocount");
					allcount += count;
				}
				log.error("allowanceByPark>>>�����ܵ���allcount:"+allcount);
				if(allcount > 0){
					Double allowance = StringUtils.formatDouble(getAllowance(time));//3000d;
					log.error("allowanceByPark>>>���ղ����ܶ��:"+allowance);
//				if(allowance<1000||allowance>3000)
//					allowance=1000d;
//				log.error("allowanceByPark>>>���ղ����ܶ�ȣ�ʵ�ʣ�:"+allowance);
					for(Map<String, Object> map : list){
						Long comid = (Long)map.get("comid");
						Long count = (Long)map.get("ocount");
						Double limit = StringUtils.formatDouble(((double)count/(double)allcount) * allowance);
						limitMap.put(comid, limit + "");
					}
				}
			}
			log.error("allowanceByPark>>>limitMap:"+limitMap.size());
			memcacheUtils.doMapLongStringCache("allow_park_limit", limitMap, "update");
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	//2015-11-05 ��ʼ��ÿ���100,��100ֹͣ
	private Double getAllowance(Long time) {
		Long baseTime = 1446652800L;//2015-11-05
		Long abs = time-baseTime;
		Long t  = abs/(24*60*60);
		log.error(">>>>>�����ݼ�100�ı�����"+t);
		if(t>0){
			Double retDouble= 500d-t*100;
			if(retDouble<0d)
				retDouble=0d;
			return retDouble;
		}
		return 100.0;
	}
	private void anlyFlyGameScore(Long ntime){
		log.error(">>>>>>>>������Ϸ����ͳ��......");
		Long btime =ntime  - 24*60*60;//����һ�����Ϸս��
		List allList = pgOnlyReadService.getAll("select * from flygame_score_tb where ctime between ? and ?", new Object[]{btime,ntime});
		if(allList!=null&&!allList.isEmpty()){
			Map<Long,Map<Integer,Map<String, Double>>> alldataMap = new HashMap<Long,Map<Integer, Map<String,Double>>>();
			for(int i=0;i<allList.size();i++){
				Map<String,Object> map =(Map) allList.get(i);
				Long uin = (Long)map.get("uin");
				Map<Integer,Map<String, Double>> dataMap = alldataMap.get(uin);
				if(dataMap==null){
					dataMap=new HashMap<Integer, Map<String,Double>>();
					for(int j=1;j<7;j++){
						Map<String, Double> m = new HashMap<String, Double>();
						m.put("score", 0.0);
						m.put("count", 0.0);
						dataMap.put(j, m);
					}
					alldataMap.put(uin, dataMap);
				}
				Double money =StringUtils.formatDouble(map.get("money"));
				//0ͣ����ͣ��ȯ 1����ͣ��ȯ 2���ȯ 3���ȯ 4��ո��� 5��������
				Integer ptype = (Integer)map.get("ptype");
				if(ptype==0)
					ptype=1;
				Map<String, Double> sMap = dataMap.get(ptype);
				Double score = sMap.get("score");
				if(ptype==1){
					score +=money*0.1;
				}else if(ptype==2){
					score +=money*0.5;
				}else if(ptype==3){
					score +=0.3;
				}else if(ptype==4){
					score +=-1;
				}else if(ptype==5){
					score +=1;
				}else if(ptype==6){
					score +=2;
				}
				sMap.put("score", StringUtils.formatDouble(score));
				sMap.put("count", sMap.get("count")+1);
			}
			//д�����ݿ�
			if(!alldataMap.isEmpty()){
				String sql ="insert into flygame_score_anlysis_tb(uin,ctime,db_bullet_count,db_bullet_score,empty_bullet_count,empty_bullet_score," +
						"gift_count,gift_score,balance_count,balance_score,ticket_count,ticket_score,second_count,second_score) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				List<Object[]> values = new ArrayList<Object[]>();
				for(Long uin: alldataMap.keySet()){
					//0ͣ����ͣ��ȯ 1����ͣ��ȯ 2���ȯ 3���ȯ 4��ո��� 5��������
					Map<Integer,Map<String, Double>> dataMap = alldataMap.get(uin);
					values.add(new Object[]{uin,ntime,dataMap.get(5).get("count").intValue(),dataMap.get(5).get("score")
							,dataMap.get(4).get("count").intValue(),dataMap.get(4).get("score")
							,dataMap.get(3).get("count").intValue(),dataMap.get(3).get("score")
							,dataMap.get(2).get("count").intValue(),dataMap.get(2).get("score")
							,dataMap.get(1).get("count").intValue(),dataMap.get(1).get("score")
							,dataMap.get(6).get("count").intValue(),dataMap.get(6).get("score")});
				}
				if(!values.isEmpty()){
					int ret = dataBaseService.bathInsert(sql, values, new int[]{4,4,4,3,4,3,4,3,4,3,4,3,4,3});
					log.error(">>>>>>>>��Ϸ����ͳ����ϣ�������"+ret+"��");
				}
			}
		}
	}
	
	private void backTicketMoney() {
		try {
			log.error("������ͣ��ȯ�����˿�.....");
			List allList = pgOnlyReadService.getAll("select * from ticket_tb where limit_day<? and state=? and resources=? and is_back_money=? ",
					new Object[]{System.currentTimeMillis()/1000,0,1,0});
			String sql = "insert into user_account_tb(uin,amount,type,create_time,remark,pay_type,target) values(?,?,?,?,?,?,?)";
			List<Object[]> values = new ArrayList<Object[]>();
			//ÿ���������˿��ܽ��
			Map<Long, Double> uinMoneyMap = new HashMap<Long, Double>();
			Map<Long, Integer> uinMoneyCount = new HashMap<Long, Integer>();
			if(allList!=null&&!allList.isEmpty()){
				log.error("�˿"+allList);
				for(int i=0;i<allList.size();i++){
					Map map = (Map)allList.get(i);
					Long uin =(Long)map.get("uin");
					Double money = StringUtils.formatDouble(map.get("pmoney"));
					if(money==null||money==0)
						continue;
					if(uinMoneyMap.containsKey(uin)){
						uinMoneyMap.put(uin, uinMoneyMap.get(uin)+money);
						uinMoneyCount.put(uin, uinMoneyCount.get(uin)+1);
					}else {
						uinMoneyMap.put(uin, money);
						uinMoneyCount.put(uin, 1);
					}
					values.add(new Object[]{uin,money,0,System.currentTimeMillis()/1000+i,"ͣ��ȯ�˿�",13,3});
				}
				if(values.size()>0){
					int r = dataBaseService.bathInsert(sql, values, new int[]{4,3,4,4,12,4,4});
					if(r>0){
						log.error(">>>>>>>�˿д���˻���ϸ��"+r+"��");
						sql = "update user_info_Tb set balance =balance+? where id=?";
						values.clear();
						log.error("�˿д���˻�,"+uinMoneyMap);
						for(Long uLong : uinMoneyMap.keySet()){
							values.add(new Object[]{uinMoneyMap.get(uLong),uLong});
						}
						if(values.size()>0){
							r = dataBaseService.bathInsert(sql, values, new int[]{3,4});
							log.error(">>>>>>>>>>�˿д���˻���"+r+"��");
							if(r>0){
								// is_back_money integer DEFAULT 0, -- ���������ͣ��ȯ��δʹ�ù����˿0δ�˿1���˿�
								r = dataBaseService.update("update ticket_tb set is_back_money=? where  limit_day<? and state=? and resources=? and is_back_money=? ",
										new Object[]{1,System.currentTimeMillis()/1000,0,1,0});
								log.error(">>>>>>>>>>�˿����ͣ��ȯ״̬��"+r+"��");
								for(Long uLong : uinMoneyMap.keySet()){
									log.error(">>>>>>�����ں���Ϣ����"+uLong);
									sendMesgToWeixin(uLong, StringUtils.formatDouble(uinMoneyMap.get(uLong)),uinMoneyCount.get(uLong));
									int ret = dataBaseService.update("insert into user_message_tb (type,ctime,content,uin,title) values (?,?,?,?,?)",
											new Object[]{9,System.currentTimeMillis()/1000,"����"+uinMoneyCount.get(uLong)+"�Ź����ͣ��ȯ���ڣ���Ӧ���˿���"+uinMoneyMap.get(uLong)+"Ԫ�Ѿ��˿�����˻���ע����ա�",uLong,"�����ͣ��ȯ���ڣ������Ӧ�˿�"});
									log.error(">>>>>>д������Ϣ��"+uLong);
								}
							}
						}
					}
				}
			}else {
				log.error("�˿�����0");
			}
			log.error("������ͣ��ȯ�����˿����.....");
		} catch (Exception e) {
			log.error(e);
		}
	}
	/**
	 * �����˿���Ϣ�����ں�
	 * @param uin
	 * @param openid
	 * @param back
	 */
	private void sendMesgToWeixin(Long uin,Double back,Integer count){
		String openid ="";
		Map userMap = pgOnlyReadService.getMap("select wxp_openid from user_info_tb where id =? ",new Object[]{uin});
		if(userMap!=null&&!userMap.isEmpty())
			openid = (String)userMap.get("wxp_openid");
		try {
			if(openid!=null&&!openid.equals("")){
				log.error(">>>>>>>>>>>Ԥ֧�����ֽ���㶩���˻�Ԥ֧����   ΢������Ϣ,uin:"+uin+",openid:"+openid);
				String first = "�����ͣ��ȯ�����˿�";
				Map<String, String> baseinfo = new HashMap<String, String>();
				List<Map<String, String>> orderinfo = new ArrayList<Map<String,String>>();
				String url = "http://s.tingchebao.com/zld/wxpaccount.do?action=balance&openid="+openid;
				baseinfo.put("url", url);
				baseinfo.put("openid", openid);
				baseinfo.put("top_color", "#000000");
				baseinfo.put("templeteid",Constants.WXPUBLIC_BACK_NOTIFYMSG_ID);
				Map<String, String> keyword1 = new HashMap<String, String>();
				keyword1.put("keyword", "orderProductPrice");
				keyword1.put("value",count+"�ţ���"+back+"Ԫ");
				keyword1.put("color", "#000000");
				orderinfo.add(keyword1);
				Map<String, String> keyword2 = new HashMap<String, String>();
				keyword2.put("keyword", "orderProductName");
				keyword2.put("value", "ͣ��ȯ�����˿�");
				keyword2.put("color", "#000000");
				orderinfo.add(keyword2);
				Map<String, String> keyword3 = new HashMap<String, String>();
				keyword3.put("keyword", "orderName");
				keyword3.put("value", "");
				keyword3.put("color", "#000000");
				orderinfo.add(keyword3);
				Map<String, String> keyword4 = new HashMap<String, String>();
				keyword4.put("keyword", "Remark");
				keyword4.put("value", "���������˻���");
				keyword4.put("color", "#000000");
				orderinfo.add(keyword4);
				Map<String, String> keyword5 = new HashMap<String, String>();
				keyword5.put("keyword", "first");
				keyword5.put("value", first);
				keyword5.put("color", "#000000");
				orderinfo.add(keyword5);
				StringUtils.sendWXTempleteMsg(baseinfo, orderinfo,getWXPAccessToken());
			}
		} catch (Exception e) {
			log.error("�˻سɹ�����Ϣ����ʧ��");
			e.printStackTrace();
		}
	}
	
	public  String getWXPAccessToken(){
		String access_token = memcacheUtils.getWXPublicToken();
		if(access_token.equals("notoken")){
			String url = Constants.WXPUBLIC_GETTOKEN_URL;
			//��weixin�ӿ�ȡaccess_token
			String result = new HttpProxy().doGet(url);
			log.error("wxpublic_access_token json:"+result);
			access_token =StringUtils.getJsonValue(result, "access_token");//result.substring(17,result.indexOf(",")-1);
			log.error("wxpublic_access_token:"+access_token);
			//���浽���� 
			memcacheUtils.setWXPublicToken(access_token);
		}
		log.error("΢�Ź��ں�access_token��"+access_token);
		return access_token;
	}
	/*
	 * ÿ��һ�����ֲ���1000���շ�Ա�����ó�1000
	 */
	private void resetRewardScore(Long createtime){
		log.error("reset rewardscore to 1000 every monday, write detail>>>begining.......");
		List<Map<String, Object>> rewardscoreList = pgOnlyReadService
				.getAll("select id,reward_score from user_info_tb where reward_score<? and state=? and (auth_flag=? or auth_flag=?) ",
						new Object[] { 1000, 0, 1, 2 });
		if(rewardscoreList != null){
			String sql = "insert into reward_account_tb(uin,score,type,create_time,remark,target) values (?,?,?,?,?,?)";
			String sql1 = "update user_info_tb set reward_score=? where id=? ";
			List<Object[]> values = new ArrayList<Object[]>();
			List<Object[]> values1 = new ArrayList<Object[]>();
			for(Map<String, Object> map : rewardscoreList){
				Long uin = (Long)map.get("id");
				Double rewardscore = Double.valueOf(map.get("reward_score") + "");
				Double addscore = com.zldpark.utils.StringUtils.formatDouble(1000 - rewardscore);
				Object[] va = new Object[6];
				Object[] va1 = new Object[2];
				va[0] = uin;
				va[1] = addscore;
				va[2] = 0;
				va[3] = createtime;
				va[4] = "ͣ������ֵ"+addscore+"����";
				va[5] = 3;
				values.add(va);
				
				va1[0] = 1000d;
				va1[1] = uin;
				values1.add(va1);
			}
			dataBaseService.bathInsert(sql, values, new int []{4,3,4,4,12,4});
			dataBaseService.bathInsert(sql1, values1, new int []{3,4});
			log.error("reset rewardscore to 1000 every monday, write detail>>>end:"+rewardscoreList.size());
		}
	}
	
	/*
	 * ��ʼͳ��
	 */
	private void consumeStart(Long nextTime){
		try {
			int consumecount = dataBaseService.update("delete from consume_anlysis_tb where create_time=? ",
					new Object[]{nextTime - 24*60*60});
			log.error("ɾ��΢��֧�������������û�ͳ��==="+consumecount+"��");
			System.err.println("��ʼ΢��֧���������û�ͳ�ƣ�"+TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis()));
			List<Object> wxuins = new ArrayList<Object>();
			List<Object> wxpuins = new ArrayList<Object>();
			List<Object> zfbuins = new ArrayList<Object>();
			List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
			list = pgOnlyReadService.getAll("select uin,pay_type from user_account_tb where type=? and (pay_type=? or pay_type=? or pay_type=?) group by uin,pay_type order by pay_type desc ",
					new Object[]{0,1,2,9});
			for(Map<String, Object> map : list){
				Integer pay_type = (Integer)map.get("pay_type");
				Long uin = (Long)map.get("uin");
				if(pay_type == 9){//΢�Ź��ں��û�
					wxpuins.add(uin);
				}else if(pay_type == 2){//΢�ų�ֵ�û�
					wxuins.add(uin);
				}else if(pay_type == 1){//֧������ֵ�û�
					zfbuins.add(uin);
				}
			}
			List<Map<String, Object>> list2 = new ArrayList<Map<String,Object>>();
			List<Map<String, Object>> list3 = new ArrayList<Map<String,Object>>();
			List<Object> duins = new ArrayList<Object>();
			List<Object> ouins = new ArrayList<Object>();
			String sql1 = "select uin,min(create_time) mintime from user_account_tb where type=? and uid>? group by uin order by mintime desc ";
			String sql2 = "select uin,min(end_time) mintime from order_tb where pay_type=? and state=? group by uin order by mintime desc ";
			list2 = pgOnlyReadService.getAll(sql1, new Object[]{1,0});
			for(Map<String, Object> map : list2){
				Long mintime = (Long)map.get("mintime");
				Long uin = (Long)map.get("uin");
				if(mintime > (nextTime - 24*60*60)){
					duins.add(uin);
				}else{
					break;
				}
			}
			list3 = pgOnlyReadService.getAll(sql2, new Object[]{2,1});
			for(Map<String, Object> map : list3){
				Long mintime = (Long)map.get("mintime");
				Long uin = (Long)map.get("uin");
				if(mintime > (nextTime - 24*60*60)){
					ouins.add(uin);
				}else{
					break;
				}
			}
			List<Object> newwx = new ArrayList<Object>();
			List<Object> newwxp = new ArrayList<Object>();
			List<Object> newzfb = new ArrayList<Object>();
			for(Object object : duins){
				if(wxuins.contains(object)){
					newwx.add(object);
				}
				if(wxpuins.contains(object)){
					newwxp.add(object);
				}
				if(zfbuins.contains(object)){
					newzfb.add(object);
				}
			}
			for(Object object : ouins){
				if(wxuins.contains(object) && !newwx.contains(object)){
					newwx.add(object);
				}
				if(wxpuins.contains(object) && !newwxp.contains(object)){
					newwxp.add(object);
				}
				if(zfbuins.contains(object) && !newzfb.contains(object)){
					newzfb.add(object);
				}
			}
			int wxcount = newwx.size();
			int zfbcount = newzfb.size();
			int wxpcount = newwxp.size();
			
			log.error("΢��֧���������û�ͳ�ƿ�ʼд��...��1��");
			System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"΢��֧���������û�ͳ�ƿ�ʼд��...��1��");
			String sql = "insert into consume_anlysis_tb(create_time,wx_total,zfb_total,wxp_total) values(?,?,?,?)";
			dataBaseService.update(sql, new Object[]{nextTime - 24*60*60,wxcount,zfbcount,wxpcount});
			System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"΢��֧���������û�ͳ��д�����...");
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	/**
	 * ͳ�ƺ��ת��
	 * @param todaybeigintime
	 */
	private void anlysisBonus(Long todaybeigintime) {
		try {
			int bonuscount = dataBaseService.update("delete from reg_anlysis_tb where ctime=?", new Object[]{todaybeigintime-24*60*60});
			log.error("ɾ�����ͳ������==="+bonuscount+"��");
			/**
			 *   id bigint NOT NULL,
			  bonus_num integer, -- �����
			  reg_num integer, -- ע������������Ч��
			  amount integer, -- ���
			  pv_number integer, -- pv��
			  hit_number integer, -- �����
			  down_num integer, -- ������
			  ctime bigint, -- ����
			  atype integer, 1����ͷ����2���������3���պ����998ֱ�������999�շ�Ա�Ƽ���1000���׺��
			  order_num integer, -- ����������
			  --���ͳ��
				select * from user_account_tb where uin in( --�������
				select id from user_info_tb where mobile in( --ע���û�
				select mobile from bonus_record_tb where ctime between 1420560000 and 1420646400) --���������
				and reg_time  between 1420560000 and 1420646400 and auth_flag=4  )
			 */
			log.error("ɾ�����ͳ������==="+bonuscount+"��");
			//1���պ������
			List<Map<String, Object>> bList = pgOnlyReadService.getAll("select count(id) count,bid from bonus_record_tb where ctime between ? and ? group by bid ",
					new Object[]{todaybeigintime-24*60*60,todaybeigintime}) ;
			/*1.8
			 * 1;5155
			2;5432
			418;1
			25;2
			 */
			log.error(bList);
			//2ע��������Ч���ƣ�:
			List<Map<String, Object>> rList = pgOnlyReadService.getAll("select count(ID) count,media from user_info_tb where id in(select uin from " +
					" car_info_tb) and  reg_time  between ? and ? and auth_flag=? and media>?  group by media ", 
					new Object[]{todaybeigintime-24*60*60,todaybeigintime,4,0});
			/*1.8
			 * 15;999
			83;1
			9;2
			2;1000
			 */
			log.error(rList);
			
			
			//������д������  1000={}
			Map<String, List<Long>> dataMap = new HashMap<String, List<Long>>();
			//�����������
			if(bList!=null&&!bList.isEmpty()){
				for(Map<String, Object> map : bList){
					Long bid = (Long)map.get("bid");
					if(bid!=null&&bid>999){
						bid = 1000L;
					}else if(bid==null)
						continue;
					Long count = (Long)map.get("count");
					if(bid!=null){
						if(dataMap.containsKey(bid+"")){
							List<Long> dList = dataMap.get(bid+"");
							Long cLong = dList.get(0);
							//log.error(cLong);
							cLong = cLong+count;
							//log.error(dList);
							dList.remove(0);
							dList.add(cLong);
							//dataMap.put(bid+"",dList);
						}else {
							List<Long> dList = new ArrayList<Long>();
							dList.add(count);
							dataMap.put(bid+"", dList);
						}
					}
				}
			}
			log.error(dataMap);
			//����ע������ 
			if(rList!=null&&!rList.isEmpty()){
				for(Map<String,Object> map : rList){
					Integer media = (Integer)map.get("media");
					if(media==null)
						continue;
					Long count = (Long)map.get("count");
					String mkey = media+"";
					if(dataMap.containsKey(mkey)){
						List<Long> dList = dataMap.get(mkey);
						dList.add(count);
					}else {
						List<Long> dList = new ArrayList<Long>();
						dList.add(0L);
						dList.add(count);
						dataMap.put(mkey, dList);
					}
				}
				for(String key : dataMap.keySet()){
					List<Long> vList = dataMap.get(key);
					if(vList.size()==1)
						vList.add(0L);
					dataMap.put(key, vList);
				}
			}
			
			log.error(dataMap);
			//3���ղ���������
			if(dataMap!=null&&dataMap.size()>0){
				String sql = "insert into reg_anlysis_tb (bonus_num,reg_num,order_num,atype,ctime) values(?,?,?,?,?)";
				List<Object[]> values = new ArrayList<Object[]>();
				for(String key : dataMap.keySet()){
					Long ocount = pgOnlyReadService.getLong("select count(distinct uin) from user_account_tb where uin in(select id from user_info_Tb " +
							"where reg_time  between ?  and ? and auth_flag=? and media=? ) and type=? and remark like ? and create_time between ? and ? ", 
							new Object[]{todaybeigintime-24*60*60,todaybeigintime,4,Integer.valueOf(key),1,"ͣ����%",todaybeigintime-24*60*60,todaybeigintime});
					List<Long> vlList = dataMap.get(key);
					Object[] valuObjects = new Object[]{vlList.get(0),vlList.get(1),ocount,Integer.valueOf(key),todaybeigintime-24*60*60};
					values.add(valuObjects);
					System.out.println("sql="+sql+",params:"+objArry2String(valuObjects));
				}
				//д�����ݿ�
				if(values.size()>0){
					int ret = dataBaseService.bathInsert(sql, values, new int[]{4,4,4,4,4});
					log.error("���ͳ�����ݽ�����������==="+ret+"��");
				}
			}
		} catch (Exception e) {
			log.error(e);
		}
		
	}
	public  String objArry2String(Object[] values){
		StringBuffer rBuffer = new StringBuffer();
		if(values!=null&&values.length>0){
			for(Object o : values){
				rBuffer.append(o+",");
			}
		}
		return rBuffer.toString();
	}
	//ÿ15����ʱ�̿�ʼ
	private Long getTime (){
		Long time = System.currentTimeMillis()/1000;
		time = time -time%(15*60) ;
		return time;
	}
	
	//ÿ10���ӿ�ʼ
	private Long gethTime(){
		Long time = System.currentTimeMillis()/1000;
		time = time -time%(10*60) ;
		return time;
	}
	
	/*
	 * ��ʼͳ��
	 */
	private void hasparkerStart(Long nextTime){
		System.err.println("��ʼ�շ�Ա�ڸڿ�֧������ͳ�ƣ�"+TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis()));
		Long count = pgOnlyReadService.getLong("select count(*) total from com_info_tb where epay=? and is_hasparker=?",
				new Object[]{1,1});
		String sql = "insert into hasparker_anlysis_tb(anlysis_time,total) values (?,?)";
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"�շ�Ա�ڸڿ�֧������ͳ�ƿ�ʼд��...");
		dataBaseService.update(sql, new Object[]{nextTime,count});
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"�շ�Ա�ڸڿ�֧������ͳ��д�����...");
	}
	
	/*
	 * ͳ���շ�Ա�������,�ڸڻ���
	 * @param nextTime
	 */
	
	private void anlysisParkerOnline(Long ntime){
		try {
			//����������
			log.error(">>>>��ʼ��ѯ�����շ�Ա��Ѳ��Ա....");
			List<Map<String,Object>> uinMap = pgOnlyReadService.getAll("select id from user_info_tb where auth_flag in(?,?,?)", 
					new Object[]{1,2,16});
			List<Long> uinList = new ArrayList<Long>();
			if(uinMap != null && !uinMap.isEmpty()){
				for(Map<String,Object> uMap : uinMap){
					uinList.add((Long)uMap.get("id"));
				}
			}
			Map<Long , Long> userMapCache = memcacheUtils.readParkerTokentimCache(uinList);
			Map<Long , Long> userMap = new HashMap<Long, Long>();
			//���˵�����ʱ�䳬��10���ӵ��շ�Ա
			if(userMapCache != null && !userMapCache.isEmpty()){
				for(Long key : userMapCache.keySet()){
					if(userMapCache.get(key) > ntime - 10*60){
						userMap.put(key,userMapCache.get(key));
					}
				}
			}
			log.error(">>>>>>��ǰ����������"+userMap.size());
			//������С��10������λ���շ�Ա����Ϊ�����ߵ��շ�Ա������������Ϊ������������ڡ�
			//��ѯ�ϴ�����γ�ȵ��շ�Ա���� �û��ϴ��ص����
			String sql ="select uid ,is_onseat ,max(ctime) ctime from user_local_tb group by uid ,is_onseat order by uid";
			
			List<Map<String,Object>>  uidList = dataBaseService.getAll(sql,null);
			log.error(">>>>�ϴ���γ�ȴ�С ��"+uidList.size());
			
			Map<Long, String> uList = new HashMap<Long, String>();
			for(Map<String,Object> map : uidList){
				Long uid = (Long)map.get("uid");
				Integer isOnseat=(Integer)map.get("is_onseat");
				Long ctime = (Long)map.get("ctime");
				if(uList.containsKey(uid)){
					String v = uList.get(uid);
					Long ptime = Long.valueOf(v.split("_")[0]);
					if(ctime>ptime){
						uList.put(uid,ctime+"_"+isOnseat);
					}
				}else {
					uList.put(uid,ctime+"_"+isOnseat);
				}
			}
			
			//�����������У��������û��ϴ��ص����ʱ�����ϰ汾�û������������У������������ڸڣ�//�°汾�շ�Ա��Ҫ��ѯ����ϴ���γ��ʱ��С�ڵ�ǰʱ��30�������ڵ���Ϊ�ڸ�
			//�ڸڼ���
			List<Long> onlineList = new ArrayList<Long>();
			
			for(Long key : userMap.keySet()){
				if(!uList.containsKey(key)){//�����������У��������û��ϴ��ص����ʱ�����ϰ汾�û������������У������������ڸ�
					onlineList.add(key);
				}else {//�����������У����û��ϴ��ص����ʱ�������һ���ϴ���γ�ȵ�ʱ�� �������30����ǰ�ģ���Ϊ�����ߣ�30�������ڵ����ڸ�
					String v = uList.get(key);
					Long time =Long.valueOf(v.split("_")[0]);
					Integer isOnseat = Integer.valueOf(v.split("_")[1]);
					if(isOnseat==1&&time>(ntime-20*60))
						onlineList.add(key);
				}
			}
			//1,�����շ�ԱΪ������
			//2���ڻ�����10��������ʱ����Ϊ����
			//3�����·����ڸڵ��շ�Ա
			//1��2�ݲ�����ֻд���ڸڵ��շ�Ա
			int a = dataBaseService.update("update user_info_tb set online_flag=? where auth_flag in(?,?) and online_flag=? ", new Object[]{22,1,2,23});
			System.err.println(">>>>>>������"+a+"��������Ա");
			if(onlineList.size()>0){
				System.out.println(">>>>>>��ǰ�ڸ�������"+onlineList.size());
				String preParms = "";
				List<Object> values = new ArrayList<Object>();
				for(Long u : onlineList){
					if(preParms.equals(""))
						preParms = "?";
					else
						preParms +=",?";
					values.add(u);
				}
				values.add(0,1);
				values.add(0,2);
				values.add(0,23);
				int b = dataBaseService.updateParamList("update user_info_tb set online_flag=? where auth_flag in(?,?) and id in("+preParms+") ", values);
				log.error(">>>>>>������"+b+"���ڸ���Ա");
			}
			
			//ͳ�ƿ�����֧���ĳ���
			//1��ѯ���ߵ��շ�Ա�ĳ������
			List onlineUserComList = dataBaseService.getAll("select distinct comid from user_info_tb where online_flag=? ", new Object[]{23});
			log.error(">>>>���³���");
			if(onlineUserComList!=null&&onlineUserComList.size()>0){
				List<Object> params = new ArrayList<Object>();
				String prePra = "";
				for(int i=0;i<onlineUserComList.size();i++){
					Map map = (Map)onlineUserComList.get(i);
					params.add(Long.valueOf(map.get("comid")+""));
					if(i!=0)
						prePra+=",";
					prePra +="?";
				}
				//	System.out.println(params);
				//System.err.println(prePra);
				if(params!=null){
					params.add(0,1);
					//2�������г�������֧��
					int i = dataBaseService.update("update com_info_tb set is_hasparker =?", new Object[]{0});
					//3���·������շ�Ա�ڸڵĳ���
					int r = dataBaseService.updateParamList("update com_info_tb set is_hasparker =? where id in("+prePra+")", params);
					log.error(">>>>>ͣ�����ڸ����� >>>������"+i+"��������������"+r+"������");
				}
			}
			
			//�����շ������������������1Сʱ���ߵļ�1����
			scroe(onlineList);
		} catch (Exception e) {
			log.error("anlysisParkerOnline",e);
		}
	}
	
	/*
	 * ��ʼͳ��
	 */
	//rewrite
	private void directPayStart(Long nextTime){
		try {
			int directcount = dataBaseService.update("delete from directpay_anlysis_tb where create_time=?",
					new Object[]{nextTime - 24*60*60});
			log.error("ɾ��ֱ������==="+directcount+"��");
//		System.err.println("��ʼֱ��ͳ�ƣ�"+TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis()));
//		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
//		list =dataBaseService.getAll("select * from user_account_tb where target=? and uid>? and create_time between ? and ? order by create_time",
//				new Object[]{1,0,nextTime-(24*60*60),nextTime-1});
//		Map<String, Integer> directPayMap = new HashMap<String, Integer>();
//		nextTime = nextTime-24*60*60;
//		for(Map<String, Object> map : list){
//			String key = nextTime+"";
//			if(directPayMap.containsKey(key)){
//				Integer count = directPayMap.get(key);
//				directPayMap.put(key, count + 1);
//			}else{
//				directPayMap.put(key, 1);
//			}
//		}
			Long count = dataBaseService.getLong("select count(id) from order_tb where " +
					" create_time between ? and ? and c_type=? and pay_type=?", 
					new Object[]{nextTime-(24*60*60),nextTime-1,4,2}) ;
			if(count==null)
				count=0L;
			int ret = dataBaseService.update("insert into directpay_anlysis_tb(create_time,total) values (?,?)", new Object[]{nextTime-24*60*60,count});
			log.error(">>>>>ֱ��ͳ�ƣ�ʱ�䣺"+TimeTools.getTime_yyyyMMdd_HHmmss((nextTime-24*60*60)*1000)+",������"+count+",д����:"+ret);
//		List<Object[]> values = new ArrayList<Object[]>();
//		for(String key: directPayMap.keySet()){	
//			Object[] va = new Object[2];
//			va[0]=Long.valueOf(key);
//			Integer total = directPayMap.get(key);
//			va[1]=total;
//			values.add(va);
//		}
//		log.error("ֱ��ͳ�ƿ�ʼд��...��"+values.size()+"��");
//		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"ֱ��ͳ�ƿ�ʼд��...����"+values.size()+"��");
//		dataBaseService.bathInsert(sql, values, new int []{4,4});
//		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"ֱ��ͳ��д�����...");
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	/*
	 * ��ʼ������
	 */
	private void directPayInit(){
		System.err.println("��ʼ����ֱ��ͳ�ƣ�"+TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis()));
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		list =dataBaseService.getAll("select * from user_account_tb where target=? and uid>? order by create_time",
				new Object[]{1,0});
		Map<String, Integer> directPayMap = new HashMap<String, Integer>();
		for(Map<String, Object> map : list){
			Long ctime = (Long)map.get("create_time");
			ctime = TimeTools.getBeginTime(ctime*1000);
			String key = ctime+"";
			if(directPayMap.containsKey(key)){
				Integer count = directPayMap.get(key);
				directPayMap.put(key, count + 1);
			}else{
				directPayMap.put(key, 1);
			}
		}
		String sql = "insert into directpay_anlysis_tb(create_time,total) values (?,?)";
		List<Object[]> values = new ArrayList<Object[]>();
		for(String key: directPayMap.keySet()){
			Object[] va = new Object[2];
			va[0]=Long.valueOf(key);
			Integer total = directPayMap.get(key);
			va[1]=total;
			values.add(va);
		}
		log.error("��ʼ��ֱ��ͳ��......");
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"ֱ��ͳ�ƿ�ʼд��...����"+values.size()+"��");
		dataBaseService.bathInsert(sql, values, new int []{4,4});
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"ֱ��ͳ��д�����...");
	}
	
	/*
	 * ��ʼ����
	 */
	private void order3Start(Long nextTime){
		try {
			System.err.println("��ʼ�����ն���ͳ�ƣ�"+TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis()));
			List<Map<String, Object>> list1 = new ArrayList<Map<String,Object>>();
			List<Map<String, Object>> list2 = new ArrayList<Map<String,Object>>();
			String sql1 = "select imei,count(1) order_3 from order_tb where imei!='' and create_time between ? and ? group by imei";
			list1 = dataBaseService.getAll(sql1, new Object[]{nextTime-3*(24*60*60),nextTime-1});
			String sql2 = "select imei,sum(total) money_3 from order_tb where imei!='' and end_time between ? and ? and pay_type=? and total>? group by imei";
			list2 = dataBaseService.getAll(sql2, new Object[]{nextTime-3*(24*60*60),nextTime-1,2,0});
			List<Map<String , Object>> sqlMaps = new ArrayList<Map<String,Object>>();
			boolean b = false;
			for(Map<String, Object> map : list1){
				String imei = (String)map.get("imei");
				for(Map<String, Object> map2 : list2){
					String imei2 = (String)map2.get("imei");
					if(imei.equals(imei2)){
						map.put("money_3", map2.get("money_3"));
						break;
					}
				}
				Long order_3 = (Long)map.get("order_3");
				Map<String, Object> sqlMap = new HashMap<String, Object>();
				sqlMap.put("sql", "update mobile_tb set order_3=?,money_3=? where imei=?");
				sqlMap.put("values", new Object[]{map.get("order_3"),map.get("money_3"),map.get("imei")});
				sqlMaps.add(sqlMap);
			}
			b= dataBaseService.bathUpdate_order_3(sqlMaps);
			log.error("�����ն���ͳ�����...");
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	private void anlyCharge(Long nextTime){
		try {
			log.error("=========��ʼͳ���շ����==========");
			//������ͳ���ֽ��շ�
			List<Map<String, Object>> list1 = new ArrayList<Map<String,Object>>();
			String sql1 = "select sum(b.amount) csum,a.comid from order_tb a,parkuser_cash_tb b where a.end_time between" +
					" ? and ? and a.state=? and a.id=b.orderid and b.type=? and a.uid> ? group by a.comid ";
			list1 = dataBaseService.getAll(sql1, new Object[]{nextTime-(24*60*60), nextTime, 1, 0, 0});
			//�����շ�--�����˻�
			List<Map<String, Object>> list2 = new ArrayList<Map<String,Object>>();
			String sql2 = "select sum(a.amount) psum,o.comid from order_tb o,park_account_tb a where o.id=a.orderid and o.end_time between ? and ? " +
					" and a.type= ? and a.source=? and o.uid>? group by o.comid ";
			list2 = dataBaseService.getAll(sql2, new Object[]{nextTime-(24*60*60), nextTime, 0, 0, 0});
			//�����շ�--�շ�Ա�˻�
			List<Map<String, Object>> list3 = new ArrayList<Map<String,Object>>();
			String sql3 = "select sum(a.amount) psum,o.comid from order_tb o,parkuser_account_tb a where o.id=a.orderid and o.end_time between ? and ? " +
					" and a.type=? and a.target=? and a.remark like ? and o.uid>? group by o.comid ";
			list3 = dataBaseService.getAll(sql3, new Object[]{nextTime-(24*60*60), nextTime, 0, 4, "ͣ����%", 0});
			//�����շ�--��Ӫ�����˻�
			List<Map<String, Object>> list4 = new ArrayList<Map<String,Object>>();
			String sql4 = "select sum(g.amount) gsum,o.comid from order_tb o, group_account_tb g where o.id=g.orderid and o.end_time between ? and ? " +
					" and g.type=? and g.source=? and o.uid>? group by o.comid ";
			list4 = dataBaseService.getAll(sql4, new Object[]{nextTime-(24*60*60), nextTime, 0, 0, 0});
			//�����շ�--�����̻��˻�
			List<Map<String, Object>> list5 = new ArrayList<Map<String,Object>>();
			String sql5 = "select sum(c.amount) csum,o.comid from order_tb o, city_account_tb c where o.id=c.orderid and o.end_time between ? and ? " +
					" and c.type=? and c.source=? and o.uid>? group by o.comid ";
			list5 = dataBaseService.getAll(sql5, new Object[]{nextTime-(24*60*60), nextTime, 0, 0, 0});
			
			Map<String, Map<String, Object>> cMap = new HashMap<String, Map<String, Object>>();
			nextTime = nextTime-24*60*60;
			if(list1 != null && !list1.isEmpty()){
				for(Map<String, Object> map : list1){
					Map<String, Object> map2 = new HashMap<String, Object>();
					String key = map.get("comid") + "";
					if(cMap.containsKey(key)){
						Map<String, Object> map3 = cMap.get(key);
						map3.put("cash", map.get("csum"));
					}else{
						map2.put("cash", map.get("csum"));
						cMap.put(key, map2);
					}
				}
			}
			
			if(list2 != null && !list2.isEmpty()){
				for(Map<String, Object> map : list2){
					Map<String, Object> map2 = new HashMap<String, Object>();
					String key = map.get("comid") + "";
					if(cMap.containsKey(key)){
						Map<String, Object> map3 = cMap.get(key);
						map3.put("epay_park", map.get("psum"));
					}else{
						map2.put("epay_park", map.get("psum"));
						cMap.put(key, map2);
					}
				}
			}
			
			if(list3 != null && !list3.isEmpty()){
				for(Map<String, Object> map : list3){
					Map<String, Object> map2 = new HashMap<String, Object>();
					String key = map.get("comid") + "";
					if(cMap.containsKey(key)){
						Map<String, Object> map3 = cMap.get(key);
						map3.put("epay_collector", map.get("psum"));
					}else{
						map2.put("epay_collector", map.get("psum"));
						cMap.put(key, map2);
					}
				}
			}
			
			if(list4 != null && !list4.isEmpty()){
				for(Map<String, Object> map : list4){
					Map<String, Object> map2 = new HashMap<String, Object>();
					String key = map.get("comid") + "";
					if(cMap.containsKey(key)){
						Map<String, Object> map3 = cMap.get(key);
						map3.put("epay_group", map.get("gsum"));
					}else{
						map2.put("epay_group", map.get("gsum"));
						cMap.put(key, map2);
					}
				}
			}
			
			if(list5 != null && !list5.isEmpty()){
				for(Map<String, Object> map : list5){
					Map<String, Object> map2 = new HashMap<String, Object>();
					String key = map.get("comid") + "";
					if(cMap.containsKey(key)){
						Map<String, Object> map3 = cMap.get(key);
						map3.put("epay_city", map.get("csum"));
					}else{
						map2.put("epay_city", map.get("csum"));
						cMap.put(key, map2);
					}
				}
			}
			String sql = "insert into collect_anlysis_tb(create_time,comid,cash,epay_collector,epay_park,epay_group,epay_city) values (?,?,?,?,?,?,?)";
			List<Object[]> values = new ArrayList<Object[]>();
			for(String key: cMap.keySet()){
				Object[] va = new Object[7];
				va[0] = nextTime;
				va[1] = Long.valueOf(key);
				Map<String, Object> map = cMap.get(key);
				Double cash = 0d;
				Double epay_collector = 0d;
				Double epay_park = 0d;
				Double epay_group = 0d;
				Double epay_city = 0d;
				if(map.get("cash") != null){
					cash = Double.valueOf(map.get("cash") + "");
				}
				if(map.get("epay_collector") != null){
					epay_collector = Double.valueOf(map.get("epay_collector") + "");
				}
				if(map.get("epay_park") != null){
					epay_park = Double.valueOf(map.get("epay_park") + "");
				}
				if(map.get("epay_group") != null){
					epay_group = Double.valueOf(map.get("epay_group") + "");
				}
				if(map.get("epay_city") != null){
					epay_city = Double.valueOf(map.get("epay_city") + "");
				}
				va[2] = cash;
				va[3] = epay_collector;
				va[4] = epay_park;
				va[5] = epay_group;
				va[6] = epay_city;
				values.add(va);
			}
			
			int r = dataBaseService.bathInsert(sql, values, new int []{4,4,3,3,3,3,3});
			log.error("r:"+r);
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	/*
	 * ��ʼͳ��
	 */
	private void registerStart(Long nextTime){
		try {
			int regcount = dataBaseService.update("delete from register_anlysis_tb where reg_time=?",
					new Object[]{nextTime - 24*60*60});
			log.error("ɾ��ע������==="+regcount+"��");
			System.err.println("��ʼ����ע��ͳ�ƣ�"+TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis()));
			List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
			list =dataBaseService.getAll("select u.*,c.car_number from user_info_tb u left join car_info_tb c on u.id=c.uin where u.auth_flag=? and u.reg_time between ? and ? order by u.reg_time",
					new Object[]{4,nextTime-(24*60*60),nextTime-1});
			Map<String, Map<String, Object>> regMap = new HashMap<String, Map<String, Object>>();
			nextTime = nextTime-24*60*60;
			for(Map<String, Object> map : list){
				Map<String, Object> map2 = new HashMap<String, Object>();
				String key = nextTime+"";
				if(regMap.containsKey(key)){
					Map<String, Object> map3 = regMap.get(key);
					Integer hascarnumber = (Integer)map3.get("hascarnumber");
					Integer allcarowner = (Integer)map3.get("allcarowner");
					if(map.get("car_number") != null){
						map3.put("hascarnumber", hascarnumber + 1);
					}
					map3.put("allcarowner", allcarowner + 1);
					regMap.put(key, map3);
				}else{
					if(map.get("car_number") != null){
						map2.put("hascarnumber", 1);
					}else{
						map2.put("hascarnumber", 0);
					}
					map2.put("allcarowner", 1);
					regMap.put(key, map2);
				}
			}
			String sql = "insert into register_anlysis_tb(reg_time,reg_count,carnumber_count) values (?,?,?)";
			List<Object[]> values = new ArrayList<Object[]>();
			for(String key: regMap.keySet()){
				Object[] va = new Object[3];
				va[0]=Long.valueOf(key);
				Map<String, Object> map = regMap.get(key);
				va[1] = map.get("allcarowner");
				va[2] = map.get("hascarnumber");
				values.add(va);
			}
			log.error("����ע��ͳ�ƿ�ʼд��...��"+values.size()+"��");
			System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"����ע��ͳ�ƿ�ʼд��...����"+values.size()+"��");
			dataBaseService.bathInsert(sql, values, new int []{4,4,4});
			System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"����ע��ͳ��д�����...");
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	/*
	 * ��ʼ������
	 */
	private void registerInit(){
		System.err.println("��ʼ���㳵��ע�᣺"+TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis()));
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		list =dataBaseService.getAll("select u.*,c.car_number from user_info_tb u left join car_info_tb c on u.id=c.uin where u.auth_flag=? order by reg_time",
				new Object[]{4});
		Map<String, Map<String, Object>> registerMap = new HashMap<String, Map<String, Object>>();
		for(Map<String, Object> map : list){
			Map<String, Object> map2 = new HashMap<String, Object>();
			Long rtime = (Long)map.get("reg_time");
			rtime = TimeTools.getBeginTime(rtime*1000);
			String key = rtime+"";
			if(registerMap.containsKey(key)){
				Map<String, Object> map3 = registerMap.get(key);
				Integer hascarnumber = (Integer)map3.get("hascarnumber");
				Integer allcarowner = (Integer)map3.get("allcarowner");
				if(map.get("car_number") != null){
					map3.put("hascarnumber", hascarnumber + 1);
				}
				map3.put("allcarowner", allcarowner + 1);
				registerMap.put(key, map3);
			}else{
				if(map.get("car_number") != null){
					map2.put("hascarnumber", 1);
				}else{
					map2.put("hascarnumber", 0);
				}
				map2.put("allcarowner", 1);
				registerMap.put(key, map2);
			}
		}
		String sql = "insert into register_anlysis_tb(reg_time,reg_count,carnumber_count) values (?,?,?)";
		List<Object[]> values = new ArrayList<Object[]>();
		for(String key: registerMap.keySet()){
			Object[] va = new Object[3];
			va[0]=Long.valueOf(key);
			Map<String, Object> map = registerMap.get(key);
			va[1] = map.get("allcarowner");
			va[2] = map.get("hascarnumber");
			values.add(va);
		}
		log.error("��ʼ������ע��ͳ��......");
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"����ע��ͳ�ƿ�ʼд��...����"+values.size()+"��");
		dataBaseService.bathInsert(sql, values, new int []{4,4,4});
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"����ע��ͳ��д�����...");
	}
	
	/*
	 * ��ʼͳ��
	 */
	private void mobilePayStart(Long nextTime){
		try {
			int mobilecount = dataBaseService.update("delete from mobilepay_anlysis_tb where create_time=?",
					new Object[]{nextTime - 24*60*60});
			log.error("ɾ���ֻ�֧������==="+mobilecount+"��");
			System.err.println("��ʼ�����ֻ�֧��ͳ�ƣ�"+TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis()));
			List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
			list =dataBaseService.getAll("select * from order_tb where state=? and pay_type=? and end_time between ? and ? and c_type !=? and total>=? order by end_time",
					new Object[]{1,2,nextTime-(24*60*60),nextTime-1,4,1});
			Map<String, Integer> mobilepayMap = new HashMap<String, Integer>();
			nextTime = nextTime-24*60*60;
			for(Map<String, Object> map : list){
				Long comId = (Long)map.get("comid");
				String key = comId+"_"+nextTime;
				if(mobilepayMap.containsKey(key)){
					Integer count = mobilepayMap.get(key);
					mobilepayMap.put(key, count + 1);
				}else{
					mobilepayMap.put(key, 1);
				}
			}
			String sql = "insert into mobilepay_anlysis_tb(comid,create_time,mobilepay_count) values (?,?,?)";
			List<Object[]> values = new ArrayList<Object[]>();
			for(String key: mobilepayMap.keySet()){
				Object[] va = new Object[3];
				va[0]=Long.valueOf(key.split("_")[0]);
				va[1]=Long.valueOf(key.split("_")[1]);
				Integer mobilepay_count = mobilepayMap.get(key);
				va[2]=mobilepay_count;
				values.add(va);
			}
			log.error("�ֻ�֧��ͳ�ƿ�ʼд��...��"+values.size()+"��");
			System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"��ʼд��...����"+values.size()+"��");
			dataBaseService.bathInsert(sql, values, new int []{4,4,4});
			System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"д�����...");
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	/*
	 * ��ʼ������
	 */
	private void mobilePayInit(){
		System.err.println("��ʼ�����ֻ�֧����"+TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis()));
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		list =dataBaseService.getAll("select * from order_tb where state=? and pay_type=? and total>=? order by end_time",
				new Object[]{1,2,1});
		Map<String, Integer> mobilepayMap = new HashMap<String, Integer>();
		for(Map<String, Object> map : list){
			Long comId = (Long)map.get("comid");
			Long etime = (Long)map.get("end_time");
			etime = TimeTools.getBeginTime(etime*1000);
			String key = comId+"_"+etime;
			if(mobilepayMap.containsKey(key)){
				Integer count = mobilepayMap.get(key);
				mobilepayMap.put(key, count + 1);
			}else{
				mobilepayMap.put(key, 1);
			}
		}
		String sql = "insert into mobilepay_anlysis_tb(comid,create_time,mobilepay_count) values (?,?,?)";
		List<Object[]> values = new ArrayList<Object[]>();
		for(String key: mobilepayMap.keySet()){
			Object[] va = new Object[3];
			va[0]=Long.valueOf(key.split("_")[0]);
			va[1]=Long.valueOf(key.split("_")[1]);
			Integer mobilepay_count = mobilepayMap.get(key);
			va[2]=mobilepay_count;
			values.add(va);
		}
		log.error("��ʼ���ֻ�֧��ͳ��......");
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"��ʼд��...����"+values.size()+"��");
		dataBaseService.bathInsert(sql, values, new int []{4,4,4});
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"д�����...");
	}
	
	/**
	 * ��ʼͳ��
	 */
	private void start(Long nextTime){
		System.err.println("��ʼ���㣺"+TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis()));
		List<Map<String, Object>> lalaList =dataBaseService.getAll("select * from share_log_tb where create_time between ? and ?  order by create_time",
				new Object[]{nextTime-899,nextTime});
		List<Map<String, Object>> orderList =dataBaseService.getAll("select * from order_tb  where state=? order by create_time",
				new Object[]{0});
		
		Map<String, List<Integer>> comLalaMap = new HashMap<String,  List<Integer>>();
		for(Map<String, Object> map : lalaList){
			Long comId = (Long)map.get("comid");
			Integer number = (Integer)map.get("s_number");
			String key = comId+"_"+nextTime;
			if(comLalaMap.containsKey(key)){
				List<Integer> nList = comLalaMap.get(key);
				nList.add(number);
			}else {
				List<Integer> nList = new ArrayList<Integer>();
				nList.add(number);
				comLalaMap.put(key, nList);
			}
		}
		for(String key : comLalaMap.keySet()){
			List<Integer> lnum = comLalaMap.get(key);
			if(lnum.size()>1){
				Integer total =0;
				for(Integer num : lnum){
					total +=num;
				}
				total = total/lnum.size();
				lnum.clear();
				lnum.add(total);
			}
		}
		
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"��������lala");
		for(Map<String, Object> map : orderList){
			Long comId = (Long)map.get("comid");
			String key = comId+"_"+nextTime;
			if(comLalaMap.containsKey(key)){//�����ʱ���ķ�������
				List<Integer> nlInteger = comLalaMap.get(key);
				if(nlInteger.size()==2){//�Ѿ������ʱ�����ռ�ò�λʱ��ԭ��ֵ��1
					Integer c = nlInteger.get(1);
					c=c+1;
					nlInteger.remove(1);
					nlInteger.add(c);
				}else {//û�������ʱ�����ռ�ò�λʱ����1����λռ��
					nlInteger.add(1);
				}
			}else {//û�����ʱ���ķ�������
				List<Integer> _nlIntegers=new ArrayList<Integer>();
				_nlIntegers.add(0);
				_nlIntegers.add(1);
				comLalaMap.put(key, _nlIntegers);
			}
		}
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"�������˶���");
		//System.out.println("һ������ݣ�"+comLalaMap+","+comLalaMap.size());
		
		/*
		 * ��ʼд��
		 *  create_time bigint,
			  comid bigint,
			  share_count integer DEFAULT 0,
			  free_count integer DEFAULT 0,
		 */
		String sql = "insert into park_anlysis_tb (create_time,comid,share_count,used_count) values (?,?,?,?)";
		List<Object[]> values = new ArrayList<Object[]>();
		System.out.println(comLalaMap.size());
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"�ϲ�lala��ռ����");
		for(String key: comLalaMap.keySet()){
			//System.out.println(key);
			
			Object[] va = new Object[4];
			va[0]=Long.valueOf(key.split("_")[1]);
			va[1]=Long.valueOf(key.split("_")[0]);
			List<Integer> nList = comLalaMap.get(key);
			va[2]=nList.get(0);
			if(nList.size()>1){
				va[3]=nList.get(1);
			}else {
				va[3]=0;
			}
			values.add(va);
		}
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"��ʼд��...����"+values.size()+"��");
		dataBaseService.bathInsert(sql, values, new int []{4,4,4,4});
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"д�����...");
		
	}
	/**
	 * ��ʼ������
	 */
	private void init(){
		System.err.println("��ʼ���㣺"+TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis()));
		//Long start = 1405699200L;//2014-7-19 15:54:21  1405699200
		List<Map<String, Object>> lalaList =dataBaseService.getAll("select * from share_log_tb order by create_time",
				new Object[]{});
		List<Map<String, Object>> orderList =dataBaseService.getAll("select * from order_tb  order by create_time",
				new Object[]{});
		/*
		 *   comid bigint,
			  s_number integer DEFAULT 0,
			  create_time bigint,
		 * д��park_anlysis_tb��
		 *  create_time bigint,
			  comid bigint,
			  share_count integer DEFAULT 0,
			  free_count integer DEFAULT 0,
		 * 
		 */
		Map<String, List<Integer>> comLalaMap = new HashMap<String,  List<Integer>>();
		/*
		 * ��15����Ϊ��λͳ�Ƴ�����lala������
		 * 726_1405772100=[118, 114, 123],
			 702_1405764000=[85], 
			 728_1405769400=[90], 
			 773_1405773900=[800], 
			 643_1405759500=[127],
			 702_1405779300=[78], 
			 726_1405776600=[126, 126, 125],
		 */
		for(Map<String, Object> map : lalaList){
			Long ctime = (Long)map.get("create_time");
			Long comId = (Long)map.get("comid");
			Integer number = (Integer)map.get("s_number");
			ctime = ctime -ctime%(15*60)+15*60;
			String key = comId+"_"+ctime;
			if(comLalaMap.containsKey(key)){
				List<Integer> nList = comLalaMap.get(key);
				nList.add(number);
			}else {
				List<Integer> nList = new ArrayList<Integer>();
				nList.add(number);
				comLalaMap.put(key, nList);
			}
		}
		
		//System.out.println("һ������ݣ�"+comLalaMap+","+comLalaMap.size());
		/*
		 * ȫ��lala������ȡƽ��ֵ
		 *   726_1405772100=[(118+114+123)/3],
			 702_1405764000=[85], 
			 728_1405769400=[90], 
			 773_1405773900=[800], 
			 643_1405759500=[127],
			 702_1405779300=[78], 
			 726_1405776600=[(126+126+125)/3]
		 */
		for(String key : comLalaMap.keySet()){
			List<Integer> lnum = comLalaMap.get(key);
			if(lnum.size()>1){
				Integer total =0;
				for(Integer num : lnum){
					total +=num;
				}
				total = total/lnum.size();
				lnum.clear();
				lnum.add(total);
			}
			/*Long kt = Long.valueOf(key.split("_")[1]);
			String nextKey =key.split("_")[0]+"_"+ (kt+15*60);
			if(!comLalaMap.containsKey(nextKey))
				comLalaMap.put(nextKey, lnum);*/
		}
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"��������lala");
		//System.out.println("һ������ݣ�"+comLalaMap+","+comLalaMap.size());
		/*
		 * ͳ��ռ�ò�λ����
		 * ������ʼʱ��:2014-09-13 05:05:00
			��������ʱ��:2014-09-13 07:13:04
			����ʱ�����Ӧ����ռ����һ����λ��
			2014-09-13 05:15:00
			2014-09-13 05:30:00
			2014-09-13 05:45:00
			2014-09-13 06:00:00
			2014-09-13 06:15:00
			2014-09-13 06:30:00
			2014-09-13 06:45:00
			2014-09-13 07:00:00
		 *
		 * ѭ�����뵽ÿ��ʱ�����,�Ѿ������ʱ�����ռ�ò�λʱ��ԭ��ֵ��1
		 */
		//Map<String, Integer> comorderMap = new HashMap<String,  Integer>();
		for(Map<String, Object> map : orderList){
			Long etime = (Long)map.get("end_time");
			if(etime==null)
				continue;
			Long ctime = (Long)map.get("create_time");
			Long comId = (Long)map.get("comid");
			Long [] bt = getTimeInOff(ctime,etime);
			for(Long b: bt){//ѭ�����뵽ÿ��ʱ�����,�Ѿ������ʱ�����ռ�ò�λʱ��ԭ��ֵ��1
				String key = comId+"_"+b;
				if(comLalaMap.containsKey(key)){//�����ʱ���ķ�������
					List<Integer> nlInteger = comLalaMap.get(key);
					if(nlInteger.size()==2){//�Ѿ������ʱ�����ռ�ò�λʱ��ԭ��ֵ��1
						Integer c = nlInteger.get(1);
						c=c+1;
						nlInteger.remove(1);
						nlInteger.add(c);
					}else {//û�������ʱ�����ռ�ò�λʱ����1����λռ��
						nlInteger.add(1);
					}
				}else {//û�����ʱ���ķ�������
					List<Integer> _nlIntegers=new ArrayList<Integer>();
					_nlIntegers.add(0);
					_nlIntegers.add(1);
					comLalaMap.put(key, _nlIntegers);
					/*Long lastKey = null;
					Long lastTime = null;
					for(String _key : comLalaMap.keySet()){
						Long cid = Long.valueOf(_key.split("_")[0]);
						if(cid.intValue()==comId.intValue()){
							Long kt = Long.valueOf(_key.split("_")[1]);
							Long lt = b-kt;
							if(lastKey==null){
								lastKey = lt;
								lastTime = kt;
							}else {
								if(lt<lastKey){
									lastKey = lt;
									lastTime = kt;
								}
							}
						}
					}
					if(lastTime==null){
						
					}else {
						List<Integer> nlInteger = comLalaMap.get(comId+"_"+lastTime);
						List<Integer> _nlIntegers=new ArrayList<Integer>();
						_nlIntegers.add(nlInteger.get(0));
						_nlIntegers.add(1);
						comLalaMap.put(key, _nlIntegers);
					}*/
				}
			}
		}
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"�������˶���");
		//System.out.println("һ������ݣ�"+comLalaMap+","+comLalaMap.size());
		
		/*
		 * ��ʼд��
		 *  create_time bigint,
			  comid bigint,
			  share_count integer DEFAULT 0,
			  free_count integer DEFAULT 0,
		 */
		String sql = "insert into park_anlysis_tb (create_time,comid,share_count,used_count) values (?,?,?,?)";
		List<Object[]> values = new ArrayList<Object[]>();
		System.out.println(comLalaMap.size());
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"�ϲ�lala��ռ����");
		for(String key: comLalaMap.keySet()){
			//System.out.println(key);
			
			Object[] va = new Object[4];
			va[0]=Long.valueOf(key.split("_")[1]);
			va[1]=Long.valueOf(key.split("_")[0]);
			List<Integer> nList = comLalaMap.get(key);
			va[2]=nList.get(0);
			if(nList.size()>1){
				va[3]=nList.get(1);
			}else {
				va[3]=0;
			}
			values.add(va);
		}
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"��ʼд��...����"+values.size()+"��");
		dataBaseService.bathInsert(sql, values, new int []{4,4,4,4});
		System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(System.currentTimeMillis())+"д�����...");
	}
	
	/**
	 * ȡ����ʱ�������ڵĿ�ȡֵ
	 * @param start ��ʼʱ��
	 * @param end ����ʱ��
	 * start = 1410555900 =2014-09-13 05:05:00
	 * end = 1410567784 = 2014-09-13 08:23:04
	 * @return [];
	 */
	
	private  Long [] getTimeInOff(Long start,Long end){
		start = start-start%(15*60)  +15*60;
		end   = end -end%(15*60);
		Long s = end-start;
		int t = s.intValue()/(15*60);
		Long [] times = new Long[t+1];
		for(int i=0;i<=t;i++){
			times[i]=start+i*15*60;
		}
		return times;
	}
	/**
	 * ʮ����ͳ��һ�����߻��� 
	 * @param uin
	 */
	private void scroe(List<Long> uinList){
		log.error("ͳ�ƻ���");
		Long btime = TimeTools.getToDayBeginTime();
		List<Map<String, Object>> allUserList = dataBaseService.getAll("select uin from collector_scroe_tb where create_time=?", new Object[]{btime});
		List<Map<String, Object>> allepayUserList = dataBaseService.getAll("select id from user_info_tb where comid in(select id from com_info_Tb where epay=?)", new Object[]{1});
		//��Ҫ���µ��շ�Ա
		List<Long> updateUinList = new ArrayList<Long>();
		if(allUserList!=null&&!allUserList.isEmpty()){
			for(Map<String, Object> map : allUserList){
				updateUinList.add((Long)map.get("uin"));
			}
		}
		//���п�֧���������շ�Ա�ſ��Ի���
		List<Long> allepayUser = new ArrayList<Long>();
		if(allepayUserList!=null&&!allepayUserList.isEmpty()){
			for(Map<String, Object> map : allepayUserList){
				allepayUser.add((Long)map.get("id"));
			}
		}
		//log.error("ͳ�ƻ���,�շ�Ա��"+updateUinList);
		//���µ�SQL��� 
		String updateSql ="update collector_scroe_tb  set online_scroe=online_scroe+? where uin=? and create_time=? ";
		//�½���SQL���
		String inserSql = "insert into collector_scroe_tb (uin,lala_scroe,nfc_score,praise_scroe,create_time,pai_score,online_scroe) values (?,?,?,?,?,?,?)";
		//���µ�ֵ		
		List<Object[]> updateValues= new ArrayList<Object[]>();
		//�½���ֵ	
		List<Object[]> insertValues= new ArrayList<Object[]>();
		//�ж����Ƿ��и��µļ����У��ڵ��� ���£����ڵ��� �½�
		for(Long uin:uinList){
			if(!allepayUser.contains(uin)){
				log.error("--->>����֧������û�л��� ��"+uin);
				continue;
			}
			if(updateUinList.contains(uin)){
				updateValues.add(new Object[]{0.05,uin,btime});
			}else {
				insertValues.add(new Object[]{uin,0,0.0,0,btime,0,0.05});
			}
		}
		//����
		if(updateValues.size()>0){
			int ut = dataBaseService.bathInsert(updateSql,updateValues,new int[]{3,4,4});
			log.error("������"+ut+"������");
		}
		//�½�
		if(insertValues.size()>0){
			int it =dataBaseService.bathInsert(inserSql,insertValues,new int[]{4,4,3,4,4,4,3});
			log.error("�²�����"+it+"������");
		}
	}
	
	public static void main(String[] args) {
		
	/*	Map<Long , Long> userMap = new HashMap<Long, Long>();
		userMap.put(1L, 1419928500L);
		userMap.put(2L, 1419928500L-15*60);
		userMap.put(3L, 1419928500L-9*60);
		userMap.put(4L, 1419928500L-16*60);
		//���˵�����ʱ�䳬��10���ӵ��շ�Ա
		Long ntime = System.currentTimeMillis()/1000-15*60;
		Map<Long , Long> userMap1 = new HashMap<Long, Long>();
		for(Long key : userMap.keySet()){
			if(userMap.get(key)>ntime)
				userMap1.put(key,userMap.get(key));
		}
				
		System.err.println(userMap1);*/
//		Long eLong = 1410563584l;
//		Long [] r = (getTimeInOff(1410555900L,eLong));
//		System.out.println("b:"+TimeTools.getTime_yyyyMMdd_HHmmss(1410555900000L));
//		for(Long s : r){
//			System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(s*1000));
//		}
//		System.out.println("e:"+TimeTools.getTime_yyyyMMdd_HHmmss(eLong*1000));
	}
}
