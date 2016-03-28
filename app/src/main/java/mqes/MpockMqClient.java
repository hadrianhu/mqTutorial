package mqes;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.sql.Timestamp;
import java.util.List;

import Mpock.Globals;
import Mpock.Vars;
import de.greenrobot.event.EventBus;
import eventBusClasses.MqConnected;
import eventBusClasses.MqDisonnected;



public class MpockMqClient implements MqttCallback {

    int state = BEGIN;
    static final int BEGIN = 0;
    public static final int CONNECTED = 1;
    static final int PUBLISHED = 2;
    static final int SUBSCRIBED = 3;
    static final int DISCONNECTED = 4;
    static final int FINISH = 5;
    static final int ERROR = 6;
    static final int DISCONNECT = 7;

    // PREFS
    SharedPreferences prefs;
    String userName;

    // Private instance variables
    MqttAsyncClient client;
    String brokerUrl;
    private boolean log = true;
    private MqttConnectOptions conOpt;
    // private boolean clean;
    Throwable ex = null;
    Object waiter = new Object();
    boolean donext = false;
    private String password;
    private String userNameMqtt;
    // String clientId = "testerhope";
    String clientId;
    MpockMqClient sampleClient;

    // ANDROID INSERTS
    Context context;

    String tag = "MpockMqClient";

    Vars vars;

    // BROADCAST CONSTANTS
    // PINGER
    public static final String MQTT_PING_ACTION = "com.dalelane.mqtt.PING";
    // DELIVERY COMPLETE
    public static final String MQTT_DEL_COMPLETE = "MQTT_DEL_COMPLETE";
    // CONNECTED
    public static final String MQTT_CONNECTED = "MQTT_CONNECTED";
    // DIS - CONNECTED
    public static final String MQTT_DISCONNECTED = "MQTT_DISCONNECTED";

    //NOTIFICAITON COUNTER
    private int numMessages = 0;


    public MpockMqClient(String userNameL, Context contextL)
            throws MqttException, PackageManager.NameNotFoundException {
        // public SampleAsyncCallBack(String userNameL) throws MqttException {
        log("creating SampleAsyncCallBack object");
        this.brokerUrl = "tcp://" + Globals.mqRoot + ":" + Globals.MQ_ROOT_PORT;

        //	this.quietMode = false;
        // this.clean = true;
        this.password = null;
        this.userNameMqtt = null;
        this.userName = userNameL;
        this.context = contextL;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        MemoryPersistence persistence = new MemoryPersistence();
        // MqttDefaultFilePersistence dataStore = new
        // MqttDefaultFilePersistence(tmpDir);

        vars = new Vars(contextL);

        try {
            log("creating object, username is:" + userName);
            // Construct the object that contains connection parameters
            // such as cleansession and LWAT
            conOpt = new MqttConnectOptions();
            // String shit = "gcmtest";
            conOpt.setWill(Globals.appName + "death", userName.getBytes(), 2, false);
            conOpt.setCleanSession(Globals.clean);
            // conOpt.setCleanSession(vars.mqClean);
            if (password != null) {
                conOpt.setPassword(this.password.toCharArray());
            }
            if (userName != null) {
                conOpt.setUserName(this.userName);
            }

            // Construct the MqttClient instance
            // client = new MqttAsyncClient(this.brokerUrl, clientId,
            // persistence);
            log("creating MqttAsyncClient username is:" + this.userName);
            client = new MqttAsyncClient(this.brokerUrl, this.userName,
                    persistence);

            // Set this wrapper as the callback handler
            client.setCallback(this);

        } catch (MqttException e) {
            e.printStackTrace();
            log("Unable to set up client: " + e.toString());
        }

    }


