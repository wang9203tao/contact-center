package org.voice9.cc.websocket;


import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyWebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
    private Logger logger = LoggerFactory.getLogger(MyWebSocketServerHandler.class);
    private static final String WEBSOCKET_PATH = "";
    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest){
            //以http请求形式接入，但是走的是websocket
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        }else if (msg instanceof WebSocketFrame){
            //处理websocket客户端的消息
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        //要求Upgrade为websocket，过滤掉get/Post
        if (!req.decoderResult().isSuccess()
                || (!"websocket".equals(req.headers().get("Upgrade")))) {
            //若不是websocket方式，则创建BAD_REQUEST的req，返回给客户端
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:9502/websocket", null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory
                    .sendUnsupportedVersionResponse(ctx.channel()); } else {
            handshaker.handshake(ctx.channel(), req); }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // Check for closing frame if (frame instanceof CloseWebSocketFrame) {
        handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain()); return; }
        if(frame instanceof PingWebSocketFrame) {
        ctx.channel().write(new PongWebSocketFrame(frame.content().retain())); return; }
        if (!(frame instanceof TextWebSocketFrame)) {
        logger.error("数据帧类型不支持!"); throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName())); }

    // Send the uppercase string back. String request = ((TextWebSocketFrame) frame).text(); Print.info("Netty服务器接收到的信息: " + request); if (request.equals(Const.HEARTBEAT)){
            ctx.channel().write(new TextWebSocketFrame(request)); return; }





        }
}
