package com.zld.pcloud_xunlang.pojo;

import java.util.List;

public class XunLangAttaches extends XunLangBase{
	private List<XunLangAttach> attaches ;

	public List<XunLangAttach> getAttaches() {
		return attaches;
	}

	public void setAttaches(List<XunLangAttach> attaches) {
		this.attaches = attaches;
	}

	@Override
	public String toString() {
		return super.toString()+" XunLangAttaches [attaches=" + attaches + "]";
	}
	
}
