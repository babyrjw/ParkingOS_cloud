package com.zld.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zld.pojo.Berth;
import com.zld.pojo.GenPosOrderReq;
import com.zld.pojo.GenPosOrderResp;
import com.zld.service.DataBaseService;
import com.zld.service.GenPosOrderService;
import com.zld.service.PgOnlyReadService;

/**
 * ���ɶ���ʱû��Ԥ֧�������ֽ�Ԥ֧��
 * @author whx
 *
 */
@Service("genCash")
public class GenPosOrderCashServiceImpl implements GenPosOrderService {
	@Autowired
	private DataBaseService writeService;
	@Autowired
	private PgOnlyReadService readService;
	
	Logger logger = Logger.getLogger(GenPosOrderCashServiceImpl.class);
	@Override
	public GenPosOrderResp genPosOrder(GenPosOrderReq req) {
		GenPosOrderResp resp = new GenPosOrderResp();
		try {
			//logger.error(req.toString());
			Long orderId = req.getOrderId();//������
			String carNumber = req.getCarNumber();//���ƺ�
			Berth berth = req.getBerth();//��λ
			String imei = req.getImei();
			Long uid = req.getUid();//�շ�Ա���
			Long userId = req.getUserId();//�������
			Long workId = req.getWorkId();//�ϰ���
			Long berthOrderId = req.getBerthOrderId();//��Ҫ�󶨵ĳ������������
			Long startTime = req.getStartTime();
			Integer cType = req.getcType();//�������ɷ�ʽ 2��¼�복�� 5���¿���Ա
			Integer carType = req.getCarType();//��������
			Integer version = req.getVersion();//�汾��
			Long parkId = req.getParkId();//�������
			Long groupId = req.getGroupId();//��Ӫ���ű��
			Long curTime = req.getCurTime();//��ǰʱ��
			//-------------------------Ԥ������----------------------//
			Double prepay = req.getPrepay();//Ԥ֧�����
			//-------------------------У�����----------------------//
			if(orderId <= 0 
					|| carNumber == null
					|| "".equals(carNumber)
					|| berth == null
					|| workId <= 0
					|| startTime <= 0
					|| uid <= 0
					|| parkId <= 0
					|| carType < 0
					|| groupId <= 0){
				resp.setResult(-2);
				resp.setErrmsg("��������");
				return resp;
			}
			//-------------------------Ԥ������----------------------//
			int pay_type = 0;//Ĭ��ֵ
			if(prepay > 0){//�ֽ�Ԥ֧��
				pay_type = 4;//�ֽ�Ԥ֧��
			}
			//-------------------------�����߼�----------------------//
			List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
			//���ɶ���
			Map<String, Object> orderSqlMap = new HashMap<String, Object>();
			//��ι�����¼��
			Map<String, Object> workDetailSqlMap = new HashMap<String, Object>();
			//�ֽ���ˮ
			Map<String, Object> cashSqlMap = new HashMap<String, Object>();
			//���²�λ������İ�״̬
			Map<String, Object> berthOrderSqlMap = new HashMap<String, Object>();
			//���²�λ��״̬
			Map<String, Object> berthSqlMap = new HashMap<String, Object>();
			
			orderSqlMap.put("sql", "insert into order_tb (id,comid,groupid,berthsec_id,uin,state," +
					"create_time,c_type,uid,imei,car_number,berthnumber,prepaid,prepaid_pay_time," +
					"pay_type,car_type) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			orderSqlMap.put("values", new Object[]{orderId, parkId, groupId, berth.getBerthsec_id(),
					userId, 0, startTime, cType, uid, imei, carNumber, berth.getId(), prepay, 
					curTime, pay_type, carType});//�����շѵ��ֶΣ�������ϸ����Ҫȡ��ǰʱ���ڣ�������һ��������п��ܶԲ�������
			bathSql.add(orderSqlMap);
			
			workDetailSqlMap.put("sql", "insert into work_detail_tb (uid,orderid,bid,workid,berthsec_id) " +
					"values(?,?,?,?,?)");
			workDetailSqlMap.put("values", new Object[]{uid, orderId, berth.getId(), workId,
					berth.getBerthsec_id()});
			bathSql.add(workDetailSqlMap);
			
			if(prepay > 0){//�ֽ�Ԥ֧��
				cashSqlMap.put("sql", "insert into parkuser_cash_tb(uin,amount,type,orderid,create_time," +
						"target,comid,berthseg_id,berth_id,groupid) values(?,?,?,?,?,?,?,?,?,?)");
				cashSqlMap.put("values", new Object[]{uid, prepay, 0, orderId, curTime, 1, parkId, 
						berth.getBerthsec_id(), berth.getId(), groupId});
				bathSql.add(cashSqlMap);
			}
			if(berthOrderId > 0){//�󶨳���������
				berthOrderSqlMap.put("sql", "update berth_order_tb set orderid=?,in_uid=? where id=? ");
				berthOrderSqlMap.put("values", new Object[]{orderId, uid, berthOrderId});
				bathSql.add(berthOrderSqlMap);
			}
			berthSqlMap.put("sql", "update com_park_tb set order_id=?,state=?,enter_time=? where id=? ");
			berthSqlMap.put("values", new Object[]{orderId, 1, curTime, berth.getId()});
			bathSql.add(berthSqlMap);
			boolean b = writeService.bathUpdate2(bathSql);
			if(b){
				resp.setResult(1);
				resp.setErrmsg("�����ɹ������ڴ�ӡ����ƾ��...");
				return resp;
			}
			resp.setResult(-5);
			resp.setErrmsg("����ʧ��");
		} catch (Exception e) {
			logger.error(e);
			resp.setResult(-1);
			resp.setErrmsg("ϵͳ����");
		}
		return resp;
	}

}
