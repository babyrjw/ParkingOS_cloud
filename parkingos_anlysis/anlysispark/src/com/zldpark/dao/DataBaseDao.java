package com.zldpark.dao;

import java.util.List;
import java.util.Map;

public interface DataBaseDao {
	
		
		/**
		 * ����ɾ����
		 * @param sql
		 * @param values
		 * @return
		 */
		//���� --����ɾ����
		public int update(String sql,Object[] values );
		//������� --�������
		public int bathInsert(String sql,List<Object[]> lists,int[] argTypes);
		
		/**
		 * ��ѯ����
		 * @param sql
		 * @param values
		 * @return
		 */
		//ȡ���� 
		public Long getLong(String sql,Object[] values );
		//���м�¼
		public List getAll(String sql,Object[] values );
		//��ĳһ�ֶ�
		public Object getObject(String sql,Object[] values,Class type);
		//ȡһ����¼
		public Map getPojo(String sql,Object[] values);
}
