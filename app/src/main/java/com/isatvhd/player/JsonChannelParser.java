package com.isatvhd.player;
import com.google.gson.*;
import java.util.*;

/** Best-effort parser for common IPTV JSON shapes; unknown objects are skipped, never fabricated. */
public final class JsonChannelParser {
 private static final String[] URL_KEYS={"url","stream","stream_url","link","file","src","play_url","playUrl"};
 private static final String[] NAME_KEYS={"name","title","channel_name"," kanal","label"};
 private static final String[] LOGO_KEYS={"logo","icon","image","poster","tvg-logo"};
 private static final String[] GROUP_KEYS={"group","group-title","category","genre","country"};
 public static List<Channel> parse(String json,String source,boolean vod) {
  List<Channel> out=new ArrayList<>();
  JsonElement root=JsonParser.parseString(json);
  walk(root,source,vod,out);
  return out;
 }
 private static void walk(JsonElement e,String source,boolean vod,List<Channel> out) {
  if(e==null||e.isJsonNull())return;
  if(e.isJsonArray()){for(JsonElement x:e.getAsJsonArray())walk(x,source,vod,out);return;}
  if(!e.isJsonObject())return;
  JsonObject o=e.getAsJsonObject();
  String url=value(o,URL_KEYS);
  String name=value(o,NAME_KEYS);
  if(!url.isEmpty() && (url.startsWith("http://")||url.startsWith("https://"))) {
   if(name.isEmpty())name=lastPart(url);
   String logo=value(o,LOGO_KEYS),group=value(o,GROUP_KEYS);
   out.add(new Channel(name,url,logo,group,source,vod));
  }
  for(Map.Entry<String,JsonElement> en:o.entrySet()) {
   if(en.getValue().isJsonArray()||en.getValue().isJsonObject())walk(en.getValue(),source,vod,out);
  }
 }
 private static String value(JsonObject o,String[] keys){
  for(String k:keys)if(o.has(k)&&o.get(k).isJsonPrimitive()){
   try{String v=o.get(k).getAsString().trim();if(!v.isEmpty())return v;}catch(Exception ignored){}
  }return "";
 }
 private static String lastPart(String u){try{String p=u.substring(0,u.indexOf('?')>0?u.indexOf('?'):u.length());return p.substring(p.lastIndexOf('/')+1);}catch(Exception e){return "Yayın";}}
 private JsonChannelParser(){}
}
