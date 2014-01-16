package midistreamer;

public class WorkerThread extends Thread {
    
    Callable t;
    
    public WorkerThread(Callable t) {
        this.t=t;
    }
    
    @Override
    public void run() {
        t.callback();
    }
}
