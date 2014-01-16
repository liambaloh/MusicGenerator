package midistreamer;

public class MIDIKeyboardStreamPlayer {
    
    public static int DEFAULT_RECEIVER_PORT = 3700;
    
    public static void main(String[] args) {
        
        MIDIStreamPlayer msp = new MIDIStreamPlayer(0, 1, DEFAULT_RECEIVER_PORT);
        msp.start();
    }
}
