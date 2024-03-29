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

package com.flazr.rtmp.server;

import android.util.Log;

import com.flazr.rtmp.RtmpConfig;
import com.flazr.util.StopMonitor;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

public class RtmpServer {
    
    protected static final ChannelGroup ALL_CHANNELS = new DefaultChannelGroup("rtmp-server");    

    protected static final Timer GLOBAL_TIMER =
            new HashedWheelTimer(RtmpConfig.TIMER_TICK_SIZE, TimeUnit.MILLISECONDS);

    public static void main(String[] args) throws Exception {

        RtmpConfig.configureServer();

        ChannelFactory factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());

        ServerBootstrap bootstrap = new ServerBootstrap(factory);

        bootstrap.setPipelineFactory(new RtmpServerPipelineFactory());
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        InetSocketAddress socketAddress = new InetSocketAddress(RtmpConfig.SERVER_PORT);
        bootstrap.bind(socketAddress);
        Log.i(RtmpServer.class.getName(), "server started, listening on: "+ socketAddress);

        Thread monitor = new StopMonitor(RtmpConfig.SERVER_STOP_PORT);
        monitor.start();
        monitor.join();

        GLOBAL_TIMER.stop();
        ChannelGroupFuture future = ALL_CHANNELS.close();
        Log.i(RtmpServer.class.getName(), "closing channels");
        future.awaitUninterruptibly();
        Log.i(RtmpServer.class.getName(), "releasing resources");
        factory.releaseExternalResources();
        Log.i(RtmpServer.class.getName(), "server stopped");

    }

}