    public void publish(String topicName, int qos, byte[] payload,
                        String userContext) throws Throwable {
        log("MQTT CLIENT...PUBLSIHING FUNCTION....:");
        printState();
        // Use a state machine to decide which step to do next. State change
        // occurs
        // when a notification is received that an MQTT action has completed
        while (state != FINISH) {
            switch (state) {
                case BEGIN:
                    log("PUBLSIHING FUNCTION: CASE BEGIN");
                    // Connect using a non blocking connect
                    MqttConnector con = new MqttConnector();
                    con.doConnect();
                    break;
                case CONNECTED:
                    log("PUBLSIHING FUNCTION: CASE CONNECTED");

                    // CREAT PUBLISHER OBJECT
                    Publisher pub = new Publisher();
                    pub.doPublish(topicName, qos, payload, userContext);

                    Subscriber sub = new Subscriber();

                    if (userName != null) {
                        //  sub.doSubscribe(Globals.appName + "/"+vars.clientId+"/#", 2);
                      //  sub.doSubscribe(Globals.appName + "/" + userName + "/#", 2);

                        sub.doSubscribe(Globals.appName + "/" + userName + "/#", 2);


                        log("username was NOT NULL we suscribed");
                    } else {
                        log("uername == null OR CLIENT ID IS ZERO");
                    }

                    //clientId = prefs.getString("clientId", "000");
//                    String clientId = prefs.getString("clientId", "000");
//                    if(vars.clientId != null && clientId != "000"){
//                        sub.doSubscribe(Globals.appName + "/"+vars.clientId+"/#", 2);
//                        log("vars.clientId was NOT NULL we suscribed");
//                    }else{
//                        log("vars.clientId == null OR CLIENT ID IS ZERO");
//                    }

                    //sub.doSubscribe(Globals.appName+"/everyone/#", 2);


                    // SUBSCRIBE STUFF
                    // log("starting Subscriber");
            /*Subscriber sub = new Subscriber();
            //	SUB TO EVERYONE
				sub.doSubscribe(Globals.appName+"/everyone/#", 2);
			//	SUB TO USER NAME
				sub.doSubscribe(Globals.appName+"/" + vars.prefs.getString("userId", null)+ "/#", 2);
            //  SUB TO GROUP CHAT
                sub.doSubscribe(Globals.appName+"/groups/1", 2);
                //NEW CONTACT
                sub.doSubscribe(Globals.appName+"/newContact", 2);
                //REMOVE CONTACT
                sub.doSubscribe(Globals.appName+"/removeContact",2);*/

                    state = FINISH;
                    log(">>>>>>>>>>>>>>>: STATE CHANGED TO:"
                            + MqUtilz.getState(state));
                    // log("state IS:" + state);
                    donext = true;
                    break;
                case PUBLISHED:
                    // state = FINISH;
                    // donext = true;
                    // break;
                    state = FINISH;
                    log(">>>>>>>>>>>>>>>: STATE CHANGED TO:" + getState(state));
                    donext = true;
                    break;
                case DISCONNECT:
                    Disconnector disc = new Disconnector();
                    disc.doDisconnect();
                    break;
                case ERROR:
                    throw ex;
                case DISCONNECTED:
                    // state = FINISH;
                    state = FINISH;
                    log(">>>>>>>>>>>>>>>: STATE CHANGED TO:" + getState(state));
                    donext = true;
                    break;
            }

            waitForStateChange(3000);
            // }
        }
    }

