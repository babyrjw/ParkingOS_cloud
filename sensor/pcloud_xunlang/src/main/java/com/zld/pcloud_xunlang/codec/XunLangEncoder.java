package com.zld.pcloud_xunlang.codec;

import com.zld.pcloud_xunlang.util.Utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class XunLangEncoder extends MessageToByteEncoder<byte[]>{

	@Override
	protected void encode(ChannelHandlerContext paramChannelHandlerContext, byte[] data, ByteBuf buf)
			throws Exception {
		// TODO Auto-generated method stub
		byte sign = 0x00;
		System.out.println(Utils.bytesToHexString(data, 0, data.length));
		for(int i = 0 ; i < data.length; ++i){
			if(data[i] == (byte)0x0A){
				buf.writeByte(0x7D);
				buf.writeByte(0x58);
				sign ^= (byte)0x7D;
				sign ^= (byte)0x58;
			}else if(data[i] == (byte)0x7D){
				buf.writeByte(0x7D);
				buf.writeByte(0x5D);
				sign ^= (byte)0x7D;
				sign ^= (byte)0x5D;
			}else if(data[i] == (byte)0x00){
				buf.writeByte(0x7D);
				buf.writeByte(0x57);
				sign ^= (byte)0x7D;
				sign ^= (byte)0x57;
			}else{
				buf.writeByte(data[i]);
				sign ^= (byte)data[i];
			}
		}
		if(sign == (byte)0x0A){
			buf.writeByte(0x7D);
			buf.writeByte(0x58);
		}else if(sign == (byte)0x7D){
			buf.writeByte(0x7D);
			buf.writeByte(0x5D);
		}else if(sign == (byte)0x00){
			buf.writeByte(0x7D);
			buf.writeByte(0x57);
		}else{
			buf.writeByte(sign);
		}
	}

}
