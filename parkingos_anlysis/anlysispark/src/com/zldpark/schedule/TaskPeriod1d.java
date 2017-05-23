package com.zldpark.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.zldpark.facade.StatsAccountFacade;
import com.zldpark.impl.CommonMethods;
import com.zldpark.pojo.Berth;
import com.zldpark.pojo.StatsAccountClass;
import com.zldpark.pojo.StatsCard;
import com.zldpark.pojo.StatsCardResp;
import com.zldpark.pojo.StatsFacadeResp;
import com.zldpark.pojo.StatsReq;
import com.zldpark.service.DataBaseService;
import com.zldpark.service.PgOnlyReadService;
import com.zldpark.service.StatsCardService;
import com.zldpark.utils.ExecutorsUtil;
import com.zldpark.utils.MemcacheUtils;
import com.zldpark.utils.StringUtils;
import com.zldpark.utils.TimeTools;

public class TaskPeriod1d implements Runnable {
	private DataBaseService dataBaseService;
	private PgOnlyReadService pgOnlyReadService;
	private MemcacheUtils memcacheUtils;
	private CommonMethods commonMethods;
	private StatsAccountFacade accountFacade;
	private StatsCardService cardService;
	
	public TaskPeriod1d(DataBaseService dataBaseService, PgOnlyReadService pgOnlyReadService,
			MemcacheUtils memcacheUtils, CommonMethods commonMethods, StatsAccountFacade accountFacade, 
			StatsCardService cardService){
		this.dataBaseService = dataBaseService;
		this.pgOnlyReadService = pgOnlyReadService;
		this.memcacheUtils = memcacheUtils;
		this.commonMethods = commonMethods;
		this.accountFacade = accountFacade;
		this.cardService = cardService;
	}

	private static Logger log = Logger.getLogger(TaskPeriod1d.class);
	