    public void publishMultiple(int qos,
                                String[][] pubArray) throws Throwable {
        log("30.0  MQTT CLIENT...publishMultiple FUNCTION....:");
        printState();

        while (state != FINISH) {
            log("starting while (state != FINISH)");
            switch (state) {
                case BEGIN:
                    log("PUBLSIHING FUNCTION: CASE BEGIN");
                    // Connect using a non blocking connect

                    MqttConnector con = new MqttConnector();
                    con.doConnect();
                    break;
                case CONNECTED:
                    log("PUBLSIHING FUNCTION: CASE CONNECTED");

                    // TWO DEMO ARRAY THAT CONTAINS TOPIC TO PUBLISH TO, WHAT TO
                    // PUBLISH AND CONTEXT TO USE
                    log("two dem array size:" + pubArray.length);

                    for (int i = 0; i < pubArray.length; i++) {
                        log("iiiiiiiiiiiiiiii, i:" + i);

                        log("pubArray[i][0]" + pubArray[i][0]);
                        log("pubArray[i][1]" + pubArray[i][1]);
                        log("pubArray[i][2]" + pubArray[i][2]);

                        Publisher pub = new Publisher();
                        pub.doPublish(pubArray[i][0], qos,
                                pubArray[i][1].getBytes(), pubArray[i][2]);
                        log("PUBLISHED STREAM:" + pubArray[i][0] + ":" + qos + ":"
                                + pubArray[i][1].getBytes() + ":" + pubArray[i][2]);
                        log("-----------------------------------------");
                    }


                    // DIRECT CREATION
                    log("calling single publishhhhhhhhhhhhhh");

                    state = FINISH;
                    log(">>>>>>>>>>>>>>>: STATE CHANGED TO:" + getState(state));
                    // log("state IS:" + state);
                    donext = true;
                    break;
                case PUBLISHED:
                    log("PUBLSIHING FUNCTION: CASE PUBLISHED");
                    // state = DISCONNECT;
                    state = FINISH;
                    log(">>>>>>>>>>>>>>>: STATE CHANGED TO:" + getState(state));
                    donext = true;
                    break;
                case DISCONNECT:
                    log("PUBLSIHING FUNCTION: CASE DISCONNECT");
                    Disconnector disc = new Disconnector();
                    disc.doDisconnect();
                    break;
                case ERROR:
                    throw ex;
                case DISCONNECTED:
                    state = FINISH;
                    log(">>>>>>>>>>>>>>>: STATE CHANGED TO:" + getState(state));
                    donext = true;
                    break;
            }

            // if (state != FINISH) {
            // Wait until notified about a state change and then perform next
            // action
            waitForStateChange(3000);
            // }
        }
    }

    private void waitForStateChange(int maxTTW) throws MqttException {
        synchronized (waiter) {
            if (!donext) {
                try {
                    waiter.wait(maxTTW);
                } catch (InterruptedException e) {
                    log("timed out");
                    e.printStackTrace();
                }

                if (ex != null) {
                    throw (MqttException) ex;
                }
            }
            donext = false;
        }
    }

    public void subscribe(String topicName, int qos) throws Throwable {
        //utils.Logg.logFunc("MQTT: - SUBSCRIBE FUNCTION");
        log("MQTT: - SUBSCRIBE FUNCTION");
        log("-- topic:" + topicName);
        printState();
        while (state != FINISH) {
            switch (state) {
                case BEGIN:
                    // Connect using a non blocking connect
                    MqttConnector con = new MqttConnector();// DIFFERENCE ONE
                    con.doConnect();
                    break;
                case CONNECTED:
                    // Subscribe using a non blocking subscribe
                    Subscriber sub = new Subscriber();
                    sub.doSubscribe(topicName, qos);

                    //IMPORTED FROM PUBLISH FUNCTION
                    state = FINISH;
                    log(">>>>>>>>>>>>>>>: STATE CHANGED TO:"
                            + MqUtilz.getState(state));
                    // log("state IS:" + state);
                    donext = true;
                    break;

                //break;
                case SUBSCRIBED:
                    log("MQTT: INSIDE SUBSCRIBE FUNCTION");
                    log("SATE IS DISCONNECT");
                    state = DISCONNECT;
                    donext = true;
                    break;
                case DISCONNECT:
                    Disconnector disc = new Disconnector();
                    disc.doDisconnect();
                    break;
                case ERROR:
                    throw ex;
                case DISCONNECTED:
                    state = FINISH;
                    log(">>>>>>>>>>>>>>>: STATE CHANGED TO:" + getState(state));
                    donext = true;
                    break;
            }

            // if (state != FINISH && state != DISCONNECT) {
            waitForStateChange(10000);
        }
        // }
    }

    //SUB TO MANY THINGS.

