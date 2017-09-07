package com.zld.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zld.pojo.AutoPayPosOrderReq;
import com.zld.pojo.AutoPayPosOrderResp;
import com.zld.pojo.Card;
import com.zld.pojo.DefaultCardReq;
import com.zld.pojo.DefaultCardResp;
import com.zld.pojo.ManuPayPosOrderReq;
import com.zld.pojo.ManuPayPosOrderResp;
import com.zld.pojo.Order;
import com.zld.pojo.PayEscapePosOrderReq;
import com.zld.pojo.PayEscapePosOrderResp;
import com.zld.service.CardService;
import com.zld.service.DataBaseService;
import com.zld.service.PayPosOrderService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.Check;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;

/**
 * ˢ�̼ҿ����㶩��
 * @author Administrator
 *
 */
@Service("payCard")
public class PayPosOrderCardServiceImpl implements PayPosOrderService {
	@Autowired
	private CardService cardService;
	@Autowired
	private DataBaseService writeService;
	@Autowired
	private PgOnlyReadService readService;
	@Autowired
	private CommonMethods commonMethods;
	@Autowired
	private PublicMethods publicMethods;
	
	Logger logger = Logger.getLogger(PayPosOrderCardServiceImpl.class);
	
	/**
	 * ����ˢ���������Զ����㶩��
	 */
	@Override
	public AutoPayPosOrderResp autoPayPosOrder(AutoPayPosOrderReq req) {
		AutoPayPosOrderResp resp = new AutoPayPosOrderResp();
		try {
			//logger.error("req:"+req.toString());
			//----------------------------����--------------------------------//
			Long curTime = req.getCurTime();
			Order order = req.getOrder();
			Double money = req.getMoney();//�ܽ��
			String imei = req.getImei();//�ֻ�����
			Long workId = req.getWorkId();//��ǰ�ϰ��¼
			Long uid = req.getUid();//�շ�Ա���
			Integer version = req.getVersion();//�汾��
			Long brethOrderId = req.getBerthOrderId();
			Long endTime = req.getEndTime();
			long userId = req.getUserId();
			Long groupId = req.getGroupId();
			//----------------------------У�����--------------------------------//
			if(order == null 
					|| uid <= 0 
					|| workId <= 0 
					|| money < 0
					|| endTime == null
					|| curTime == null
					|| groupId <= 0){//money����Ϊ��
				resp.setResult(-2);
				resp.setErrmsg("��������");
				return resp;//��������
			}
			//----------------------------��ȡ������Ϣ--------------------------------//
			logger.error("order:"+order.toString());
			if(order.getState() != 0){
				resp.setResult(-3);
				resp.setErrmsg("�����Ѵ���");
				return resp;
			}
			long orderId = order.getId();
			double prepay = order.getPrepaid();
			String carNumber = order.getCar_number();
			String nfc_uuid = order.getNfc_uuid();
			int pay_type = order.getPay_type();
			long parkId = order.getComid();
			long berthId = order.getBerthnumber();
			long berthSegId = order.getBerthsec_id();
			if(parkId <= 0){
				resp.setResult(-4);
				resp.setErrmsg("������Ϣ����");
				return resp;
			}
			//----------------------------��ȡ��Ƭ��Ϣ--------------------------------//
			Card card = null;
			if(pay_type == 9 && nfc_uuid != null && !"".equals(nfc_uuid)){
				logger.error("ˢ��Ԥ֧����>>>orderid:"+orderId);
				card = readService.getPOJO("select * from com_nfc_tb where nfc_uuid=? " +
						" and is_delete=? and type=? and state<>? and group_id=? limit ? ", 
						new Object[]{nfc_uuid, 0, 2, 1, groupId, 1}, Card.class);
				//state 0������ 1��ע��  2�����û� 3����������ʱ�Ŀ�Ƭ�������ã�Ҫ�����ſ�ʹ�ã�
				//�����nfc_uuid�õ��ڣ���Ϊorder_tb��洢���Ǿ���������nfc_uuid
				if(card == null){
					logger.error("û���ҵ���Ƭ��Ϣ>>>orderid:"+orderId);
					resp.setResult(-7);
					resp.setErrmsg("������Ϣ����");
					return resp;
				}
			}else if(pay_type == 0 && prepay == 0){
				logger.error("û��Ԥ֧���������Ի�ȡһ��Ĭ�ϵĿ�Ƭ>>>orderid:"+orderId);
				DefaultCardReq defaultCardReq = new DefaultCardReq();
				defaultCardReq.setParkId(parkId);
				defaultCardReq.setUserId(userId);
				defaultCardReq.setCarNumber(carNumber);
				DefaultCardResp defaultCardResp = cardService.getDefaultCard(defaultCardReq);
				if(defaultCardResp.getResult() == 1 
						&& defaultCardResp.getCard() != null){
					card = defaultCardResp.getCard();
					pay_type = 9;//��Ϊˢ��֧����ʽ
					logger.error("pay_type:"+pay_type+"card:"+card.toString());
				}
			}
			//----------------------------�����߼�--------------------------------//
			logger.error("orderid:"+orderId+"pay_type:"+pay_type+",prepay:"+prepay
					+",money:"+money);
			if(card != null){//ˢ�������Ĳ��ҽ���㹻��
				double balance = card.getBalance();
				logger.error("orderid:"+orderId+"card:"+card.toString()+",balance:"+balance);
				if(pay_type == 9 && prepay + balance >= money){//ˢ��Ԥ֧������δԤ֧��
					List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
					//���¶���״̬
					Map<String, Object> orderSqlMap = new HashMap<String, Object>();
					orderSqlMap.put("sql", "update order_tb set state=?,total=?,end_time=?," +
							"pay_type=?,imei=?,out_uid=?,nfc_uuid=? where id=?");
					orderSqlMap.put("values", new Object[]{1, money, endTime, pay_type, imei,
							uid, card.getNfc_uuid(), orderId});
					bathSql.add(orderSqlMap);
					if(prepay < money){
						int consume_type = 0;//-- ���ѷ�ʽ 0��֧��ͣ���ѣ���Ԥ���� 1��Ԥ��ͣ���� 2������ͣ����
						String remark = "ͣ���� " + carNumber;
						if(prepay > 0){//��Ԥ��
							consume_type = 2;
							remark = "����ͣ���� " + carNumber;
						}
						Double pursueMoney = StringUtils.formatDouble(money - prepay);
						Map<String, Object> cardSqlMap = new HashMap<String, Object>();
						cardSqlMap.put("sql", "update com_nfc_tb set balance=balance-? where id=? ");
						cardSqlMap.put("values", new Object[]{pursueMoney, card.getId()});
						bathSql.add(cardSqlMap);
						Map<String, Object> cardAccountSqlMap = new HashMap<String, Object>();
						cardAccountSqlMap.put("sql", "insert into card_account_tb(uin,card_id,type,consume_type," +
								"amount,create_time,remark,orderid,uid,comid,berthseg_id,berth_id,groupid)" +
								" values(?,?,?,?,?,?,?,?,?,?,?,?,?)");
						cardAccountSqlMap.put("values", new Object[]{userId, card.getId(), 1, consume_type, 
								pursueMoney, curTime, remark, orderId, uid, parkId, berthSegId, berthId, groupId});
						bathSql.add(cardAccountSqlMap);
					}
					if(prepay > money){
						double refundMoney = StringUtils.formatDouble(prepay - money);
						//���¿�Ƭ���
						Map<String, Object> cardSqlMap = new HashMap<String, Object>();
						//��Ƭ��ˮ
						Map<String, Object> cardAccountSqlMap = new HashMap<String, Object>();
						cardSqlMap.put("sql", "update com_nfc_tb set balance=balance+? where id=?");
						cardSqlMap.put("values", new Object[]{refundMoney, card.getId()});
						bathSql.add(cardSqlMap);
						Long card_account_id = writeService.getkey("seq_card_account_tb");
						cardAccountSqlMap.put("sql", "insert into card_account_tb(id,uin,card_id,type,charge_type," +
								"amount,create_time,remark,orderid,uid,comid,berthseg_id,berth_id,groupid) " +
								"values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
						cardAccountSqlMap.put("values", new Object[]{card_account_id, userId, card.getId(), 0, 4, refundMoney,
								curTime, "Ԥ֧���˿� " + carNumber, orderId, uid, parkId, berthSegId, berthId, groupId});
						bathSql.add(cardAccountSqlMap);
					}
					if(order.getBerthnumber() > 0){
						//���²�λ״̬
						Map<String, Object> berthSqlMap = new HashMap<String, Object>();
						berthSqlMap.put("sql", "update com_park_tb set state=?,order_id=?,end_time=? " +
								" where id =? and order_id=?");
						berthSqlMap.put("values", new Object[]{0, null, endTime, 
								order.getBerthnumber(), orderId});
						bathSql.add(berthSqlMap);
					}
					if(brethOrderId > 0){
						//���³���������״̬
						Map<String, Object> berthOrderSqlMap = new HashMap<String, Object>();
						berthOrderSqlMap.put("sql", "update berth_order_tb set out_uid=?," +
								"order_total=? where id=? ");
						berthOrderSqlMap.put("values", new Object[]{uid, money, brethOrderId});
						bathSql.add(berthOrderSqlMap);
					}
					boolean result = writeService.bathUpdate2(bathSql);
					logger.error("result:"+result);
					if(result){
						resp.setResult(1);
						resp.setErrmsg("ˢ������ɹ�");
						//����Ƭ���Ѷ���
						//�ֻ���
						Map userMap = readService.getMap("select mobile from user_info_tb where id =? ", new Object[]{userId});
						logger.error("����Ƭ���Ѷ��ţ�user:"+userMap);
						if(userMap!=null&&!userMap.isEmpty()){
							String mobile =(String) userMap.get("mobile");
							logger.error("����Ƭ���Ѷ��ţ�mobile:"+mobile);
							if(money>0&&mobile!=null&&Check.checkMobile(mobile)){
								logger.error("��ʼ����Ƭ���Ѷ��ţ�mobile:"+mobile);
								String timeStr = TimeTools.gettime();
								Map comMap = readService.getMap("select company_name from com_info_tb where id =? ", new Object[]{order.getComid()});	
								String comName ="";
								if(comMap!=null)
									comName = (String)comMap.get("company_name");
								String dur = StringUtils.getTimeString(order.getCreate_time(), endTime);
								publicMethods.sendCardMessage(mobile, "����"+timeStr+"��"+comName+"ͣ����" +
										"ͣ��"+dur+"���Զ��۷�"+money+"Ԫ���շ�Ա���"+uid+".");
							}
						}
						return resp;
					}
				}
			}
			resp.setResult(0);
			resp.setErrmsg("ˢ�����㶩��ʧ��");
			return resp;
		} catch (Exception e) {
			logger.error(e);
			resp.setResult(-1);
			resp.setErrmsg("ϵͳ����");
		}
		return resp;
	}

	@Override
	public ManuPayPosOrderResp manuPayPosOrder(ManuPayPosOrderReq req) {
		ManuPayPosOrderResp resp = new ManuPayPosOrderResp();
		try {
			logger.error(req.toString());
			//----------------------------����--------------------------------//
			Long curTime = req.getCurTime();
			Order order = req.getOrder();
			Double money = req.getMoney();//�ܽ��
			String imei = req.getImei();//�ֻ�����
			Long workId = req.getWorkId();//��ǰ�ϰ��¼
			Long uid = req.getUid();//�շ�Ա���
			Integer version = req.getVersion();//�汾��
			Long brethOrderId = req.getBerthOrderId();
			Long endTime = req.getEndTime();
			String nfc_uuid = req.getNfc_uuid();
			long userId = req.getUserId();
			int bindcard = req.getBindcard();
			Long groupId = req.getGroupId();
			//----------------------------У�����--------------------------------//
			if(order == null 
					|| uid <= 0 
					|| workId <= 0 
					|| money < 0
					|| endTime == null
					|| curTime == null
					|| groupId <= 0){//money����Ϊ��
				resp.setResult(-2);
				resp.setErrmsg("��������");
				return resp;//��������
			}
			//----------------------------��ȡ������Ϣ--------------------------------//
			logger.error("order:"+order.toString());
			long orderId = order.getId();
			double prepay = order.getPrepaid();
			int state = order.getState();
			long berthId = order.getBerthnumber();
			int cType = order.getC_type();
			String carNumber = order.getCar_number();
			long parkId = order.getComid();
			long berthSegId = order.getBerthsec_id();
			if(state == 1){
				resp.setResult(-3);
				resp.setErrmsg("�����ѽ���");
				return resp;
			}
			if(state == 2){
				resp.setResult(-4);
				resp.setErrmsg("����Ϊδ�ɲ�����������!");
				return resp;
			}
			if(parkId <= 0){
				resp.setResult(-7);
				resp.setErrmsg("������Ϣ����!");
				return resp;
			}
			if(prepay >= money 
					|| cType == 5){//��Щ���Ӧ����autoPayPosOrder����
				resp.setResult(-8);
				resp.setErrmsg("����ʧ��!");
				return resp;
			}
			int consume_type = 0;
			double pursueMoney = money;
			String remark = "ͣ���� " + carNumber;
			if(prepay > 0){
				consume_type = 2;
				pursueMoney = StringUtils.formatDouble(money - prepay);
				remark = "����ͣ���� " + carNumber;
			}
			logger.error("consume_type:"+consume_type+",pursueMoney:"+pursueMoney);
			//----------------------------��ȡ��Ƭ��Ϣ--------------------------------//
			Card card = null;
			if(nfc_uuid == null || "".equals(nfc_uuid)){
				logger.error("�����Զ���ȡһ���������Ĭ�Ͽ�Ƭ>>>orderid:"+orderId);
				if(parkId > 0){
					DefaultCardReq defaultCardReq = new DefaultCardReq();
					defaultCardReq.setParkId(parkId);
					defaultCardReq.setUserId(userId);
					defaultCardReq.setCarNumber(carNumber);
					DefaultCardResp defaultCardResp = cardService.getDefaultCard(defaultCardReq);
					if(defaultCardResp.getResult() == 1 
							&& defaultCardResp.getCard() != null){
						card = defaultCardResp.getCard();
						logger.error(card.toString());
					}
				}
			} else {
				logger.error("�ֶ�ˢ��>>>orderid:"+orderId);
				card = commonMethods.card(nfc_uuid, groupId);
			}
			if(card == null){
				resp.setResult(-9);
				resp.setErrmsg("�ÿ�Ƭδ�����������ڵ�ǰ��Ӫ����");
				return resp;
			}
			logger.error(card.toString());
			if(card.getState() == 1){//ע��״̬
				resp.setResult(-12);
				resp.setErrmsg("��Ƭ��ע���������¿���");
				return resp;
			}
			if(card.getState() == 3){//����״̬
				resp.setResult(-5);
				resp.setErrmsg("��Ƭδ����");
				return resp;
			}
			if(bindcard == 0 && card.getState() == 0){
				resp.setResult(-6);
				resp.setErrmsg("��Ƭδ���û�");
				return resp;
			}
			if(card.getBalance() < pursueMoney){//����ֱ�ӷ��أ����������
				resp.setResult(-13);
				resp.setErrmsg("���㣬��Ƭ���:"+card.getBalance()+"Ԫ");
				return resp;
			}
			//----------------------------�����߼�--------------------------------//
			List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
			Map<String, Object> cardSqlMap = new HashMap<String, Object>();
			cardSqlMap.put("sql", "update com_nfc_tb set balance=balance-? where id=? ");
			cardSqlMap.put("values", new Object[]{pursueMoney, card.getId()});
			bathSql.add(cardSqlMap);
			Map<String, Object> cardAccountSqlMap = new HashMap<String, Object>();
			cardAccountSqlMap.put("sql", "insert into card_account_tb(uin,card_id,type,consume_type," +
					"amount,create_time,remark,orderid,uid,comid,berthseg_id,berth_id,groupid) " +
					" values(?,?,?,?,?,?,?,?,?,?,?,?,?)");
			cardAccountSqlMap.put("values", new Object[]{userId, card.getId(), 1, consume_type, pursueMoney,
					curTime, remark, orderId, uid, parkId, berthSegId, berthId, groupId});
			bathSql.add(cardAccountSqlMap);
			//���¶���״̬
			Map<String, Object> orderSqlMap = new HashMap<String, Object>();
			orderSqlMap.put("sql", "update order_tb set state=?,total=?,end_time=?,pay_type=?,imei=?," +
					"out_uid=?,nfc_uuid=? where id=?");
			orderSqlMap.put("values", new Object[]{1, money, endTime, 9, imei, uid, nfc_uuid, orderId});
			bathSql.add(orderSqlMap);
			if(berthId > 0){
				//���²�λ״̬
				Map<String, Object> berthSqlMap = new HashMap<String, Object>();
				berthSqlMap.put("sql", "update com_park_tb set state=?,order_id=?,end_time=?" +
						" where id =? and order_id=?");
				berthSqlMap.put("values", new Object[]{0, null, endTime, berthId, orderId});
				bathSql.add(berthSqlMap);
			}
			if(brethOrderId > 0){
				//���³���������״̬
				Map<String, Object> berthOrderSqlMap = new HashMap<String, Object>();
				berthOrderSqlMap.put("sql", "update berth_order_tb set out_uid=?,order_total=? where id=? ");
				berthOrderSqlMap.put("values", new Object[]{uid, money, brethOrderId});
				bathSql.add(berthOrderSqlMap);
			}
			boolean b = writeService.bathUpdate2(bathSql);
			logger.error("b"+b);
			if(b){
				resp.setResult(1);
				resp.setErrmsg("ˢ������ɹ�");
				return resp;
			}
			resp.setResult(0);
			resp.setErrmsg("ˢ������ʧ��");
		} catch (Exception e) {
			// TODO: handle exception
			resp.setResult(-1);
			resp.setErrmsg("ϵͳ����");
		}
		return resp;
	}

	@Override
	public PayEscapePosOrderResp payEscapePosOrder(PayEscapePosOrderReq req) {
		PayEscapePosOrderResp resp = new PayEscapePosOrderResp();
		try {
			logger.error(req.toString());
			Long curTime = req.getCurTime();
			Order order = req.getOrder();
			long uid = req.getUid();//�շ�Ա���
			String imei = req.getImei();
			double money = req.getMoney();//������
			int version = req.getVersion();
			long berthSegId = req.getBerthSegId();
			Long brethOrderId = req.getBerthOrderId();
			String nfc_uuid = req.getNfc_uuid();
			long userId = req.getUserId();
			int bindcard = req.getBindcard();
			Long groupId = req.getGroupId();//׷���շ�Ա���ڵ���Ӫ����
			Long berthId = req.getBerthId();//׷�ɶ����Ĳ�λ,����Ϊ-1��2016-10-14��ӣ�
			long parkId = req.getParkId();//׷���շ�Ա���ڵ�ͣ����
			if(order == null 
					|| uid <= 0 
					|| money < 0 
					|| curTime == null
					|| groupId <= 0
					|| parkId <= 0){//money����Ϊ0
				resp.setResult(-2);
				resp.setErrmsg("��������");
				return resp;
			}
			//-----------------------------��ȡ������Ϣ-----------------------------//
			logger.error("order:"+order.toString());
			long orderId = order.getId();
			double prepay = order.getPrepaid();
			int state = order.getState();
			int cType = order.getC_type();
			String carNumber = order.getCar_number();
			if(state == 0){
				resp.setResult(-3);
				resp.setErrmsg("���ӵ�������������");
				return resp;
			}
			if(state == 1){
				resp.setResult(-4);
				resp.setErrmsg("�����ѽ���");
				return resp;
			}
			if(prepay >= money
					|| cType == 5){//Ԥ��������֧������ʱӦ����autoPayPosOrder����
				resp.setResult(-8);
				resp.setErrmsg("����ʧ��!");
				return resp;
			}
			int consume_type = 3;//׷��ͣ����
			double pursueMoney = money;
			if(prepay > 0){
				pursueMoney = StringUtils.formatDouble(money - prepay);
			}
			logger.error("consume_type:"+consume_type+",pursueMoney:"+pursueMoney);
			//----------------------------��ȡ��Ƭ��Ϣ--------------------------------//
			Card card = null;
			if(nfc_uuid == null || "".equals(nfc_uuid)){
				logger.error("�����Զ���ȡһ���������Ĭ�Ͽ�Ƭ>>>orderid:"+orderId);
				if(parkId > 0){
					DefaultCardReq defaultCardReq = new DefaultCardReq();
					defaultCardReq.setParkId(parkId);
					defaultCardReq.setUserId(userId);
					defaultCardReq.setCarNumber(carNumber);
					DefaultCardResp defaultCardResp = cardService.getDefaultCard(defaultCardReq);
					if(defaultCardResp.getResult() == 1 
							&& defaultCardResp.getCard() != null){
						card = defaultCardResp.getCard();
						logger.error(card.toString());
					}
				}
			}else{
				logger.error("�ֶ�ˢ��>>>orderid:"+orderId);
				card = commonMethods.card(nfc_uuid, groupId);
				//�����������ѯ��׷�ɵ�ʱ�����һ����׷�ɶ���ӵ����ñ�����ܲ������
			}
			if(card == null){
				resp.setResult(-9);
				resp.setErrmsg("�ÿ�Ƭδ�����������ڵ�ǰ��Ӫ����");
				return resp;
			}
			logger.error(card.toString());
			if(card.getState() == 1){//ע��״̬
				resp.setResult(-12);
				resp.setErrmsg("��Ƭ��ע���������¿���");
				return resp;
			}
			if(card.getState() == 3){//����״̬
				resp.setResult(-5);
				resp.setErrmsg("��Ƭδ����");
				return resp;
			}
			if(bindcard == 0 && card.getState() == 0){//����״̬
				resp.setResult(-6);
				resp.setErrmsg("��Ƭδ���û�");
				return resp;
			}
			if(card.getBalance() < pursueMoney){//����ֱ�ӷ��أ����������
				resp.setResult(-13);
				resp.setErrmsg("���㣬��Ƭ���:"+card.getBalance()+"Ԫ");
				return resp;
			}
			//----------------------------�����߼�--------------------------------//
			List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
			Map<String, Object> cardSqlMap = new HashMap<String, Object>();
			cardSqlMap.put("sql", "update com_nfc_tb set balance=balance-? where id=? ");
			cardSqlMap.put("values", new Object[]{pursueMoney, card.getId()});
			bathSql.add(cardSqlMap);
			Map<String, Object> cardAccountSqlMap = new HashMap<String, Object>();
			cardAccountSqlMap.put("sql", "insert into card_account_tb(uin,card_id,type,consume_type," +
					"amount,create_time,remark,orderid,uid,comid,berthseg_id,berth_id,groupid) " +
					"values(?,?,?,?,?,?,?,?,?,?,?,?,?)");
			cardAccountSqlMap.put("values", new Object[]{userId, card.getId(), 1, consume_type, pursueMoney,
					curTime, "׷��ͣ���� " + carNumber, orderId, uid, parkId, berthSegId, berthId, groupId});
			bathSql.add(cardAccountSqlMap);
			//���¶���״̬
			Map<String, Object> orderSqlMap = new HashMap<String, Object>();
			orderSqlMap.put("sql", "update order_tb set state=?,total=?,pay_type=?,imei=?,out_uid=?," +
					"nfc_uuid=? where id=?");
			orderSqlMap.put("values", new Object[]{1, money, 9, imei, uid, nfc_uuid, orderId});
			bathSql.add(orderSqlMap);
			//����׷�ɱ�����
			Map<String, Object> escapeSqlMap = new HashMap<String, Object>();
			escapeSqlMap.put("sql", "update no_payment_tb set state=?,pursue_uid=?,pursue_time=?,act_total=?," +
					"pursue_comid=?,pursue_berthseg_id=?,pursue_berth_id=?,pursue_groupid=? where order_id=? ");
			escapeSqlMap.put("values", new Object[]{1, uid, curTime, money, parkId, berthSegId, berthId,
					groupId, orderId});
			bathSql.add(escapeSqlMap);
			
			if(brethOrderId > 0){
				//���³���������״̬
				Map<String, Object> berthOrderSqlMap = new HashMap<String, Object>();
				berthOrderSqlMap.put("sql", "update berth_order_tb set out_uid=?,order_total=? where id=? ");
				berthOrderSqlMap.put("values", new Object[]{uid, money, brethOrderId});
				bathSql.add(berthOrderSqlMap);
			}
			boolean b = writeService.bathUpdate2(bathSql);
			logger.error("b"+b);
			if(b){
				resp.setResult(1);
				resp.setErrmsg("ˢ��׷�ɳɹ�");
				return resp;
			}
			resp.setResult(0);
			resp.setErrmsg("ˢ��׷��ʧ��");
		} catch (Exception e) {
			logger.error(e);
			resp.setResult(-1);
			resp.setErrmsg("ϵͳ����");
		}
		return resp;
	}

}
