package org.voice9.cc.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.voice9.cc.websocket.wss.WssServer;
import org.zhongweixian.server.websocket.WebSocketServer;
import org.zhongweixian.server.websocket.WebSocketServerHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Create by caoliang on 2020/9/20
 */
@Component
public class WebSocketManager2 {
    private Logger logger = LoggerFactory.getLogger(WebSocketManager2.class);

    @Value("${ws.server.port:7300}")
    private Integer port;

    @Value("${ws.server.path:ws}")
    private String path;

    @Autowired
    private WebSocketHandler webSocketHandler;

    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workGroup = null;

    private Integer parentGroupSize = 2;
    private Integer childGroupSize = 4;

    private WebSocketServer webSocketServer;

    public void start() {
        //webSocketServer = new WebSocketServer(port, 10, path, 2, 4, webSocketHandler);
        //webSocketServer.start();
        this.bossGroup = new NioEventLoopGroup(this.parentGroupSize);
        this.workGroup = new NioEventLoopGroup(this.childGroupSize);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
//            ((ServerBootstrap)((ServerBootstrap)((ServerBootstrap)bootstrap.option(ChannelOption.SO_BACKLOG, 1024)).group(this.bossGroup, this.workGroup).channel(NioServerSocketChannel.class)).localAddress(new InetSocketAddress(this.port))).childHandler(new ChannelInitializer<SocketChannel>() {
//                protected void initChannel(SocketChannel ch) throws Exception {
//                    ch.pipeline().addLast("idle", new IdleStateHandler((long) 10, 0L, 0L, TimeUnit.SECONDS));
//                    ch.pipeline().addLast(new ChannelHandler[]{new HttpServerCodec()});
//                    ch.pipeline().addLast(new ChannelHandler[]{new ChunkedWriteHandler()});
//                    ch.pipeline().addLast(new ChannelHandler[]{new HttpObjectAggregator(8192)});
//                    ch.pipeline().addLast(new ChannelHandler[]{new WebSocketServerHandler(10, webSocketHandler)});
//                    ch.pipeline().addLast(new ChannelHandler[]{new WebSocketServerProtocolHandler("/" + path, (String)null, true, 655350)});
//                }
//            });
            bootstrap.group(bossGroup, workGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new ChannelInitializer() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast("codec-http", new HttpServerCodec());
                    pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                    pipeline.addLast("handler", webSocketHandler);
                }

            });

            ChannelFuture channelFuture = bootstrap.bind().sync();
            if (channelFuture.isSuccess()) {
                this.logger.info("websocket started on port:{}, path:{}", this.port, this.path);
            }
        } catch (Exception var3) {
            this.logger.error(var3.getMessage(), var3);
        }
        webSocketHandler.check();
        logger.info("websocket server:{} start", port,path);
    }

    public void stop() {
        if (webSocketServer == null) {
            return;
        }
        logger.info("websocket server:{} stop", port);
        webSocketHandler.stop();
        webSocketServer.close();
    }
}
