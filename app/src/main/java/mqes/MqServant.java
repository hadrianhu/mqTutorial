package mqes;

import android.util.Log;

import Mpock.Globals;
import Mpock.Vars;


/**
 * Created by ivan on 6/15/2015.
 */
public class MqServant {

    Boolean log = true;

    public MqServant(Vars vars){
        log("+++++++++++++:MqServant");

    }


    void log(String msg) {
        if (log && Globals.log) {
            Log.v(this.getClass().getSimpleName(), msg);
        }
    }
}
