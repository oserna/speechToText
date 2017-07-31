package sdk;

import com.aldebaran.qi.CallError;
import com.aldebaran.qi.helper.EventCallback;
import com.aldebaran.qi.helper.proxies.ALMemory;
import com.aldebaran.qi.helper.proxies.ALSpeechRecognition;

import java.util.List;

public class EventCallbackProcessor implements EventCallback<List> {

    private final ALMemory memory;
    private final ALSpeechRecognition sr;

    public EventCallbackProcessor(ALMemory memory, ALSpeechRecognition sr) throws Exception {
        this.memory = memory;
        this.sr = sr;
    }

    @Override
    public void onEvent(List list) throws InterruptedException, CallError {
        //System.out.println(o.toString() + "::" + o.getClass().getName() + ": " + Thread.currentThread().getName());
        System.out.println(list.get(0).getClass().getName() + ": " + list.get(0));
        System.out.println(list.get(1).getClass().getName() + ": " + list.get(1));
        Float f = (Float) list.get(1);
        if(f.floatValue() > 0.3) {
            memory.raiseEvent("CodeWord", "welcome");
            sr.pause(true);
        }
        else{
            System.out.println("Not enough confidence: " + f.toString());
        }
    }
}

