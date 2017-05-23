package com.zldpark.service;

import com.zldpark.pojo.AccountReq;
import com.zldpark.pojo.AccountResp;
import com.zldpark.pojo.StatsAccountResp;
import com.zldpark.pojo.StatsReq;

/**
 * ��Ŀͳ��
 * @author whx
 *
 */
public interface StatsAccountService {
	
	/**
	 * ͳ����ˮ��Ŀ
	 * @param req
	 * @return
	 */
	public StatsAccountResp statsAccount(StatsReq req);
	
	/**
	 * ����ˮ��Ŀ��ϸ
	 * @param req
	 * @return
	 */
	public AccountResp account(AccountReq req);
	
}
