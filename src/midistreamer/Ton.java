package midistreamer;

import java.io.Serializable;

public class Ton implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public Integer visina;
    public Integer trajanje;
    public Integer jakost;
    public Integer barva;
    public Integer instrument;
    public Boolean shutOffPlayer = false;
    public Integer noteOn = 0;	//0 = nudefined, 1 = noteon, 2 = noteoff
    
    public Ton(Integer visina, Integer trajanje, Integer jakost, Integer barva, Integer instrument) {
        this.visina = visina;
        this.trajanje = trajanje;
        this.jakost = jakost;
        this.barva = barva;
        this.instrument = instrument;
    }
    
    public Ton(Integer visina, Integer trajanje, Integer jakost, Integer barva, Integer instrument, Integer noteOn) {
        this.visina = visina;
        this.trajanje = trajanje;
        this.jakost = jakost;
        this.barva = barva;
        this.instrument = instrument;
        this.noteOn = noteOn;
    }
    
    public Ton(Boolean turnOff) {
        this.shutOffPlayer = true;
    }
    
}
