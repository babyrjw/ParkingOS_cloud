package com.zld.sdk.tcp;

import java.util.ArrayList;
import java.util.List;

/**
 * ��ֹ�����粻ͬ����ظ����󣬲��û��洦�����
 * @author liuqb
 * @date  2017-4-10
 */
public class CacheHandle {
	//����һ���洢signֵ��List����
	private static List<String> signs=new ArrayList<String>();;
	
    /**
     * �жϻ������Ƿ���ڵ�ǰsignֵ
     * @param item
     * @return boolean
     * ������ڷ���true������Ϊfalse
     */
    public static synchronized boolean containsItem(String item) {
		boolean isContain = signs.contains(item);
		if(signs.size()>1200){//����1200��Ԫ��ʱ��ɾ��500��
			int k = signs.size()-700;
			for(int i =0;i<k;i++)
				signs.remove(0);
		}
		if(isContain){
			if(!signs.contains(item))
				signs.add(item);
			return true;
		}else {
			signs.add(item);
		}
    	return false;
    }
    
}
