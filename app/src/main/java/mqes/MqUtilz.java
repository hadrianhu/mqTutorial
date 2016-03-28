package mqes;

import android.util.Log;

import Mpock.Globals;


public class MqUtilz {

    static boolean log = true;
	
	static void  log(String msg){
        if(log && Globals.log){
            Log.v("MQ UTILZ", msg);
        }

	}

		
		// GET THE NOTIFIER STATE
		public	static String getState(int state) {
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

}
