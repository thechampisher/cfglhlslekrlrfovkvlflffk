package com.isatvhd.player;

import android.content.Context;
import com.google.gson.*;
import okhttp3.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Resolves each repository's default branch at runtime, scans its Git tree,
 * then downloads only the named JSON data files. No dearbulut API is used.
 */
public class GitHubSource {
 public static final String DOMINO_OWNER="dominoiptv", DOMINO_REPO="dominotv";
 public static final String GUNES_OWNER="GeceHub", GUNES_REPO="gunestv";
 private final Context ctx;
 private final OkHttpClient client = new OkHttpClient.Builder()
   .connectTimeout(25,TimeUnit.SECONDS).readTimeout(90,TimeUnit.SECONDS)
   .callTimeout(120,TimeUnit.SECONDS).build();
 public GitHubSource(Context c){ctx=c.getApplicationContext();}

 public static class Downloaded {
  public final String fileName, json;
  Downloaded(String f,String j){fileName=f;json=j;}
 }
 public List<Downloaded> downloadRepo(String owner,String repo,Set<String> wanted) throws Exception {
  JsonObject meta=get("https://api.github.com/repos/"+owner+"/"+repo).getAsJsonObject();
  String branch=meta.get("default_branch").getAsString();
  JsonObject tree=get("https://api.github.com/repos/"+owner+"/"+repo+"/git/trees/"+branch+"?recursive=1").getAsJsonObject();
  if(tree.has("truncated") && tree.get("truncated").getAsBoolean())
    throw new IOException("GitHub dosya ağacı çok büyük; depo taraması kesildi.");
  List<String> paths=new ArrayList<>();
  for(JsonElement e:tree.getAsJsonArray("tree")) {
   JsonObject o=e.getAsJsonObject();
   if(!"blob".equals(o.get("type").getAsString())) continue;
   String path=o.get("path").getAsString();
   String lower=path.toLowerCase(Locale.ROOT);
   for(String file:wanted) if(lower.equals(file.toLowerCase(Locale.ROOT)) ||
       lower.endsWith("/"+file.toLowerCase(Locale.ROOT))) { paths.add(path); break; }
  }
  if(paths.isEmpty()) throw new FileNotFoundException(owner+"/"+repo+" içinde beklenen JSON bulunamadı: "+wanted);
  List<Downloaded> result=new ArrayList<>();
  for(String path:paths) {
   String raw="https://raw.githubusercontent.com/"+owner+"/"+repo+"/"+branch+"/"+encodePath(path);
   String body=getText(raw);
   JsonParser.parseString(body); // Validate before accepting.
   result.add(new Downloaded(path,body));
  }
  return result;
 }
 private JsonElement get(String url) throws Exception {
  Request r=new Request.Builder().url(url).header("Accept","application/vnd.github+json")
   .header("X-GitHub-Api-Version","2022-11-28").header("User-Agent","ISA-TV-HD-Android").build();
  try(Response x=client.newCall(r).execute()){
   if(!x.isSuccessful()||x.body()==null) throw new IOException("GitHub HTTP "+x.code()+" : "+url);
   return JsonParser.parseString(x.body().string());
  }
 }
 private String getText(String url) throws Exception {
  Request r=new Request.Builder().url(url).header("User-Agent","ISA-TV-HD-Android").build();
  try(Response x=client.newCall(r).execute()){
   if(!x.isSuccessful()||x.body()==null) throw new IOException("JSON indirilemedi HTTP "+x.code()+" : "+url);
   return x.body().string();
  }
 }
 private String encodePath(String p) {
  String[] parts=p.split("/"); StringBuilder b=new StringBuilder();
  for(String s:parts){if(b.length()>0)b.append('/');b.append(java.net.URLEncoder.encode(s,java.nio.charset.StandardCharsets.UTF_8).replace("+","%20"));}
  return b.toString();
 }
 public void commitCache(String repoKey,List<Downloaded> files) throws Exception {
  File dir=new File(ctx.getFilesDir(),"lists/"+repoKey); if(!dir.exists()&&!dir.mkdirs())throw new IOException("Önbellek klasörü açılamadı");
  for(Downloaded d:files) {
   String safe=d.fileName.replace('/','_');
   File tmp=new File(dir,safe+".tmp"), dst=new File(dir,safe);
   try(FileOutputStream out=new FileOutputStream(tmp)){out.write(d.json.getBytes("UTF-8"));out.getFD().sync();}
   if(dst.exists()&&!dst.delete())throw new IOException("Önbellek dosyası değiştirilemedi");
   if(!tmp.renameTo(dst))throw new IOException("Önbellek kaydı tamamlanamadı");
  }
 }
 public String readCache(String repoKey,String file) {
  File f=new File(new File(ctx.getFilesDir(),"lists/"+repoKey),file.replace('/','_'));
  if(!f.exists())return null;
  try(FileInputStream in=new FileInputStream(f)){byte[] b=new byte[(int)f.length()];int n=in.read(b);return new String(b,0,n,"UTF-8");}
  catch(Exception e){return null;}
 }
}