    public void subscribe(List<String> topics, int qos) throws Throwable {
        log("MQTT: - SUBSCRIBE FUNCTION");
        log("--  num topics:" + topics.size());
        printState();
        while (state != FINISH) {
            switch (state) {
                case BEGIN:
                    // Connect using a non blocking connect
                    MqttConnector con = new MqttConnector();// DIFFERENCE ONE
                    con.doConnect();
                    break;
                case CONNECTED:
                    // Subscribe using a non blocking subscribe
                    Subscriber sub = new Subscriber();

                    //LOOP THROUGH EACH TOPIC AND SUB
                    for (String t : topics) {
                        log("subscribing to:" + t);
                        sub.doSubscribe(t, qos);
                    }


                    //IMPORTED FROM PUBLISH FUNCTION
                    state = FINISH;
                    log(">>>>>>>>>>>>>>>: STATE CHANGED TO:"
                            + MqUtilz.getState(state));
                    // log("state IS:" + state);
                    donext = true;
                    break;

                //break;
                case SUBSCRIBED:
                    log("MQTT: INSIDE SUBSCRIBE FUNCTION");
                    log("SATE IS DISCONNECT");
                    state = DISCONNECT;
                    donext = true;
                    break;
                case DISCONNECT:
				/*Disconnector disc = new Disconnector();
				disc.doDisconnect();*/
                    break;
                case ERROR:
                    throw ex;
                case DISCONNECTED:
                    state = FINISH;
                    log(">>>>>>>>>>>>>>>: STATE CHANGED TO:" + getState(state));
                    donext = true;
                    break;
            }

            // if (state != FINISH && state != DISCONNECT) {
            waitForStateChange(10000);
        }
        // }
    }

    void log(String message) {
        if (log && Globals.log) {
            Log.v(tag, message);
        }
    }

    /**
     * *************************************************************
     */
	/* Methods to implement the MqttCallback interface */
    /**
     * *************************************************************
     */
    /**
     * @see MqttCallback#connectionLost(Throwable)
     */
    public void connectionLost(Throwable cause) {
        // Called when the connection to the server has been lost.
        // An application may choose to implement reconnection
        // logic at this point. This sample simply exits.
        log("Connection to " + brokerUrl + " lost!" + cause);
        log("attempty to REEONNNECT");


        // SEND connectionLost BROADCAST
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_DISCONNECTED);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        log("sending sent to server broadcast......................");
        context.sendBroadcast(broadcastIntent);

