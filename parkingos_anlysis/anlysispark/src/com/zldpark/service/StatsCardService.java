package com.zldpark.service;

import com.zldpark.pojo.StatsCardResp;
import com.zldpark.pojo.StatsReq;

public interface StatsCardService {
	/**
	 * Í³¼Æ¿¨Æ¬
	 * @param req
	 * @return
	 */
	public StatsCardResp statsCard(StatsReq req);
}
