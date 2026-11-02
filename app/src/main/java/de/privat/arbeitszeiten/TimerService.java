package de.privat.arbeitszeiten;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TimerService extends Service {
  static final String PREF="work_timer_pro", CHANNEL="work_timer_channel";
  static final String ACTION_START="start", ACTION_PAUSE="pause", ACTION_RESUME="resume", ACTION_STOP="stop";
  static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,MODE_PRIVATE);}
  static JSONObject state(Context c){
    android.content.SharedPreferences p=prefs(c); JSONObject j=new JSONObject();
    try {j.put("status",p.getString("status","idle"));j.put("start",p.getLong("start",0));j.put("pauseStart",p.getLong("pauseStart",0));j.put("pausedMs",p.getLong("pausedMs",0));j.put("now",System.currentTimeMillis());j.put("pending",p.getString("pending",""));}catch(Exception ignored){} return j;
  }
  static void command(Context c,String action){Intent i=new Intent(c,TimerService.class);i.setAction(action);if(android.os.Build.VERSION.SDK_INT>=26 && !ACTION_STOP.equals(action)) c.startForegroundService(i); else c.startService(i);}
  @Override public void onCreate(){super.onCreate();NotificationChannel ch=new NotificationChannel(CHANNEL,"Laufende Arbeitszeiterfassung",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(ch);}
  @Override public int onStartCommand(Intent intent,int flags,int startId){
    String a=intent==null?"":intent.getAction(); android.content.SharedPreferences p=prefs(this);String status=p.getString("status","idle");long now=System.currentTimeMillis();
    if(ACTION_START.equals(a) && "idle".equals(status) && p.getString("pending","").isEmpty()){
      p.edit().putString("status","running").putLong("start",now).putLong("pauseStart",0).putLong("pausedMs",0).apply();
    }else if(ACTION_PAUSE.equals(a)&&"running".equals(status)){
      p.edit().putString("status","paused").putLong("pauseStart",now).apply();
    }else if(ACTION_RESUME.equals(a)&&"paused".equals(status)){
      p.edit().putString("status","running").putLong("pausedMs",p.getLong("pausedMs",0)+Math.max(0,now-p.getLong("pauseStart",now))).putLong("pauseStart",0).apply();
    }else if(ACTION_STOP.equals(a) && !"idle".equals(status)){
      long start=p.getLong("start",now);long paused=p.getLong("pausedMs",0)+("paused".equals(status)?Math.max(0,now-p.getLong("pauseStart",now)):0);
      try {JSONObject record=new JSONObject();record.put("startMs",start);record.put("endMs",now);record.put("pausedMs",paused);record.put("id","auto-"+start);p.edit().putString("pending",record.toString()).putString("status","idle").apply();}catch(Exception ignored){}
      stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;
    }
    status=p.getString("status","idle");if("idle".equals(status)){stopSelf();return START_NOT_STICKY;}
    Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    String msg="paused".equals(status)?"Pause läuft – zum Fortsetzen öffnen":"Arbeitszeit läuft – zum Beenden öffnen";
    Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_recent_history).setContentTitle("Meine Arbeitszeiten Pro").setContentText(msg).setContentIntent(pi).setOngoing(true).build();
    startForeground(101,n);return START_STICKY;
  }
  @Override public android.os.IBinder onBind(Intent i){return null;}
}
