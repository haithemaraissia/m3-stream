/*
 * Flazr <http://flazr.com> Copyright (C) 2009  Peter Thomas.
 *
 * This file is part of Flazr.
 *
 * Flazr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Flazr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Flazr.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.flazr.rtmp.client;

import android.util.Log;

import com.flazr.rtmp.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class RtmpClientHandshakeHandler extends FrameDecoder implements ChannelDownstreamHandler {

    private boolean rtmpe;
    private RtmpHandshake handshake;
    private boolean handshakeDone;

    public RtmpClientHandshakeHandler(RtmpClientSession session) {
        handshake = new RtmpHandshake(session.isRtmpe(), session.getSwfHash(), session.getSwfSize());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {        
    	Log.i(this.getClass().getName(), "connected, starting handshake");                
        Channels.write(ctx, e.getFuture(), handshake.encodeClient0());
        Channels.write(ctx, e.getFuture(), handshake.encodeClient1());
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer in) {               
        if(in.readableBytes() < 1 + RtmpHandshake.HANDSHAKE_SIZE * 2) {
            return null;
        }
        handshake.decodeServerAll(in);
        Channels.write(ctx, Channels.succeededFuture(channel), handshake.encodeClient2());
        handshakeDone = true;
        rtmpe = handshake.isRtmpe(); // rare chance server refused rtmpe
        if(handshake.getSwfvBytes() != null) {
            RtmpClientHandler clientHandler = channel.getPipeline().get(RtmpClientHandler.class);
            clientHandler.setSwfvBytes(handshake.getSwfvBytes());
        }
        if(!rtmpe) {
            channel.getPipeline().remove(this);
        }
        Channels.fireChannelConnected(ctx, channel.getRemoteAddress());
        return in;
    }

    @Override
    public void handleUpstream(final ChannelHandlerContext ctx, final ChannelEvent ce) throws Exception {
        if (!handshakeDone || !rtmpe || !(ce instanceof MessageEvent)) {
            super.handleUpstream(ctx, ce);
            return;
        }
        final ChannelBuffer in = (ChannelBuffer) ((MessageEvent) ce).getMessage();
        handshake.cipherUpdateIn(in);
        Channels.fireMessageReceived(ctx, in);
    }

    @Override
    public void handleDownstream(final ChannelHandlerContext ctx, final ChannelEvent ce) {
        if (!handshakeDone || !rtmpe || !(ce instanceof MessageEvent)) {
            ctx.sendDownstream(ce);
            return;
        }
        final ChannelBuffer in = (ChannelBuffer) ((MessageEvent) ce).getMessage();
        handshake.cipherUpdateOut(in);
        ctx.sendDownstream(ce);
    }

}
