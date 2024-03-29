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

package com.flazr.rtmp.message;

import com.flazr.amf.Amf0Object;
import com.flazr.io.f4v.MovieInfo;
import com.flazr.io.f4v.TrackInfo;
import com.flazr.io.f4v.box.STSD.VideoSD;
import com.flazr.rtmp.RtmpHeader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jboss.netty.buffer.ChannelBuffer;

public abstract class Metadata extends AbstractMessage {

    protected String name;
    protected Object[] data;

    public Metadata(String name, Object... data) {
        this.name = name;
        this.data = data;
    }

    public Metadata(RtmpHeader header, ChannelBuffer in) {
        super(header, in);
    }

    //==========================================================================

    public static Metadata onPlayStatus(double duration, double bytes) {
        Map<String, Object> map = status(
                "NetStream.Play.Complete",
                pair("duration", duration),
                pair("bytes", bytes));
        return new MetadataAmf0("onPlayStatus", map);
    }

    public static Metadata rtmpSampleAccess() {
        return new MetadataAmf0("|RtmpSampleAccess", false, false);
    }

    public static Metadata dataStart() {
        return new MetadataAmf0("onStatus", object(pair("code", "NetStream.Data.Start")));
    }

    //==========================================================================

    /**
    [ (map){
        duration=112.384, moovPosition=28.0, width=640.0, height=352.0, videocodecid=avc1,
        audiocodecid=mp4a, avcprofile=100.0, avclevel=30.0, aacaot=2.0, videoframerate=29.97002997002997,
        audiosamplerate=24000.0, audiochannels=2.0, trackinfo= [
            (object){length=3369366.0, timescale=30000.0, language=eng, sampledescription=[(object){sampletype=avc1}]},
            (object){length=2697216.0, timescale=24000.0, language=eng, sampledescription=[(object){sampletype=mp4a}]}
        ]}]
    */

    public static Metadata onMetaDataTest(MovieInfo movie) {
        Amf0Object track1 = object(
            pair("length", 3369366.0),
            pair("timescale", 30000.0),
            pair("language", "eng"),
            pair("sampledescription", new Amf0Object[]{object(pair("sampletype", "avc1"))})
        );
        Amf0Object track2 = object(
            pair("length", 2697216.0),
            pair("timescale", 24000.0),
            pair("language", "eng"),
            pair("sampledescription", new Amf0Object[]{object(pair("sampletype", "mp4a"))})
        );
        Map<String, Object> map = map(
            pair("duration", movie.getDuration()),
            pair("moovPosition", movie.getMoovPosition()),
            pair("width", 640.0),
            pair("height", 352.0),
            pair("videocodecid", "avc1"),
            pair("audiocodecid", "mp4a"),
            pair("avcprofile", 100.0),
            pair("avclevel", 30.0),
            pair("aacaot", 2.0),
            pair("videoframerate", 29.97002997002997),
            pair("audiosamplerate", 24000.0),
            pair("audiochannels", 2.0),
            pair("trackinfo", new Amf0Object[]{track1, track2})
        );
        return new MetadataAmf0("onMetaData", map);
    }

    public static Metadata onMetaData(MovieInfo movie) {
        Map<String, Object> map = map(
            pair("duration", movie.getDuration()),
            pair("moovPosition", movie.getMoovPosition())
        );
        TrackInfo track1 = movie.getVideoTrack();
        Amf0Object t1 = null;
        if(track1 != null) {
            String sampleType = track1.getStsd().getSampleTypeString(1);
            t1 = object(
                pair("length", track1.getMdhd().getDuration()),
                pair("timescale", track1.getMdhd().getTimeScale()),
                pair("sampledescription", new Amf0Object[]{object(pair("sampletype", sampleType))})
            );
            VideoSD video = movie.getVideoSampleDescription();
            map(map,
                pair("width", video.getWidth()),
                pair("height", video.getHeight()),
                pair("videocodecid", sampleType)
            );
        }
        TrackInfo track2 = movie.getAudioTrack();
        Amf0Object t2 = null;
        if(track2 != null) {
            String sampleType = track2.getStsd().getSampleTypeString(1);
            t2 = object(
                pair("length", track2.getMdhd().getDuration()),
                pair("timescale", track2.getMdhd().getTimeScale()),
                pair("sampledescription", new Amf0Object[]{object(pair("sampletype", sampleType))})
            );
            map(map,
                pair("audiocodecid", sampleType)
            );
        }
        List<Amf0Object> trackList = new ArrayList<Amf0Object>();
        if(t1 != null) {
            trackList.add(t1);
        }
        if(t2 != null) {
            trackList.add(t2);
        }
        map(map, pair("trackinfo", trackList.toArray()));
        return new MetadataAmf0("onMetaData", map);
    }

    //==========================================================================

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("name: ").append(name);
        sb.append(" data: ").append(Arrays.toString(data));
        return sb.toString();
    }

}
