package com.zldpark.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zldpark.pojo.AccountReq;
import com.zldpark.pojo.AccountResp;
import com.zldpark.pojo.ParkUserCashAccount;
import com.zldpark.pojo.StatsAccount;
import com.zldpark.pojo.StatsAccountResp;
import com.zldpark.pojo.StatsReq;
import com.zldpark.service.PgOnlyReadService;
import com.zldpark.service.StatsAccountService;

@Service("parkUserCash")
public class StatsParkUserCashAccountServiceImpl implements StatsAccountService {
	@Autowired
	private PgOnlyReadService readService;
	
	Logger logger = Logger.getLogger(StatsParkUserCashAccountServiceImpl.class);
	
	@Override
	public StatsAccountResp statsAccount(StatsReq req){
		//logger.error(req.toString());
		StatsAccountResp resp = new StatsAccountResp();
		try {
			long startTime = req.getStartTime();
			long endTime = req.getEndTime();
			List<Object> idList = req.getIdList();
			int type = req.getType();//0�����շ�Ա���ͳ�� 1�����������ͳ�� 2������λ�α�Ų�ѯ 3������λ��ѯ
			if(startTime <= 0
					|| endTime <= 0
					|| idList == null
					|| idList.isEmpty()){
				resp.setResult(-1);
				resp.setErrmsg("��������");
				return resp;
			}
			String column = null;
			if(type == 0){
				column = "uin";//���շ�Ա���ͳ��
			}else if(type == 1){
				column = "comid";//���������ͳ��
			}else if(type == 2){
				column = "berthseg_id";//����λ�α��ͳ��
			}else if(type == 3){
				column = "berth_id";//����λ���ͳ��
			}else if(type == 4){
				column = "groupid";
			}
			if(column == null){
				resp.setResult(-1);
				resp.setErrmsg("��������");
				return resp;
			}
			String preParams = "";
			for(int i = 0; i<idList.size(); i++){
				if(i == 0){
					preParams ="?";
				}else{
					preParams += ",?";
				}
			}
			List<Object> params = new ArrayList<Object>();
			params.add(0);
			params.add(startTime);
			params.add(endTime);
			params.add(0);//ͣ���ѣ���Ԥ����
			params.add(1);//Ԥ��ͣ����
			params.add(2);//Ԥ���˿Ԥ�����
			params.add(3);//Ԥ�����ɣ�Ԥ�����㣩
			params.add(4);//׷��ͣ����
			params.addAll(idList);
			String sql = "select sum(amount) summoney,target,"+column+" from parkuser_cash_tb where " +
					" is_delete=? and create_time between ? and ? and target in (?,?,?,?,?) " +
					" and "+column+" in ("+preParams+") group by "+column+",target ";
			List<Map<String, Object>> list = readService.getAllMap(sql, params);
			if(list != null && !list.isEmpty()){
				List<Object> existIds = new ArrayList<Object>();//�б��Ѵ��ڵ�����
				List<StatsAccount> accounts = new ArrayList<StatsAccount>();
				for(Map<String, Object> map : list){
					Long id = (Long)map.get(column);//����
					Integer target = (Integer)map.get("target");//��������
					Double summoney = Double.valueOf(map.get("summoney") + "");//���
					
					StatsAccount account = null;
					if(existIds.contains(id)){
						for(StatsAccount statsAccount : accounts){
							long statsId = statsAccount.getId();
							if(id.intValue() == statsId){//����ƥ�������
								account = statsAccount;
								break;
							}
						}
					}else{
						existIds.add(id);
						account = new StatsAccount();
						account.setId(id);
						accounts.add(account);
					}
					switch (target) {
					case 0://ͣ���ѣ���Ԥ����
						account.setParkingFee(summoney);
						break;
					case 1://Ԥ��ͣ����
						account.setPrepayFee(summoney);
						break;
					case 2://Ԥ���˿Ԥ�����
						account.setRefundFee(summoney);
						break;
					case 3://Ԥ�����ɣ�Ԥ�����㣩
						account.setAddFee(summoney);
						break;
					case 4://׷��ͣ����
						account.setPursueFee(summoney);
						break;
					default:
						break;
					}
				}
				resp.setAccounts(accounts);
				return resp;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		resp.setResult(-1);
		resp.setErrmsg("ϵͳ����");
		return resp;
	}

	@Override
	public AccountResp account(AccountReq req) {
		logger.error(req.toString());
		AccountResp resp = new AccountResp();
		try {
			long startTime = req.getStartTime();
			long endTime = req.getEndTime();
			long id = req.getId();
			int type = req.getType();//0�����շ�Ա���ͳ�� 1�����������ͳ�� 2������λ�α�Ų�ѯ 3������λ��ѯ
			int pageNum = req.getPageNum();
			int pageSize = req.getPageSize();
			if(startTime <= 0
					|| endTime <= 0
					|| id <= 0){
				resp.setResult(-1);
				resp.setErrmsg("��������");
				return resp;
			}
			String column = null;
			if(type == 0){
				column = "uin";//���շ�Ա���ͳ��
			}else if(type == 1){
				column = "comid";//���������ͳ��
			}else if(type == 2){
				column = "berthseg_id";//����λ�α��ͳ��
			}else if(type == 3){
				column = "berth_id";//����λ���ͳ��
			}else if(type == 4){
				column = "groupid";
			}
			if(column == null){
				resp.setResult(-1);
				resp.setErrmsg("��������");
				return resp;
			}
			List<Object> params = new ArrayList<Object>();
			params.add(0);
			params.add(startTime);
			params.add(endTime);
			params.add(0);//ͣ���ѣ���Ԥ����
			params.add(1);//Ԥ��ͣ����
			params.add(2);//Ԥ���˿Ԥ�����
			params.add(3);//Ԥ�����ɣ�Ԥ�����㣩
			params.add(4);//׷��ͣ����
			params.add(id);
			String sql = "select * from parkuser_cash_tb where is_delete=? and create_time between ? and ? " +
					" and target in (?,?,?,?,?) and "+column+"=? order by create_time desc ";
			List<ParkUserCashAccount> parkUserCashAccounts = readService.getPOJOList(sql, params, pageNum, pageSize,
					ParkUserCashAccount.class);
			resp.setParkUserCashAccounts(parkUserCashAccounts);
			resp.setResult(1);
			return resp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		resp.setResult(-1);
		resp.setErrmsg("ϵͳ����");
		return resp;
	}
}
