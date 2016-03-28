package Mpock;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.google.gson.Gson;

/**
 * Created by Mac on 2016/03/17.
 */
public class Vars {
    public Context context;
    public SharedPreferences prefs;
    public SharedPreferences.Editor edit;

    public String macAddress;

    public Gson gson = new Gson();

    public Vars (Context context){
        this.context = context;

        prefs = context.getSharedPreferences(Globals.MPOCK_PREFS, 0);

        edit = prefs.edit();


        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        macAddress = info.getMacAddress();

     //   setCanteenPin(macAddress);
        System.out.println("macadress is:"+macAddress);
    }


}