	@Override
	public void run() {
		log.error("********************��ʼ1��һ�εĶ�ʱ����***********************");
		try {
			//�������ȡ�賿����ʱ�䣬��Ϊͳ��ÿһ������ݶ��Ǵ�ǰһ���0�㵽23:59:59
			Long beginTime = TimeTools.getToDayBeginTime();
			log.error("�����賿���ʱ��:" + beginTime);
			ExecutorService pool = ExecutorsUtil.getExecutorService();
			ExeTask callable0 = new ExeTask(beginTime, 0);
			ExeTask callable1 = new ExeTask(beginTime, 1);
			ExeTask callable2 = new ExeTask(beginTime, 2);
			ExeTask callable3 = new ExeTask(beginTime, 3);
			ExeTask callable4 = new ExeTask(beginTime, 4);
			Future<Object> future0 = pool.submit(callable0);
			Future<Object> future1 = pool.submit(callable1);
			Future<Object> future2 = pool.submit(callable2);
			Future<Object> future3 = pool.submit(callable3);
			Future<Object> future4 = pool.submit(callable4);
			future0.get();
			future1.get();
			future2.get();
			future3.get();
			future4.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.error("********************����1��һ�εĶ�ʱ����***********************");
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
					incomeAnly(curTime);
					break;
				case 1:
					cardAnly(curTime);
					break;
				case 2:
					berthIncomeAnly(curTime);
					break;
				case 3:
					parkIncomeAnly(curTime);
					break;
				case 4:
					groupIncomeAnly(curTime);
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
	
	private void groupIncomeAnly(Long ntime){
		try {
			log.error("��ʼͳ����Ӫ��������");
			List<Map<String, Object>> parks = pgOnlyReadService.getAll("select id from org_group_tb where state=? ",
					new Object[]{0});
			List<Object> idList = new ArrayList<Object>();
			if(parks != null && !parks.isEmpty()){
				for(Map<String, Object> map : parks){
					idList.add(map.get("id"));
				}
				StatsReq req = new StatsReq();
				req.setIdList(idList);
				req.setStartTime(ntime - 24 * 60 * 60);
				req.setEndTime(ntime);
				StatsFacadeResp resp = accountFacade.statsGroupAccount(req);
				if(resp.getResult() == 1){
					List<StatsAccountClass> classes = resp.getClasses();
					String sql = "insert into parkuser_income_anlysis_tb(uin,prepay_cash,add_cash,refund_cash,pursue_cash,pfee_cash,prepay_epay," +
							"add_epay,refund_epay,pursue_epay,pfee_epay,escape,create_time,prepay_card,add_card," +
							"refund_card,pursue_card,pfee_card,type) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
					if(classes != null && !classes.isEmpty()){
						List<Object[]> values = new ArrayList<Object[]>();
						for(StatsAccountClass account : classes){
							Object[] va = new Object[19];
							Long id = account.getId();
							Double prepay_cash = account.getCashPrepayFee();//�ֽ�Ԥ֧��
							Double add_cash = account.getCashAddFee();//�ֽ𲹽�
							Double refund_cash = account.getCashRefundFee();//�ֽ��˿�
							Double pursue_cash = account.getCashPursueFee();//�ֽ�׷��
							Double pfee_cash = account.getCashParkingFee();//�ֽ�ͣ���ѣ���Ԥ����
							Double prepay_epay = account.getePayPrepayFee();//����Ԥ֧��
							Double add_epay = account.getePayAddFee();;//���Ӳ���
							Double refund_epay = account.getePayRefundFee();//�����˿�
							Double pursue_epay = account.getePayPursueFee();//����׷��
							Double pfee_epay = account.getePayParkingFee();//����ͣ���ѣ���Ԥ����
							Double escape = account.getEscapeFee();//�ӵ�δ׷�ɵ�ͣ����
							Double prepay_card = account.getCardPrepayFee();//ˢ��Ԥ֧��
							Double add_card = account.getCardAddFee();//ˢ������
							Double refund_card = account.getCardRefundFee();//ˢ���˿�
							Double pursue_card = account.getCardPursueFee();//ˢ��׷��
							Double pfee_card = account.getCardParkingFee();//ˢ��ͣ���ѣ���Ԥ����
							
							if(prepay_cash == 0
									&& add_cash == 0
									&& refund_cash == 0
									&& pursue_cash == 0
									&& pfee_cash == 0
									&& prepay_epay == 0
									&& add_epay == 0
									&& refund_epay == 0
									&& pursue_epay == 0
									&& pfee_epay == 0
									&& prepay_card == 0
									&& add_card == 0
									&& refund_card == 0
									&& pursue_card == 0
									&& pfee_card == 0
									&& escape ==0){
								continue;
							}
							va[0] = id;
							va[1] = prepay_cash;
							va[2] = add_cash;
							va[3] = refund_cash;
							va[4] = pursue_cash;
							va[5] = pfee_cash;
							va[6] = prepay_epay;
							va[7] = add_epay;
							va[8] = refund_epay;
							va[9] = pursue_epay;
							va[10] = pfee_epay;
							va[11] = escape;
							va[12] = ntime;
							va[13] = prepay_card;
							va[14] = add_card;
							va[15] = refund_card;
							va[16] = pursue_card;
							va[17] = pfee_card;
							va[18] = 3;
							values.add(va);
						}
						int r = dataBaseService.bathInsert(sql, values, new int []{4,3,3,3,3,3,3,3,3,3,3,3,4,3,3,3,3,3,4});
						log.error("r:"+r);
					}
					
				}
			}
			
		} catch (Exception e) {
			
		}
	}
	
	private void parkIncomeAnly(Long ntime){
		try {
			log.error("��ʼͳ�Ƴ�������");
			List<Map<String, Object>> parks = pgOnlyReadService.getAll("select id from com_info_tb where state<>? ",
					new Object[]{1});
			List<Object> idList = new ArrayList<Object>();
			if(parks != null && !parks.isEmpty()){
				for(Map<String, Object> map : parks){
					idList.add(map.get("id"));
				}
				StatsReq req = new StatsReq();
				req.setIdList(idList);
				req.setStartTime(ntime - 24 * 60 * 60);
				req.setEndTime(ntime);
				StatsFacadeResp resp = accountFacade.statsParkAccount(req);
				if(resp.getResult() == 1){
					List<StatsAccountClass> classes = resp.getClasses();
					String sql = "insert into parkuser_income_anlysis_tb(uin,prepay_cash,add_cash,refund_cash,pursue_cash,pfee_cash,prepay_epay," +
							"add_epay,refund_epay,pursue_epay,pfee_epay,escape,create_time,prepay_card,add_card," +
							"refund_card,pursue_card,pfee_card,type) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
					if(classes != null && !classes.isEmpty()){
						List<Object[]> values = new ArrayList<Object[]>();
						for(StatsAccountClass account : classes){
							Object[] va = new Object[19];
							Long id = account.getId();
							Double prepay_cash = account.getCashPrepayFee();//�ֽ�Ԥ֧��
							Double add_cash = account.getCashAddFee();//�ֽ𲹽�
							Double refund_cash = account.getCashRefundFee();//�ֽ��˿�
							Double pursue_cash = account.getCashPursueFee();//�ֽ�׷��
							Double pfee_cash = account.getCashParkingFee();//�ֽ�ͣ���ѣ���Ԥ����
							Double prepay_epay = account.getePayPrepayFee();//����Ԥ֧��
							Double add_epay = account.getePayAddFee();;//���Ӳ���
							Double refund_epay = account.getePayRefundFee();//�����˿�
							Double pursue_epay = account.getePayPursueFee();//����׷��
							Double pfee_epay = account.getePayParkingFee();//����ͣ���ѣ���Ԥ����
							Double escape = account.getEscapeFee();//�ӵ�δ׷�ɵ�ͣ����
							Double prepay_card = account.getCardPrepayFee();//ˢ��Ԥ֧��
							Double add_card = account.getCardAddFee();//ˢ������
							Double refund_card = account.getCardRefundFee();//ˢ���˿�
							Double pursue_card = account.getCardPursueFee();//ˢ��׷��
							Double pfee_card = account.getCardParkingFee();//ˢ��ͣ���ѣ���Ԥ����
							
							if(prepay_cash == 0
									&& add_cash == 0
									&& refund_cash == 0
									&& pursue_cash == 0
									&& pfee_cash == 0
									&& prepay_epay == 0
									&& add_epay == 0
									&& refund_epay == 0
									&& pursue_epay == 0
									&& pfee_epay == 0
									&& prepay_card == 0
									&& add_card == 0
									&& refund_card == 0
									&& pursue_card == 0
									&& pfee_card == 0
									&& escape ==0){
								continue;
							}
							va[0] = id;
							va[1] = prepay_cash;
							va[2] = add_cash;
							va[3] = refund_cash;
							va[4] = pursue_cash;
							va[5] = pfee_cash;
							va[6] = prepay_epay;
							va[7] = add_epay;
							va[8] = refund_epay;
							va[9] = pursue_epay;
							va[10] = pfee_epay;
							va[11] = escape;
							va[12] = ntime;
							va[13] = prepay_card;
							va[14] = add_card;
							va[15] = refund_card;
							va[16] = pursue_card;
							va[17] = pfee_card;
							va[18] = 2;
							values.add(va);
						}
						int r = dataBaseService.bathInsert(sql, values, new int []{4,3,3,3,3,3,3,3,3,3,3,3,4,3,3,3,3,3,4});
						log.error("r:"+r);
					}
					
				}
			}
			
		} catch (Exception e) {
			
		}
	}
	
	private void berthIncomeAnly(Long ntime){
		try {
			log.error("��ʼͳ�Ʋ�λ����");
			long groupid = 28;//����
			List<Berth> berths = pgOnlyReadService.getPOJOList("select id from com_park_tb where " +
					" is_delete=? and comid in (select id from com_info_tb where state<>? and groupid=? )" +
					" order by id ", new Object[]{0, 1, groupid}, Berth.class);
			List<Object> idList = new ArrayList<Object>();
			if(berths != null && !berths.isEmpty()){
				for(Berth berth : berths){
					idList.add(berth.getId());
				}
				StatsReq req = new StatsReq();
				req.setIdList(idList);
				req.setStartTime(ntime - 24 * 60 * 60);
				req.setEndTime(ntime);
				StatsFacadeResp resp = accountFacade.statsBerthAccount(req);
				if(resp.getResult() == 1){
					List<StatsAccountClass> classes = resp.getClasses();
					String sql = "insert into parkuser_income_anlysis_tb(uin,prepay_cash,add_cash,refund_cash,pursue_cash,pfee_cash,prepay_epay," +
							"add_epay,refund_epay,pursue_epay,pfee_epay,escape,create_time,prepay_card,add_card," +
							"refund_card,pursue_card,pfee_card,type) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
					if(classes != null && !classes.isEmpty()){
						List<Object[]> values = new ArrayList<Object[]>();
						for(StatsAccountClass account : classes){
							Object[] va = new Object[19];
							Long id = account.getId();
							Double prepay_cash = account.getCashPrepayFee();//�ֽ�Ԥ֧��
							Double add_cash = account.getCashAddFee();//�ֽ𲹽�
							Double refund_cash = account.getCashRefundFee();//�ֽ��˿�
							Double pursue_cash = account.getCashPursueFee();//�ֽ�׷��
							Double pfee_cash = account.getCashParkingFee();//�ֽ�ͣ���ѣ���Ԥ����
							Double prepay_epay = account.getePayPrepayFee();//����Ԥ֧��
							Double add_epay = account.getePayAddFee();;//���Ӳ���
							Double refund_epay = account.getePayRefundFee();//�����˿�
							Double pursue_epay = account.getePayPursueFee();//����׷��
							Double pfee_epay = account.getePayParkingFee();//����ͣ���ѣ���Ԥ����
							Double escape = account.getEscapeFee();//�ӵ�δ׷�ɵ�ͣ����
							Double prepay_card = account.getCardPrepayFee();//ˢ��Ԥ֧��
							Double add_card = account.getCardAddFee();//ˢ������
							Double refund_card = account.getCardRefundFee();//ˢ���˿�
							Double pursue_card = account.getCardPursueFee();//ˢ��׷��
							Double pfee_card = account.getCardParkingFee();//ˢ��ͣ���ѣ���Ԥ����
							
							if(prepay_cash == 0
									&& add_cash == 0
									&& refund_cash == 0
									&& pursue_cash == 0
									&& pfee_cash == 0
									&& prepay_epay == 0
									&& add_epay == 0
									&& refund_epay == 0
									&& pursue_epay == 0
									&& pfee_epay == 0
									&& prepay_card == 0
									&& add_card == 0
									&& refund_card == 0
									&& pursue_card == 0
									&& pfee_card == 0
									&& escape ==0){
								continue;
							}
							va[0] = id;
							va[1] = prepay_cash;
							va[2] = add_cash;
							va[3] = refund_cash;
							va[4] = pursue_cash;
							va[5] = pfee_cash;
							va[6] = prepay_epay;
							va[7] = add_epay;
							va[8] = refund_epay;
							va[9] = pursue_epay;
							va[10] = pfee_epay;
							va[11] = escape;
							va[12] = ntime;
							va[13] = prepay_card;
							va[14] = add_card;
							va[15] = refund_card;
							va[16] = pursue_card;
							va[17] = pfee_card;
							va[18] = 1;
							values.add(va);
						}
						int r = dataBaseService.bathInsert(sql, values, new int []{4,3,3,3,3,3,3,3,3,3,3,3,4,3,3,3,3,3,4});
						log.error("r:"+r);
					}
					
				}
			}
			
		} catch (Exception e) {
			
		}
	}
	
	private void incomeAnly(Long ntime){
		try {
			log.error("ͳ���շ�Ա����");
			int pageNum = 1;
			int pageSize = 500;
			while (true) {
				log.error("pageNum:"+pageNum);
				List<Map<String, Object>> userList = pgOnlyReadService.getAll("select id from user_info_tb where " +
						" (auth_flag=? or auth_flag=?) and state<>? order by id ", new Object[]{1, 2, 1}, pageNum, pageSize);
				if(userList != null && !userList.isEmpty()){
					List<Object> idList = new ArrayList<Object>();
					for(Map<String, Object> map : userList){
						idList.add(map.get("id"));
					}
					incomeBlockAnly(idList, ntime);
				} else {
					log.error("ѭ������");
					return;
				}
				pageNum ++;
			}
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	private void incomeBlockAnly(List<Object> idList, Long curTime){
		try {
			StatsReq req = new StatsReq();
			req.setIdList(idList);
			req.setStartTime(curTime - 24 * 60 * 60);
			req.setEndTime(curTime);
			StatsFacadeResp resp = accountFacade.statsParkUserAccount(req);
			if(resp.getResult() == 1){
				List<StatsAccountClass> classes = resp.getClasses();
				if(classes != null && !classes.isEmpty()){
					String sql = "insert into parkuser_income_anlysis_tb(uin,prepay_cash,add_cash,refund_cash," +
							"pursue_cash,pfee_cash,prepay_epay,add_epay,refund_epay,pursue_epay,pfee_epay,escape," +
							"sensor_fee,create_time,prepay_card,add_card,refund_card,pursue_card,pfee_card," +
							"charge_card_cash,return_card_count,return_card_fee,act_card_count,act_card_fee,reg_card_count," +
							"reg_card_fee,bind_card_count) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
					List<Object[]> values = new ArrayList<Object[]>();
					for(StatsAccountClass accountClass : classes){
						long id = accountClass.getId();
						double cashParkingFee = accountClass.getCashParkingFee();
						double cashPrepayFee = accountClass.getCashPrepayFee();
						double cashRefundFee = accountClass.getCashRefundFee();
						double cashAddFee = accountClass.getCashAddFee();
						double cashPursueFee = accountClass.getCashPursueFee();
						
						double ePayParkingFee = accountClass.getePayParkingFee();
						double ePayPrepayFee = accountClass.getePayPrepayFee();
						double ePayRefundFee = accountClass.getePayRefundFee();
						double ePayAddFee = accountClass.getePayAddFee();
						double ePayPursueFee = accountClass.getePayPursueFee();
						
						double cardParkingFee = accountClass.getCardParkingFee();
						double cardPrepayFee = accountClass.getCardPrepayFee();
						double cardRefundFee = accountClass.getCardRefundFee();
						double cardAddFee = accountClass.getCardAddFee();
						double cardPursueFee = accountClass.getCardPursueFee();
						
						double escapeFee = accountClass.getEscapeFee();
						double sensorOrderFee = accountClass.getSensorOrderFee();
						
						//��Ƭͳ��
						double cardChargeCashFee = accountClass.getCardChargeCashFee();//��Ƭ��ֵ���
						double cardReturnFee = accountClass.getCardReturnFee();//�˿��˻����
						long cardReturnCount = accountClass.getCardReturnCount();//�˿�����
						double cardActFee = accountClass.getCardActFee();//�������
						long cardActCount = accountClass.getCardActCount();//���Ƭ����
						double cardRegFee = accountClass.getCardRegFee();//������ʼ�����
						long cardRegCount = accountClass.getCardRegCount();//��������
						long cardBindCount = accountClass.getCardBindCount();//���û�����
						
						if(cashParkingFee == 0
								&& cashAddFee == 0
								&& cashPrepayFee == 0
								&& cashPursueFee == 0
								&& cashRefundFee == 0
								&& ePayAddFee == 0
								&& ePayParkingFee == 0
								&& ePayPrepayFee == 0
								&& ePayPursueFee == 0
								&& ePayRefundFee == 0
								&& cardActCount == 0
								&& cardActFee == 0
								&& cardAddFee == 0
								&& cardBindCount == 0
								&& cardChargeCashFee == 0
								&& cardParkingFee == 0
								&& cardPrepayFee == 0
								&& cardPursueFee == 0
								&& cardRefundFee == 0
								&& cardRegCount == 0
								&& cardRegFee == 0
								&& cardReturnCount == 0
								&& cardReturnFee == 0
								&& sensorOrderFee == 0
								&& escapeFee == 0){
							continue;
						}
						
						Object[] va = new Object[27];
						va[0] = id;
						va[1] = cashPrepayFee;
						va[2] = cashAddFee;
						va[3] = cashRefundFee;
						va[4] = cashPursueFee;
						va[5] = cashParkingFee;
						va[6] = ePayPrepayFee;
						va[7] = ePayAddFee;
						va[8] = ePayRefundFee;
						va[9] = ePayPursueFee;
						va[10] = ePayParkingFee;
						va[11] = escapeFee;
						va[12] = sensorOrderFee;
						va[13] = curTime;
						va[14] = cardPrepayFee;
						va[15] = cardAddFee;
						va[16] = cardRefundFee;
						va[17] = cardPursueFee;
						va[18] = cardParkingFee;
						va[19] = cardChargeCashFee;
						va[20] = cardReturnCount;
						va[21] = cardReturnFee;
						va[22] = cardActCount;
						va[23] = cardActFee;
						va[24] = cardRegCount;
						va[25] = cardRegFee;
						va[26] = cardBindCount;
						values.add(va);
					}
					log.error("values size:"+values.size());
					if(!values.isEmpty()){
						int r = dataBaseService.bathInsert(sql, values, new int []{4,3,3,3,3,3,3,3,3,3,3,3,3,
								4,3,3,3,3,3,3,4,3,4,3,4,3,4});
						log.error("r:"+r);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void cardAnly(Long ntime){
		try {
			log.error("��ֵ��ͳ�Ʒ���...");
			List<Map<String, Object>> allList = pgOnlyReadService.getAll("select count(id) all_count," +
					"sum(balance) all_balance,group_id from com_nfc_tb where is_delete=? and type=?" +
					" and state in (?,?) group by group_id", new Object[]{0, 2, 0, 2});
			if(allList == null || allList.isEmpty()){
				return;
			}
			List<Object> idList = new ArrayList<Object>();
			String preParam = "";
			for(Map<String, Object> map : allList){
				idList.add(map.get("group_id"));
				if(preParam.equals("")){
					preParam = "?";
				}else{
					preParam += ",?";
				}
			}
			StatsReq req = new StatsReq();
			req.setStartTime(ntime - 24 * 60 * 60);
			req.setEndTime(ntime);
			req.setType(4);
			req.setIdList(idList);
			StatsCardResp resp = cardService.statsCard(req);
			if(resp.getResult() == 1){
				List<StatsCard> cards = resp.getCards();
				if(cards != null && !cards.isEmpty()){
					for(StatsCard card : cards){
						long id = card.getId();//ͳ�Ʊ��
						double regFee = card.getRegFee();//������ʼ�����
						double chargeCashFee = card.getChargeCashFee();//��Ƭ��ֵ���
						double returnFee = card.getReturnFee();//�˿��˻����
						double actFee = card.getActFee();//���Ƭ��ʼ�����
						long returnCount = card.getReturnCount();//�˿�����
						long actCount = card.getActCount();//���Ƭ����
						long regCount = card.getRegCount();//��������
						long bindCount = card.getBindCount();//���û�����
						//��Ŀͳ��
						double parkingFee = card.getParkingFee();//ͣ���ѣ���Ԥ����
						double prepayFee = card.getPrepayFee();//Ԥ��ͣ����
						double refundFee = card.getRefundFee();//Ԥ���˿Ԥ�����
						double addFee = card.getAddFee();//Ԥ�����ɣ�Ԥ�����㣩
						double pursueFee = card.getPursueFee();//׷��ͣ����
						
						double consumeFee = StringUtils.formatDouble(parkingFee + prepayFee 
								+ addFee + pursueFee - refundFee);
						for(Map<String, Object> map : allList){
							Long groupId = (Long)map.get("group_id");
							if(id == groupId.intValue()){
								map.put("slot_charge", chargeCashFee);
								map.put("slot_consume", consumeFee);
								map.put("slot_refund_count", returnCount);
								map.put("slot_refund_balance", returnFee);
								map.put("slot_act_count", actCount);
								map.put("slot_act_balance", actFee);
								map.put("slot_bind_count", bindCount);
							}
						}
					}
				}
			}
			String sql = "insert into card_anlysis_tb(create_time,all_count,all_balance," +
					"slot_charge,slot_consume,slot_refund_count,slot_refund_balance,slot_act_count," +
					"slot_act_balance,groupid,slot_bind_count) values(?,?,?,?,?,?,?,?,?,?,?)";
			List<Object[]> values = new ArrayList<Object[]>();
			for(Map<String, Object> map : allList){
				Long groupid = (Long)map.get("group_id");
				Long all_count = 0L;//��ֹ��ǰ�Ѽ��Ƭ������
				Double all_balance = 0d;//��ֹ��ǰ�Ѽ��Ƭ�����
				Double slot_charge = 0d;//һ���ڳ�ֵ�Ľ��
				Double slot_consume = 0d;//һ�������ѵĽ��
				Long slot_refund_count = 0L;//һ����ע���Ŀ�Ƭ����
				Double slot_refund_balance = 0d;//һ����ע���Ŀ�Ƭ�˻��Ľ��
				Long slot_act_count = 0L;//һ���ڵļ��Ƭ����
				Double slot_act_balance = 0d;//һ���ڼ���Ŀ�Ƭ��ʼ�����
				Long slot_bind_count = 0L;//һ���ڰ󶨵Ŀ�Ƭ����
				if(map.get("all_count") != null){
					all_count = (Long)map.get("all_count");
				}
				if(map.get("all_balance") != null){
					all_balance = Double.valueOf(map.get("all_balance") + "");
				}
				if(map.get("slot_charge") != null){
					slot_charge = Double.valueOf(map.get("slot_charge") + "");
				}
				if(map.get("slot_consume") != null){
					slot_consume = Double.valueOf(map.get("slot_consume") + "");
				}
				if(map.get("slot_refund_count") != null){
					slot_refund_count = (Long)map.get("slot_refund_count");
				}
				if(map.get("slot_refund_balance") != null){
					slot_refund_balance = Double.valueOf(map.get("slot_refund_balance") + "");
				}
				if(map.get("slot_act_count") != null){
					slot_act_count = (Long)map.get("slot_act_count");
				}
				if(map.get("slot_act_balance") != null){
					slot_act_balance = Double.valueOf(map.get("slot_act_balance") + "");
				}
				if(map.get("slot_bind_count") != null){
					slot_bind_count = (Long)map.get("slot_bind_count");
				}
				Object[] va = new Object[11];
				va[0] = ntime;
				va[1] = all_count;
				va[2] = all_balance;
				va[3] = slot_charge;
				va[4] = slot_consume;
				va[5] = slot_refund_count;
				va[6] = slot_refund_balance;
				va[7] = slot_act_count;
				va[8] = slot_act_balance;
				va[9] = groupid;
				va[10] = slot_bind_count;
				values.add(va);
			}
			int r = dataBaseService.bathInsert(sql, values, new int []{4,4,3,3,3,4,3,4,3,4,4});
			log.error("r:"+r);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
