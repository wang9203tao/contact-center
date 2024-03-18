package org.voice9.cc.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.listener.ConnectionListener;
import org.zhongweixian.server.websocket.WebSocketServer;
import org.zhongweixian.server.websocket.WebSocketServerHandler;

public class WssServer {
    private Logger logger = LoggerFactory.getLogger(WebSocketServer.class);
    private Integer port;
    private Integer heart = 60;
    private ConnectionListener connectionListener;
    private String path = "ws";
    private Integer parentGroupSize = 2;
    private Integer childGroupSize = 4;
    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workGroup = null;

    public WssServer(int port, ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
        this.port = port;
    }

    public WssServer(int port, String path, ConnectionListener connectionListener) {
        this.port = port;
        this.path = path;
        this.connectionListener = connectionListener;
    }

    public WssServer(int port, Integer heart, ConnectionListener connectionListener) {
        this.port = port;
        this.heart = heart;
        this.connectionListener = connectionListener;
    }

    public WssServer(int port, Integer heart, String path, ConnectionListener connectionListener) {
        this.port = port;
        this.heart = heart;
        this.path = path;
        this.connectionListener = connectionListener;
    }

    public WssServer(int port, Integer heart, String path, Integer parentGroupSize, Integer childGroupSize, ConnectionListener connectionListener) {
        this.port = port;
        this.heart = heart;
        this.path = path;
        this.parentGroupSize = parentGroupSize;
        this.childGroupSize = childGroupSize;
        this.connectionListener = connectionListener;
    }

    public void start() {
        this.bossGroup = new NioEventLoopGroup(this.parentGroupSize);
        this.workGroup = new NioEventLoopGroup(this.childGroupSize);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            ((ServerBootstrap)((ServerBootstrap)((ServerBootstrap)bootstrap.option(ChannelOption.SO_BACKLOG, 1024)).group(this.bossGroup, this.workGroup).channel(NioServerSocketChannel.class)).localAddress(new InetSocketAddress(this.port))).childHandler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast("idle", new IdleStateHandler((long)WssServer.this.heart, 0L, 0L, TimeUnit.SECONDS));
                    ch.pipeline().addLast(new ChannelHandler[]{new HttpServerCodec()});
                    ch.pipeline().addLast(new ChannelHandler[]{new ChunkedWriteHandler()});
                    ch.pipeline().addLast(new ChannelHandler[]{new HttpObjectAggregator(8192)});
                    ch.pipeline().addLast(new ChannelHandler[]{new WebSocketServerHandler(WssServer.this.heart, WssServer.this.connectionListener)});
                    ch.pipeline().addLast(new ChannelHandler[]{new WebSocketServerProtocolHandler("/" + WssServer.this.path, (String)null, true, 655350)});
                }
            });
            ChannelFuture channelFuture = bootstrap.bind().sync();
            if (channelFuture.isSuccess()) {
                this.logger.info("websocket started on port:{}, path:{}", this.port, this.path);
            }
        } catch (Exception var3) {
            this.logger.error(var3.getMessage(), var3);
        }

    }

    public void close() {
        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully();
        }

        if (this.workGroup != null) {
            this.workGroup.shutdownGracefully();
        }

    }
}
