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
import com.zld.pojo.ManuPayPosOrderReq;
import com.zld.pojo.ManuPayPosOrderResp;
import com.zld.pojo.Order;
import com.zld.pojo.PayEscapePosOrderReq;
import com.zld.pojo.PayEscapePosOrderResp;
import com.zld.service.DataBaseService;
import com.zld.service.PayPosOrderService;
import com.zld.utils.StringUtils;

/**
 * �ֽ���㶩��
 * @author Administrator
 *
 */
@Service("payCash")
public class PayPosOrderCashServiceImpl implements PayPosOrderService {
	@Autowired
	private DataBaseService writeService;
	Logger logger = Logger.getLogger(PayPosOrderCashServiceImpl.class);
	
	/**
	 * �Զ������ֽ�Ԥ֧������㹻�Ķ���������0Ԫ����Ķ���
	 */
	@Override
	public AutoPayPosOrderResp autoPayPosOrder(AutoPayPosOrderReq req) {
		AutoPayPosOrderResp resp = new AutoPayPosOrderResp();
		try {
			logger.error(req.toString());
			Long curTime = req.getCurTime();
			Order order = req.getOrder();
			long uid = req.getUid();
			String imei = req.getImei();
			double money = req.getMoney();
			int version = req.getVersion();
			Long brethOrderId = req.getBerthOrderId();
			Long endTime = req.getEndTime();
			Long workId = req.getWorkId();//�ϰ���
			long groupId = req.getGroupId();
			if(order == null 
					|| uid <= 0 
					|| money < 0 
					|| endTime == null 
					|| curTime == null
					|| groupId <= 0){//money����Ϊ0
				resp.setResult(-1);
				resp.setErrmsg("��������");
				return resp;
			}
			//-----------------------------��ȡ������Ϣ-----------------------------//
			logger.error("order:"+order.toString());
			long orderId = order.getId();
			double prepay = order.getPrepaid();
			int payType = order.getPay_type();
			int state = order.getState();
			long berthId = order.getBerthnumber();
			long berthSegId = order.getBerthsec_id();
			long parkId = order.getComid();
			if(state == 1){
				resp.setResult(-2);
				resp.setErrmsg("�����ѽ���");
				return resp;
			}
			if(state == 2){
				resp.setResult(-3);
				resp.setErrmsg("��������Ϊ�ӵ�");
				return resp;
			}
			//-------------------------------�����߼�-----------------------------//
			logger.error("orderid:"+orderId+"payType:"+payType+",prepay:"+prepay
					+",money:"+money);
			if(payType == 4 && prepay >= money 
					|| payType == 0 && money == 0 && prepay == 0){//�ֽ�Ԥ֧���㹻����û��Ԥ֧�����ҽ��Ϊ0Ԫ
				logger.error("orderid:"+orderId);
				List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
				//���¶���״̬
				Map<String, Object> orderSqlMap = new HashMap<String, Object>();
				orderSqlMap.put("sql", "update order_tb set state=?,total=?,end_time=?,pay_type=?,imei=?,out_uid=? where id=?");
				orderSqlMap.put("values", new Object[]{1, money, endTime, 1, imei, uid, orderId});
				bathSql.add(orderSqlMap);
				if(berthId > 0){
					//���²�λ״̬
					Map<String, Object> berthSqlMap = new HashMap<String, Object>();
					berthSqlMap.put("sql", "update com_park_tb set state=?,order_id=?,end_time=? where id =? and order_id=?");
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
				if(payType == 4 && prepay > money){//Ԥ���˿�
					double refundMoney = StringUtils.formatDouble(prepay - money);
					Map<String, Object> cashAccountSqlMap = new HashMap<String, Object>();
					cashAccountSqlMap.put("sql", "insert into parkuser_cash_tb(uin,amount,orderid,create_time," +
							"target,ctype,comid,berthseg_id,berth_id,groupid) values(?,?,?,?,?,?,?,?,?,?)");
					cashAccountSqlMap.put("values",  new Object[]{uid, refundMoney, orderId, curTime, 2, 1, parkId,
							berthSegId, berthId, groupId});
					bathSql.add(cashAccountSqlMap);
				}
				boolean b = writeService.bathUpdate2(bathSql);
				logger.error("payMonthOrder b :"+ b+",orderid:"+orderId+",brethOrderid:"+brethOrderId);
				if(b){
					//--------------------------���ؽ��-------------------------//
					resp.setResult(1);
					resp.setErrmsg("�ֽ����ɹ�");
					return resp;
				}
			}
			resp.setResult(0);
			resp.setErrmsg("��������ʧ��");
		} catch (Exception e) {
			logger.error(e);
			resp.setResult(-4);
			resp.setErrmsg("ϵͳ����");
		}
		return resp;
	}

	/**
	 * �ֶ�����POS���������˽ӿ�����autoPayPosOrder�ӿڣ�
	 * ֻ��autoPayPosOrder�ӿڲ��ܽ����ʱ��Ż���øýӿڡ�
	 * @param req
	 * @return
	 */
	@Override
	public ManuPayPosOrderResp manuPayPosOrder(ManuPayPosOrderReq req) {
		ManuPayPosOrderResp resp = new ManuPayPosOrderResp();
		try {
		//	logger.error(req.toString());
			Long curTime = req.getCurTime();
			Order order = req.getOrder();
			long uid = req.getUid();//�շ�Ա���
			String imei = req.getImei();
			double money = req.getMoney();//������
			int version = req.getVersion();
			Long brethOrderId = req.getBerthOrderId();//�󶨵ĳ���������
			Long endTime = req.getEndTime();//����ʱ��
			Long workId = req.getWorkId();//�ϰ���
			Long groupId = req.getGroupId();
			if(order == null 
					|| uid <= 0 
					|| money < 0 
					|| endTime == null 
					|| curTime == null
					|| groupId <= 0){//money����Ϊ0
				resp.setResult(-1);
				resp.setErrmsg("��������");
				return resp;
			}
			//-----------------------------��ȡ������Ϣ-----------------------------//
		//	logger.error("order:"+order.toString());
			
			long orderId = order.getId();
			double prepay = order.getPrepaid();
			int payType = order.getPay_type();
			int state = order.getState();
			long parkId = order.getComid();
			long berthId = order.getBerthnumber();
			long berthSegId = order.getBerthsec_id();
			int cType = order.getC_type();
			if(state == 1){
				resp.setResult(-2);
				resp.setErrmsg("�����ѽ���");
				return resp;
			}else if(state == 2){
				resp.setResult(-3);
				resp.setErrmsg("����Ϊδ�ɲ�����������!");
				return resp;
			}
			if(prepay >= money
					|| cType == 5){//Ԥ��������֧������ʱӦ����autoPayPosOrder����
				resp.setResult(-4);
				resp.setErrmsg("����ʧ��!");
				return resp;
			}
			int target = 0;
			double pursueMoney = money;
			if(prepay > 0){
				target = 3;
				pursueMoney = StringUtils.formatDouble(money - prepay);
			}
			logger.error("target:"+target+",pursueMoney:"+pursueMoney);
			//-----------------------------�����߼�-----------------------------//
			List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
			//���¶���״̬
			Map<String, Object> orderSqlMap = new HashMap<String, Object>();
			orderSqlMap.put("sql", "update order_tb set state=?,total=?,end_time=?" +
					",pay_type=?,imei=?,out_uid=? where id=?");
			orderSqlMap.put("values", new Object[]{1, money, endTime, 1, imei, uid, orderId});
			bathSql.add(orderSqlMap);
			//�ֽ���ϸ��
			Map<String, Object> cashAccountsqlMap =new HashMap<String, Object>();
			cashAccountsqlMap.put("sql", "insert into parkuser_cash_tb(uin,amount,orderid,create_time," +
					"target,ctype,comid,berthseg_id,berth_id,groupid) values(?,?,?,?,?,?,?,?,?,?)");
			cashAccountsqlMap.put("values",  new Object[]{uid, pursueMoney, orderId, curTime, target, 0, 
					parkId, berthSegId, berthId, groupId});
			bathSql.add(cashAccountsqlMap);
			if(order.getBerthnumber() > 0){
				//���²�λ״̬
				Map<String, Object> berthSqlMap = new HashMap<String, Object>();
				berthSqlMap.put("sql", "update com_park_tb set state=?,order_id=?,end_time=? where id =?" +
						" and order_id=?");
				berthSqlMap.put("values", new Object[]{0, null, endTime, order.getBerthnumber(), orderId});
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
			logger.error("orderid:"+orderId+",b:"+b);
			if(b){
				resp.setResult(1);
				resp.setErrmsg("�ֽ����ɹ�");
				return resp;
			}
			resp.setResult(0);
			resp.setErrmsg("�ֽ����ʧ��");
		} catch (Exception e) {
			logger.error(e);
			resp.setResult(-5);
			resp.setErrmsg("ϵͳ����");
		}
		return resp;
	}

	@Override
	public PayEscapePosOrderResp payEscapePosOrder(PayEscapePosOrderReq req) {
		PayEscapePosOrderResp resp = new PayEscapePosOrderResp();
		try {
			//logger.error(req.toString());
			Long curTime = req.getCurTime();
			Order order = req.getOrder();
			long uid = req.getUid();//�շ�Ա���
			String imei = req.getImei();
			double money = req.getMoney();//������
			int version = req.getVersion();
			long berthSegId = req.getBerthSegId();
			Long brethOrderId = req.getBerthOrderId();
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
			//logger.error("order:"+order.toString());
			long orderId = order.getId();
			double prepay = order.getPrepaid();
			int state = order.getState();
			int cType = order.getC_type();
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
				resp.setResult(-5);
				resp.setErrmsg("����ʧ��!");
				return resp;
			}
			int target = 4;//׷��ͣ����
			double pursueMoney = money;
			if(prepay > 0){
				pursueMoney = StringUtils.formatDouble(money - prepay);
			}
			logger.error("target:"+target+",pursueMoney:"+pursueMoney);
			//-----------------------------�����߼�-----------------------------//
			List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
			//���¶���״̬
			Map<String, Object> orderSqlMap = new HashMap<String, Object>();
			orderSqlMap.put("sql", "update order_tb set state=?,total=?," +
					"pay_type=?,imei=?,out_uid=? where id=?");
			orderSqlMap.put("values", new Object[]{1, money, 1, imei, uid, orderId});
			bathSql.add(orderSqlMap);
			//�ֽ���ϸ��
			Map<String, Object> cashAccountsqlMap =new HashMap<String, Object>();
			cashAccountsqlMap.put("sql", "insert into parkuser_cash_tb(uin,amount,orderid,create_time,target," +
					"ctype,comid,berthseg_id,berth_id,groupid) values(?,?,?,?,?,?,?,?,?,?)");
			cashAccountsqlMap.put("values",  new Object[]{uid, pursueMoney, orderId, curTime, target, 0, parkId,
					berthSegId, berthId, groupId});
			bathSql.add(cashAccountsqlMap);
			//����׷�ɱ�����
			Map<String, Object> escapeSqlMap = new HashMap<String, Object>();
			escapeSqlMap.put("sql", "update no_payment_tb set state=?,pursue_uid=?,pursue_time=?,act_total=?," +
					"pursue_comid=?,pursue_berthseg_id=?,pursue_berth_id=?,pursue_groupid=? where order_id=? ");
			escapeSqlMap.put("values", new Object[]{1, uid, curTime, money, parkId, berthSegId, berthId, groupId,
					orderId});
			bathSql.add(escapeSqlMap);
			
			if(brethOrderId > 0){
				//���³���������״̬
				Map<String, Object> berthOrderSqlMap = new HashMap<String, Object>();
				berthOrderSqlMap.put("sql", "update berth_order_tb set out_uid=?,order_total=? where id=? ");
				berthOrderSqlMap.put("values", new Object[]{uid, money, brethOrderId});
				bathSql.add(berthOrderSqlMap);
			}
			boolean b = writeService.bathUpdate2(bathSql);
			logger.error("orderid:"+orderId+",b:"+b);
			if(b){
				resp.setResult(1);
				resp.setErrmsg("�ֽ�׷�ɳɹ�");
				return resp;
			}
			resp.setResult(0);
			resp.setErrmsg("�ֽ�׷��ʧ��");
		} catch (Exception e) {
			logger.error(e);
			resp.setResult(-1);
			resp.setErrmsg("ϵͳ����");
		}
		return resp;
	}

}
