package com.isatvhd.player;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private GitHubSource github;
 private LinearLayout rows;
 private TextView status;
 private final List<Channel> all=new ArrayList<>();
 private String selected="DOMINO";
 @Override public void onCreate(Bundle b){super.onCreate(b);github=new GitHubSource(this);buildUi();showCache();}
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView label(String text,int size){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(Color.WHITE);return t;}
 private void buildUi(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(14),dp(20),dp(10));root.setBackgroundColor(0xff0b1425);
  TextView title=label("İSA TV HD",30);title.setTypeface(null,1);root.addView(title);
  LinearLayout tabs=new LinearLayout(this);
  MaterialButton domino=new MaterialButton(this);domino.setText("İSA TV HD 1 · DOMİNO");
  MaterialButton gunes=new MaterialButton(this);gunes.setText("İSA TV HD 2 · GÜNEŞ");
  tabs.addView(domino,new LinearLayout.LayoutParams(0,dp(54),1));tabs.addView(gunes,new LinearLayout.LayoutParams(0,dp(54),1));root.addView(tabs);
  MaterialButton scan=new MaterialButton(this);scan.setText("OTOMATİK TARAMA");root.addView(scan);
  status=label("Kaynak seçin ve otomatik taramaya basın.",16);root.addView(status);
  ScrollView sc=new ScrollView(this);rows=new LinearLayout(this);rows.setOrientation(1);sc.addView(rows);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
  setContentView(root);
  domino.setOnClickListener(v->{selected="DOMINO";render();});gunes.setOnClickListener(v->{selected="GUNES";render();});
  scan.setOnClickListener(v->scan());scan.setFocusable(true);scan.requestFocus();
 }
 private void showCache(){
  for(String repo:new String[]{"DOMINO","GUNES"}){
   if(repo.equals("DOMINO"))loadCached(repo,"liste.json",false);
   else {loadCached(repo,"live2.json",false);loadCached(repo,"vod2.json",true);}
  }
  render();
 }
 private void loadCached(String repo,String file,boolean vod){
  String s=github.readCache(repo,file);if(s==null)return;
  try{all.addAll(JsonChannelParser.parse(s,file,vod));}catch(Exception ignored){}
 }
 private void scan(){
  status.setText("GitHub depoları taranıyor…"); 
  io.execute(()->{
   try{
    List<GitHubSource.Downloaded> d=github.downloadRepo(GitHubSource.DOMINO_OWNER,GitHubSource.DOMINO_REPO,Collections.singleton("liste.json"));
    List<GitHubSource.Downloaded> g=github.downloadRepo(GitHubSource.GUNES_OWNER,GitHubSource.GUNES_REPO,new HashSet<>(Arrays.asList("live2.json","vod2.json")));
    // Validate and parse all new files before replacing any cache.
    List<Channel> fresh=new ArrayList<>();
    for(GitHubSource.Downloaded f:d)fresh.addAll(JsonChannelParser.parse(f.json,f.fileName,false));
    for(GitHubSource.Downloaded f:g)fresh.addAll(JsonChannelParser.parse(f.json,f.fileName,f.fileName.toLowerCase(Locale.ROOT).endsWith("vod2.json")));
    github.commitCache("DOMINO",d);github.commitCache("GUNES",g);
    runOnUiThread(()->{all.clear();all.addAll(fresh);status.setText("Tarama tamamlandı · "+all.size()+" kayıt indirildi.");render();});
   }catch(Exception e){runOnUiThread(()->status.setText("Tarama hatası: "+e.getMessage()+" · Önceki kayıtlar korunuyor."));}
  });
 }
 private void render(){
  rows.removeAllViews();int count=0;
  for(Channel c:all){
   if(selected.equals("DOMINO")&&!c.source.toLowerCase(Locale.ROOT).contains("liste.json"))continue;
   if(selected.equals("GUNES")&&c.source.toLowerCase(Locale.ROOT).contains("liste.json"))continue;
   TextView row=label((c.isVod?"▶  ":"●  ")+c.name+(c.group.isEmpty()?"":"   ·   "+c.group),18);
   row.setPadding(dp(14),dp(13),dp(14),dp(13));row.setFocusable(true);
   row.setBackgroundResource(android.R.drawable.list_selector_background);
   row.setOnClickListener(v->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra("url",c.url);i.putExtra("name",c.name);startActivity(i);});
   rows.addView(row);count++;
  }
  if(count==0){TextView empty=label("Henüz liste yok. OTOMATİK TARAMA'ya basın.",18);empty.setPadding(dp(10),dp(20),dp(10),dp(20));rows.addView(empty);}
 }
 @Override protected void onDestroy(){super.onDestroy();io.shutdownNow();}
}