        //SEND EVENTBUS ABOUT DIS
        MqDisonnected mqDisonnected = new MqDisonnected("disconnected");
        EventBus.getDefault().postSticky(mqDisonnected);

    }


    public void deliveryComplete(IMqttDeliveryToken token) {
        log("44.0 dddddddddddddddd: MQTT DELIVERY CALL BACK: ddddddddddddddd");
        log("44.1 IMqttToken USER CONTEXT:" + (String) token.getUserContext());

        String messageContext = (String) token.getUserContext();

        // SEND MESSAGE RECEIVED BROADCAST
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Globals.DEL_TO_SERV);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra("message", messageContext);
        log("44.2 sending broadcast with extra:" + messageContext);
        log("44.3 sending sent to server broadcast......................");
        context.sendBroadcast(broadcastIntent);

        //SEND EVENTBUS ABOUT DISCONNECTED
        MqConnected mqConnected = new MqConnected("connected");
        EventBus.getDefault().postSticky(mqConnected);



    }

    /**
     * @throws org.json.JSONException
     * @see MqttCallback#messageArrived(String, MqttMessage)
     */
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // Called when a message arrives from the server that matches any
        // subscription made by the client
        String time = new Timestamp(System.currentTimeMillis()).toString();
        log("Time:\t" + time + "  Topic:\t" + topic
                + "  Message:\t" + new String(message.getPayload())
                + "  QoS:\t" + message.getQos());

        try {
            //PROCESS MESSAGE...
            if (topic.endsWith("/testing")) {
                log("topic.endsWith Globals.NEW_STUDENT_CHARGE");

            //    EventBus.getDefault().post(studentCharge);

            } else {
                //NotifyServant.oneOff(vars, "Message from the Server", Globals.NOTI_TYPE_UPDATE);
            }


            //  }
        } catch (Exception e) {
            if (e instanceof NullPointerException) {

                log("SHIT NPE!!");
            } else if (e instanceof com.google.gson.JsonSyntaxException) {
                //handle this one
            } else {
                // We didn't expect this one. What could it be? Let's log it, and let it bubble up the hierarchy.
                throw e;
            }
        }

    }

    // PRINT STATE
    void printState() {
        log("PRINT STATE:" + MqUtilz.getState(state));
    }

    // GET THE NOTIFIER STATE
    String getState(int state) {
        String stateF = "NO STATE FOUND";
        // log("getState: sent state:"+state);

        if (state == 0) {
            stateF = "BEGIN";
        } else if (state == 1) {
            stateF = "CONNECTED";
        } else if (state == 2) {
            stateF = "PUBLISHED";
        } else if (state == 3) {
            stateF = "SUBSCRIBED";
        } else if (state == 4) {
            stateF = "DISCONNECTED";
        } else if (state == 5) {
            stateF = "FINISH";
        } else if (state == 6) {
            stateF = "ERROR";
        } else if (state == 6) {
            stateF = "DISCONNECT";
        }
        return stateF;
    }

    /**
     * Connect in a non blocking way and then sit back and wait to be notified
     * that the action has completed.
     */
    public class MqttConnector {

        public MqttConnector() {
            log("MqttConnector() CONSTRUCTOR");
        }


        public void doConnect() {
            // Connect to the server
            // Get a token and setup an asynchronous listener on the token which
            // will be notified once the connect completes
            // log("Connecting to " + brokerUrl + " with client ID "
            // + client.getClientId());
            log("MQTT: MqttConnector: Connecting to " + brokerUrl
                    + " with client ID " + client.getClientId());

            IMqttActionListener conListener = new IMqttActionListener() {
                public void onSuccess(IMqttToken asyncActionToken) {
                    log("MQTT: CONNECTOR IMqttActionListener -- CONNECT SUCCESSFULL!");
                    log("MQTT: CONNECTOR IMqttActionListener -- context:"
                            + asyncActionToken.getUserContext());
                    state = CONNECTED;

                    MqConnected mqConnected = new MqConnected("connected");
                    EventBus.getDefault().postSticky(mqConnected);

                    //MySingleton.getInstance(vars.context).brokerConnected = true;
                    // log("MQCONN:set singletone broker conected to:"+MySingleton.getInstance(vars.context).brokerConnected);
                    carryOn();
                }

                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    ex = exception;
                    state = ERROR;
                    log("connect failed" + exception);
                    // CHECK IF ITS A MESSAGE ID
                    String messageContext = (String) asyncActionToken
                            .getUserContext();
                    // SEND MESSAGE RECEIVED BROADCAST
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(MQTT_CONNECTED);
                    broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    broadcastIntent.putExtra("message", messageContext);
                    log("sending sent to server broadcast......................");
                    context.sendBroadcast(broadcastIntent);
                    carryOn();
                }

                public void carryOn() {
                    synchronized (waiter) {
                        donext = true;
                        waiter.notifyAll();
                    }
                }
            };

            try {
                // Connect using a non blocking connect
                client.connect(conOpt, "Connect sample context", conListener);
            } catch (MqttException e) {
                state = ERROR;
                log(">>>>>>>>>>>>>>>: STATE CHANGED TO:" + getState(state));
                donext = true;
                ex = e;
            }
        }
    }

    public class Publisher {

        // public void doPublish(String topicName, int qos, byte[] payload) {
        public void doPublish(String topicName, int qos, byte[] payload,
                              String userContext) {

            log("ASYNCALLBACK:PULISHER CLASS: doPublish");


            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);

            String time = new Timestamp(System.currentTimeMillis()).toString();
            log("Publishing at: " + time + " to topic \"" + topicName
                    + "\" qos " + qos);

            // Setup a listener object to be notified when the publish
            // completes.
            // ASD
            IMqttActionListener pubListener = new IMqttActionListener() {
                public void onSuccess(IMqttToken asyncActionToken) {
                    log("IMqttActionListener Publish Completed");

                    String test = "test";
                    asyncActionToken.setUserContext(test);

                    //    String context = (String)   asyncActionToken.getUserContext();


                    state = PUBLISHED;
                    log("STATE SHOULD BE PUBLISHED:...........SSSSSSSSSSS."
                            + state);
                    log(">>>>>>>>>>>>>>>: STATE CHANGED TO:" + getState(state));
                    carryOn();
                }

                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    ex = exception;
                    state = ERROR;
                    log("Publish failed" + exception);
                    carryOn();
                }

                public void carryOn() {
                    log("ASYNCALLBACK:PULISHER CLASS: doPublish ||carryOn");
                    printState();
                    synchronized (waiter) {
                        donext = true;
                        waiter.notifyAll();
                    }
                }
            };

            try {
                // Publish the message
                // client.publish(topicName, message, "Pub sample context",
                // pubListener);
                // NEW WAY ADDING CONTEXT VAR
                client.publish(topicName, message, userContext, pubListener);
                //  client.pu

                // client.disconnect();
                // log("dddddddddddddddddddddddddd:DISONNECT CALLED");

            } catch (MqttException e) {
                state = ERROR;
                donext = true;
                ex = e;
            }
        }
    }

    /**
     * Subscribe in a non blocking way and then sit back and wait to be notified
     * that the action has completed.
     */
    public class Subscriber {

        public void doSubscribe(final String topicName, int qos) {
            log("-------SUBSCRIBER CLASS");
            // Make a subscription
            // Get a token and setup an asynchronous listener on the token which
            // will be notified once the subscription is in place.
            log("Subscribing to topic \"" + topicName + "\" qos " + qos);

            IMqttActionListener subListener = new IMqttActionListener() {
                public void onSuccess(IMqttToken asyncActionToken) {
                    log("Subscribe Completed to:"+topicName);
                    state = SUBSCRIBED;
                    // MySingleton.getInstance(vars.context).brokerConnected = true;

                    EventBus.getDefault().postSticky(new MqConnected("connected"));

                    carryOn();
                }

                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    ex = exception;
                    state = ERROR;
                    log("Subscribe failed" + exception);
                    carryOn();
                }

                public void carryOn() {
                    synchronized (waiter) {
                        donext = true;
                        waiter.notifyAll();
                    }
                }
            };

            try {
                client.subscribe(topicName, qos, "Subscribe sample context",
                        subListener);
            } catch (MqttException e) {
                state = ERROR;
                donext = true;
                ex = e;
            }
        }
    }

    /**
     * Disconnect in a non blocking way and then sit back and wait to be
     * notified that the action has completed.
     */
    public class Disconnector {

        public void doDisconnect() {
            // Disconnect the client
            log("Disconnecting");

            IMqttActionListener discListener = new IMqttActionListener() {
                public void onSuccess(IMqttToken asyncActionToken) {
                    log("Disconnect Completed");
                    state = DISCONNECTED;
                    carryOn();
                }

                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    ex = exception;
                    state = ERROR;
                    log("Disconnect failed" + exception);
                    carryOn();
                }

                public void carryOn() {
                    synchronized (waiter) {
                        donext = true;
                        waiter.notifyAll();
                    }
                }
            };

            try {
                client.disconnect("Disconnect sample context", discListener);
            } catch (MqttException e) {
                state = ERROR;
                donext = true;
                ex = e;
            }
        }
    }

    // @Override
    // public IBinder onBind(Intent arg0) {
    // // TODO Auto-generated method stub
    // return null;
    // }
}