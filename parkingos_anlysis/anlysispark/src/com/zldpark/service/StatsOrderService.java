package com.zldpark.service;

import com.zldpark.pojo.StatsOrderResp;
import com.zldpark.pojo.StatsReq;

public interface StatsOrderService {
	/**
	 * ����ͳ��
	 * @param req
	 * @return
	 */
	public StatsOrderResp statsOrder(StatsReq req);
}
