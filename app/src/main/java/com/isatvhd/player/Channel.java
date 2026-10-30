package com.isatvhd.player;
public class Channel {
 public String name="", url="", logo="", group="", source="", id="";
 public boolean isVod;
 public Channel(String n,String u,String l,String g,String s,boolean v) {
  name=n; url=u; logo=l; group=g; source=s; isVod=v;
 }
}
