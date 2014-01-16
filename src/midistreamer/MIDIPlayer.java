package midistreamer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public abstract class MIDIPlayer extends Thread {
	
    protected Synthesizer sintetizator;
    protected MidiChannel kanal;
    protected BlockingQueue<Ton> zapis;
	
    MIDIPlayer(int playerChannel, int playerProgram) {

        this.zapis=new LinkedBlockingQueue(); // Ustvari izravnalnik
        
        try {
            this.sintetizator = MidiSystem.getSynthesizer(); // Ustvari napravo MIDI (glej Vajo 3)
            this.sintetizator.open(); // Odpri napravo MIDI
        } catch (MidiUnavailableException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        this.kanal = this.sintetizator.getChannels()[playerChannel]; // Doloci kanal MIID
        
        kanal.programChange(playerProgram); // Doloci glasbilo http://en.wikipedia.org/wiki/General_MIDI
    }
    
    /**
     * Abstraktna metoda sprejemnika
     * 
     * To metodo je potrebno reimplementirati, tako da vsebuje kodo:
     * 1. za nalaganje zvočnega zapisa in 
     * 2. in polnjene v izravnalnik (npr. super.zapis.put(newTon))
     */
    public abstract void rcv();
    
    /**
     * Program predvajalnika MIDI
     * 
     * Predvaja zvočni zapis iz izravnalnika.
     */
    public void run() {

        Ton t = null;
        
        this.rcv(); //Naloži celotni zvočni zapis
        
        System.out.println("PLAYER: START");
        
        /**
         * PREDVAJAJ: Glej rešitve vaja 03
         */
        while(true) {            
            
            t = this.zapis.poll(); // Naloži najnovejši Ton
            if(t != null){
				try {
					if(t.shutOffPlayer){
						System.exit(0);
					}
					if(t.noteOn == 1){
				    	kanal.programChange(t.instrument);
				        kanal.noteOn(t.visina,t.jakost); // Začni predavajati
					}else if(t.noteOn == 2){
				        kanal.noteOff(t.visina,t.jakost); // Ustavi predavajati
					}else{
				    	kanal.programChange(t.instrument);
				        kanal.noteOn(t.visina,t.jakost); // Predvajaj in ustavi
				        Thread.sleep(t.trajanje);
					    kanal.noteOff(t.visina,t.jakost); // Konačj s predvajanjem
					}
				} catch (InterruptedException e) {
				    e.printStackTrace();
				    System.exit(1);
				} finally {
				}
            }
        }
        
    }
    
}
