package com.zld.pcloud_xunlang.handler.impl;

import com.zld.pcloud_xunlang.handler.IHandler;

public class NullHandler implements IHandler{

	@Override
	public Object failed(Exception e) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object completed(String respBody) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object cancelled() {
		// TODO Auto-generated method stub
		return null;
	}

}
