package com.zld.pcloud_xunlang;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.zld.pcloud_xunlang.codec.XunLangDecoder;
import com.zld.pcloud_xunlang.codec.XunLangEncoder;
import com.zld.pcloud_xunlang.codec.XunLangPOJODecoder;
import com.zld.pcloud_xunlang.codec.XunLangPOJOEncoder;
import com.zld.pcloud_xunlang.handler.impl.SensorServerHandler;
import com.zld.pcloud_xunlang.handler.impl.XunLangHandler;
import com.zld.pcloud_xunlang.util.SpringContextUtil;

public class SensorServer {
	Logger logger = Logger.getLogger(SensorServer.class);
	
	public SensorServer(){
		/**
		 * 加载application-context.xml，之后方可使用SpringContextUtil获取实例
		 */
		SpringContextUtil.initContext();
	}
	
	public void bind(int port) throws Exception{
		logger.error("listening port:" + port);
		EventLoopGroup parentGroup = new NioEventLoopGroup();
		EventLoopGroup childGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(parentGroup, childGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 1024)
				.childHandler(new ChildChannelHandler());
			//绑定端口，同步等待成功
			ChannelFuture f = bootstrap.bind(port).sync();
			//等待服务端监听端口关闭
			f.channel().closeFuture().sync();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			parentGroup.shutdownGracefully();
			childGroup.shutdownGracefully();
		}
	}
	
	public class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
		
		@Override
		protected void initChannel(SocketChannel channel) throws Exception {
			//这里的handler是线程安全的，所以添加了Sharable注解，共用一个实例
			SensorServerHandler sensorHandler = SpringContextUtil.getBean("sensorServerHandler",
					SensorServerHandler.class);
			ByteBuf buf = Unpooled.copiedBuffer(new byte[]{(byte)0xFF});
			channel.pipeline()
				.addLast(new DelimiterBasedFrameDecoder(8096, true, buf))
				.addLast(new XunLangDecoder())
				.addLast(new XunLangPOJODecoder())
				.addLast(new XunLangEncoder())
				.addLast(new XunLangPOJOEncoder())
				.addLast(new XunLangHandler());
				//.addLast(sensorHandler);
		}

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ExecutorService pool = Executors.newFixedThreadPool(2);
			//需开两个线程，因为这两个方法都是阻塞的
			pool.execute(new Runnable() {
				
				@Override
				public void run() {
					try {
						new SensorServer().bind(Constants.SENSOR_PORT);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
