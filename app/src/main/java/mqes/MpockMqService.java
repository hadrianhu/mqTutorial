package mqes;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import Mpock.Globals;
import Mpock.Vars;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import eventBusClasses.MqDisonnected;
import eventBusClasses.PubMessage;


public class MpockMqService extends Service {
    static final String TAG = "async test";

    MpockMqClient sampleClient;
    String userName;

    static final String tag = "MpockMqService";

    // -------------------keep alive
    // PINGER
    public static final String MQTT_PING_ACTION = "com.dalelane.mqtt.PING";
    // receiver that wakes the Service up when it's time to ping the server
    private PingSender pingSender;
    // receiver that notifies the Service when the phone gets data connection
    private NetworkConnectionIntentReceiver netConnReceiver;
    // KEEPALIVE
    int keepAlive = 50;

    // /----------- NEW STUFF
    Vars vars;
    // -------------------; BROADCAST STUFF
    // RECEIVER
    private AckReceiver ackReceiver;

    Boolean log = true;

    SharedPreferences prefs;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        log("service on create");
        // SharedPreferences prefs = PreferenceManager
        // .getDefaultSharedPreferences(this);
        // GET USERNAME
        // userName = "SHITtwo";
        //log("USERNAME FOUND IN PREFS:" + userName);
        // create alerter

        log("-----------: CREATING SERVICE DEMO CLASS :---------");

        vars = new Vars(this);
        // log("reset33.1 USERNAME FOUND IN PREFS:" + MiscUtilz.getMqUsername(vars));
        //userName = vars.clientId;

        //USE THE WIFI MAC ADDRESS - old way
        //userName = vars.macAddress;

        //NEW WAY
        userName = vars.macAddress;
        log("mqusername set to:"+userName);

        //PARTNER DEVICE NAME


        // userName = MiscUtilz.getMqUsername(vars);

        // BROADCAST RECEIVER STUFF
        Log.v(tag, "initing payment broadcast receiver");
        ackReceiver = new AckReceiver();

        // BROADCAST RECEIVER WHEN MESSAGE HAS BEEN DELIVERED
        // MESS DEL TO SERVER RECEIVER
        IntentFilter delServFilter = new IntentFilter(Globals.DEL_TO_SERV);
        delServFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(ackReceiver, delServFilter);

