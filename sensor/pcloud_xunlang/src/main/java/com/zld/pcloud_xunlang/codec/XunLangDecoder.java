package com.zld.pcloud_xunlang.codec;

import java.util.List;

import org.apache.log4j.Logger;

import com.zld.pcloud_xunlang.util.Utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class XunLangDecoder extends ByteToMessageDecoder{
	Logger logger = Logger.getLogger(XunLangDecoder.class);
	
	@Override
	protected void decode(ChannelHandlerContext paramChannelHandlerContext, ByteBuf buf,
			List<Object> paramList) throws Exception {
		// TODO Auto-generated method stub
		
		long size = buf.readableBytes();
		logger.info("接受到消息,长度:"+size);
		if(size <= 0){
			return;
		}
		try{
			byte[] data = new byte[buf.readableBytes()];
			byte[] source = new byte[buf.readableBytes()];
			int index = 0 ;
			int source_index = 0;
			byte sign = 0x00;
			while(buf.readableBytes() > 0){
				byte b = buf.readByte();
				source[source_index++] = b;
				if(b == (byte)0xFE){
					sign = (byte)(sign ^ 0xFE);
					b = buf.readByte();
					source[source_index++] = b;
					if(b == (byte)0x00){
						b = (byte)0xFE;
						sign = (byte)(sign ^ 0x00);
					}else if(b == (byte)0x01){
						b = (byte)0xFF;
						sign = (byte)(sign ^ 0x01);
					}else{
						throw new Exception("Protocol Error:0xFE:"+b);
					}
				}else{
					if(buf.readableBytes() > 0){
						sign = (byte)(sign ^ b);
					}
				}
				data[index++] = b;
			}			
			--index;
			if(sign == data[index]){
				paramList.add(data);
			}else{
				logger.error("Protocol Error:Sign Error");
				//throw new Exception("Protocol Error:Sign Error");
			}
			logger.info(Utils.bytesToHexString(source, 0, source.length));
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			//buf.release();
		}
	}
}
