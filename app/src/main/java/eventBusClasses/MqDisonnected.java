package eventBusClasses;

import android.util.Log;

import Mpock.Globals;


/**
 * Created by Mac on 2015/12/06.
 */
public class MqDisonnected {

    public boolean log = true;

    public MqDisonnected(String sm){
        log("++++++++:MqDisonnected created ");
      //  log("PubMessage sentMessage recep:"+sm.recep);
       // this.sm = sm;

    }

    void log(String msg) {
        if (log && Globals.log) {
            Log.v(this.getClass().getSimpleName(), msg);
        }
    }
}