        //GET PREFS
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //REGIST FOR EVENTBUT
        EventBus.getDefault().register(this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        log("service on onStartCommand");
        String message = "asycntest - FROM NEW ANDROID";
        try {
            log("creating client");

            if (sampleClient == null) {

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        log("NEW THREAD");

                        //	scheduleNextPing();
                        // create pinger
                        if (pingSender == null) {
                            log("ping sender is NULL creating");
                            pingSender = new PingSender();
                            registerReceiver(pingSender, new IntentFilter(
                                    MQTT_PING_ACTION));
                        }
                        // STATE CHANGE RECEIVER
                    /*	if (netConnReceiver == null) {
                            netConnReceiver = new NetworkConnectionIntentReceiver();
							registerReceiver(netConnReceiver, new IntentFilter(
									ConnectivityManager.CONNECTIVITY_ACTION));
						}*/

                        if (isOnline()) {
                            try {
                                sampleClient = new MpockMqClient(
                                        userName, getApplicationContext());

                                log("username is: " + userName);
                                sampleClient.publish(Globals.heartbeatString,
                                        Globals.heartBeatMqQos, userName.getBytes(),
                                        Globals.heartbeatString);

                                //NEW WAY TO SUBSCRIBE IN BEGINNING INSTEAD OF AFTER EVERY PUBLISH
                               // subToTopics();
                                 scheduleNextPing();
                                log("scheduleNextPing called on onstart");
                            } catch (MqttException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                            // sampleClient = new SampleAsyncCallBack(userName);
                            catch (Throwable e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        } else {
                            log("inside service demo init: NOT ONLINE");
                        }
                    }
                };

                thread.start();

                // sampleClient.publish("hearbeat", 0, userName.getBytes());
            } else if (sampleClient != null) {
                log("CALLED SERVICE DEMO AGAIN, CLIENT IS NOT NULL PUBLISHING...GOOD NEWS");
                String test = "threads win!";

                //TESTING MOVED RECEIVER HERE TO PREVEN DOUBLE CLIENTS BEGIN CREATED
                if (netConnReceiver == null) {
                    netConnReceiver = new NetworkConnectionIntentReceiver();
                    registerReceiver(netConnReceiver, new IntentFilter(
                            ConnectivityManager.CONNECTIVITY_ACTION));
                }


                if (sampleClient.client.isConnected()) {
                    log("client is connecteed");
                    log("sample client current state:" + sampleClient.state);
                    sampleClient.state = 1;

                    if (intent != null) {
                        Bundle extras = intent.getExtras();
                        if (extras != null) {
                            if (extras.getString(Globals.COMMAND) != null) {
                                log("command request received");
                                String command = extras.getString(Globals.COMMAND);
                                log("command is:"+command);


                                if(Globals.COMMAND.equalsIgnoreCase("login")){
                                    log("command is login");

                                }
                                //COMMAND IS CHALLENGE

                            }
                        }
                    }

                    // sampleClient.publish("desktop2", 0, test.getBytes());
                } else if (!sampleClient.client.isConnected()) {
                    log("xxxxxxxxxxxxxxxxxxxxxxxx --client is not connecte43ed");
                    // alerter.alerter("Error", "Client disconnected");
                    reconnectClient();
                }

            }

            log("SERVICE DEMO - JUST FINISHED -sampleClient.publish");
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return START_STICKY;
    }

    //CLIENT POINTS LISTENER BUS LISTENER
    //CLIENT POINTS LISTENER BUS LISTENER
    @Subscribe
    public void onEvent(PubMessage scr ) {
        log("chat on eventbus studentCharge post receieved");

        //GET STUDENT STRING
        // StudentCharge sorm = new StudentCharge(StudentCharge studentCharge);
        //  log("got json string of Student:"+sorm.toString());

//        sampleClient.state = 1;
//        try {
//            log("going to publish to:"+partnerDeviceName + "/"+Globals.STUDENT_CHARGE_REPLY);
//            sampleClient.publish(Globals.appName + "/" +partnerDeviceName + "/"+Globals.STUDENT_CHARGE_REPLY,2,scr.toString().getBytes(),scr.toString());
//        } catch (Throwable throwable) {
//            throwable.printStackTrace();
//        }


    }


    // -----------------------------------------KEEP ALIVE ADS.

    private void scheduleNextPing() {
        log("scheduleNextPing..METHOD..called");


        PendingIntent pendingIntent = PendingIntent
                .getBroadcast(this, 0, new Intent(MQTT_PING_ACTION),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar wakeUpTime = Calendar.getInstance();
        // Calendar.m
        wakeUpTime.add(Calendar.SECOND, keepAlive);

        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(),
                pendingIntent);
    }

    public class PingSender extends BroadcastReceiver {

        public PingSender(){
            log("pingSender constructor scheduleNextPing");
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            log("ppppppppppppppp   Ping Sender broadcast received...");


            superReconnect();

            // start the next keep alive period
            scheduleNextPing();
            log("scheduleNextPing called from PingSender extends BroadcastReceiver");
        }
    }

    public void superReconnect() {
        log("+++++++++++++++++++++++++: super reconnect");

        //	if (isOnline()) {
        log(" we are online attempting to reconnect Attempting to reconnect...");

        if (sampleClient == null) {
            log("sample client is NULL");
            try {
                sampleClient = new MpockMqClient(userName,
                        getApplicationContext());
                String shit = "shit!";
                log("username is: " + userName);

                //PUB INITIAL HEARTBEAT
                sampleClient.publish(Globals.heartbeatString, Globals.heartBeatMqQos,
                        userName.getBytes(), Globals.heartbeatString);


            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Throwable e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } else if (sampleClient != null) {
            log("sample client is NOT NULL");

            if (sampleClient.client.isConnected()) {
                log("Service is ALREADY CONNECTED sending heartbeat");
                log("client STATE:" + sampleClient.state);
                try {
                    sampleClient.state = 1;
                    sampleClient.publish(Globals.heartbeatString, Globals.heartBeatMqQos,
                            userName.getBytes(), "memetoxheartbeatnnn");


                } catch (MqttPersistenceException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Throwable e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (!sampleClient.client.isConnected()) {
                log("inside ping function CLIENT NOT CONNECTED.");
                try {
                    sampleClient.state = MpockMqClient.BEGIN;
                    log("cleint state set to begin");
                    sampleClient.publish(Globals.heartbeatString, Globals.heartBeatMqQos,
                            userName.getBytes(), Globals.heartbeatString);
                    log("Service is NOT connected..now reconnected");
                } catch (Throwable e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    log("ERROR unable to reconnect CLIENT");

                    //SEND EVENTBUS ABOUT DIS
                    MqDisonnected mqDisonnected = new MqDisonnected("disconnected");
                    EventBus.getDefault().postSticky(mqDisonnected);
                }
            }
        }
    }

    //SUBSCRIBE LIST
    private void subToTopics() {

        //CHANGE STATE
        log("sample client state before sub:" + MqUtilz.getState(sampleClient.state));
        sampleClient.state = sampleClient.CONNECTED;
        log("state changed to:" + MqUtilz.getState(sampleClient.state));

        //LIST OF TPICS
        List<String> topicList = new ArrayList();
        // ADD EVERYONE SUB
        // topicList.add(Globals.appName + "/everyone/#");

        if (userName != null) {
            //  sub.doSubscribe(Globals.appName + "/"+vars.clientId+"/#", 2);
            topicList.add(Globals.appName + "/" + userName + "/#");
            log("username was NOT NULL we suscribed");
        } else {
            log("uername == null OR CLIENT ID IS ZERO");
        }


        for (String t : topicList) {
            log("subscribed to:" + topicList);
        }

        try {
            sampleClient.subscribe(topicList, 2);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void reconnectClient() {
        log("RECONNECT CLIENT FUNCTION");
        log("inside ping function CLIENT NOT CONNECTED.");
        try {
            sampleClient.state = MpockMqClient.BEGIN;
            log("cleint state set to begin");
            sampleClient.publish("memetoxheartbeat", Globals.mqQos,
                    userName.getBytes(), "memetoxheartbeat");
            log("Service is NOT connected..now reconnected");
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log("ERROR unable to reconnect CLIENT");
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected()) {
            return true;
        }
        return false;
    }

    // --------------------------------------------------------------------------------

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    public void log(String message) {
        if (log && Globals.log) {
            Log.v(tag, message);
        }

    }

    public class NetworkConnectionIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            log("nnnnnnnnnnnnnnnnnnnnnnnnnnnnnn    ---------NetworkConnectionIntentReceiver");

            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            WakeLock wl = pm
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();

            superReconnect();
            // }

            // we're finished - if the phone is switched off, it's okay for the
            // CPU
            // to sleep now
            wl.release();
        }
    }

    // +++++++++++++++++BROADCAST RECEIVER
    public class AckReceiver extends BroadcastReceiver {
        // public static final String ACK_RESP = "vumilia.MESSAGE_ACK";

        @Override
        public void onReceive(Context context, Intent intent) {

            log("GLOBALS mess rec:" + Globals.MESS_REC);
            log("action:" + intent.getAction());

            // CHECK INTENT
            if (intent.getAction() != null
                    && intent.getAction().equals(Globals.DEL_TO_SERV)) {

                log("rrrrrrrrrrrrrrrrr: MQ SERVICE, RECEIVE MESSAGE CALL BACK.rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr)");

                // System.out
                // .println("DEL TO SERVER BROADCAST RESPONSE RECEIVED....");

                // GET MESSAGE ID BY PARSING MESSAGE CONTEXT
                // Bundle extras = getIntent().getExtras();
                // if(extras != null){
                // GET MESSAGE
                String messageContext = intent.getStringExtra("message");
                log("messageContext = " + messageContext);

                // SEND MESSAGE RECEIVED BROADCAST
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(Globals.NEW_REQUEST);
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                //  broadcastIntent.putExtra("replyString", replyString);
                log("SENDING NEW REQUEST broadcast......................");
                vars.context.sendBroadcast(broadcastIntent);

            } else if (intent.getAction() != null
                    && intent.getAction().equals(Globals.MESS_REC)) {

                log("SEND TAG, NEW TAG RECEIVED BROADCAST RESPONSE RECEIVED....");

                // reLoad();
            } else {
                log("SEND_TAG RECEIVED, BUT ACTION NOT FOUND:"
                        + intent.getAction());
            }

        }

    }

}