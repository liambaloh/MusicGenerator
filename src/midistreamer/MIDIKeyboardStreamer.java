package midistreamer;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

public class MIDIKeyboardStreamer extends Applet implements ActionListener, KeyListener {
    
    public int DEFAULT_TRANSMITTER_PORT = 3400;
    public int DEFAULT_RECEIVER_PORT = 3700;
    public String DEFAULT_RECEIVER_ADDRESS = "127.0.0.1";
    
    protected int receiverPort;
    protected int transmitterPort;
    protected String recieverAddress;
    protected DatagramSocket tsocket;
    protected byte[] sndBuffer;
    protected DatagramPacket packet;
    protected ByteArrayOutputStream bos;
    protected ObjectOutputStream out;
    
    Random randomgen = new Random();
    
    protected int lastInstrument = 1;
    protected int lastInstrumentIndex = 1;
    
    TextArea displayArea;
    TextField typingArea;
      
    @Override
    public void init() {
        
        Button button = new Button("Clear");
        button.addActionListener(this);

        typingArea = new TextField(20);
        typingArea.addKeyListener(this);

        displayArea = new TextArea(5, 20);
        displayArea.setEditable(false);

        setLayout(new BorderLayout());
        add("Center", displayArea);
        add("North", typingArea);
        add("South", button);
        
        try {
            tsocket = new DatagramSocket(DEFAULT_TRANSMITTER_PORT);
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        initialize();
    }
    
    public void snd(int keyCode, boolean noteOn) {
        System.out.println("TRANSMITTER: START");
        
        /*
        lastInstrumentIndex++;
        if(lastInstrumentIndex > 100){
        	lastInstrumentIndex = 0;
        }
        lastInstrument = lastInstrumentIndex;*/
        
        try {                
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            
            Ton t;
            System.out.println("keycode:"+keyCode);
        	if(noteOn){
            	t = new Ton(keyCode,500,100,6, lastInstrument, 1);
                out.writeObject(t);
        	}else{
            	t = new Ton(keyCode,500,100,6, lastInstrument, 2);
                out.writeObject(t);
        	}
            
            byte[] sndBuffer = bos.toByteArray();

            packet = new DatagramPacket(sndBuffer, sndBuffer.length, InetAddress.getByName(DEFAULT_RECEIVER_ADDRESS), DEFAULT_RECEIVER_PORT);
            tsocket.send(packet);

            out.close();
            bos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("TRANSMITTER: END");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //Clear the text components.
        displayArea.setText("");
        typingArea.setText("");

        //Return the focus to the typing area.
        typingArea.requestFocus();
    }
    
    Map<Integer,Boolean> keystate = new HashMap();
    
    public boolean keyDown(int keyVal){
    	if(keystate.containsKey(keyVal) && keystate.get(keyVal)){
    		return true;
    	}
    	return false;
    }
    
    /** Handle the key typed event from the text field. */
    public void keyTyped(KeyEvent e) {
        char keyChar = e.getKeyChar();
    	int keyVal = Character.getNumericValue(keyChar)+50;

    	//Select chord type with 0 - 9
    	if(keyChar >= '0' && keyChar <= '9'){
    		CHORD_SELECTED = keyChar - 48;
    		return;
    	}

    	//Simple generation with 'q'. Uses simple markov model
    	if(keyChar == 'q'){
    		generateSequence();
    		playSequence();
			return;
    	}
    	
    	//Phrase generation with 'w'. Uses complex markov model
    	if(keyChar == 'w'){
    		generatePhrase();
    		playSequence();
			return;
    	}
    	
    	//Play note/chord
    	if(!keyDown(keyVal)){
	        this.sendChord(keyVal, true, CHORD_SELECTED, 0);
	        this.displayArea.append(String.valueOf(keyChar));
	        keystate.put(keyVal, true);
    	}
    	
    }

    /** Handle the key pressed event from the text field. */
    public void keyPressed(KeyEvent e) {
    	
    }

    /** Handle the key released event from the text field. */
    public void keyReleased(KeyEvent e) {
        char keyChar = e.getKeyChar();
    	int keyVal = Character.getNumericValue(keyChar)+50;
        this.sendChord(keyVal, false, CHORD_SELECTED, 0);
        keystate.put(keyVal, false);
        this.displayArea.append(String.valueOf(keyChar));
    }

    public static int CHORD_SELECTED = 0;				//Izbran
    public static final int CHORD_SINGLE = 0;			//Ena nota
    public static final int CHORD_KVINTAKORD_V = 1;		//Vliki kvintakord
    public static final int CHORD_KVINTAKORD_M = 2;		//Mali kvintakord
    public static final int CHORD_KVINTAKORD_ZM = 3;	//Zmanjšani kvintakord
    public static final int CHORD_KVINTAKORD_ZV = 4;	//Zveèani kvintakord
    public static final int CHORD_SEPTAKORD_V = 5;		//Veliki septakord
    public static final int CHORD_SEPTAKORD_M = 6;		//Mali septakord
    public static final int CHORD_SEPTAKORD_DOM = 7;	//Dominantni septakord
    public static final int CHORD_SEPTAKORD_PZM = 8;	//Pol-zmanjšani septakord
    public static final int CHORD_SEPTAKORD_ZM = 9;		//Zmanjšani septakord
    
    public static final int OCTAVE = 12;				//Notes in an octave C3 + OCTAVE -> C4

    public static final int C0 = 12;
    public static String[] noteNames = new String[]{"C","Cs","D","Ds","E","F","Fs","G","Gs","A","As","H"};
    
    /*
    public static final int C4 = 60;
    public static final int Cs4 = 61;
    public static final int D4 = 62;
    public static final int Ds4 = 63;
    public static final int E4 = 64;
    public static final int F4 = 65;
    public static final int Fs4 = 66;
    public static final int G4 = 67;
    public static final int Gs4 = 68;
    public static final int A4 = 69;
    public static final int As4 = 70;
    public static final int B4 = 72;
    public static final int C5 = 73;
    */

    public static Hashtable<String,Integer> notes = new Hashtable<String,Integer>();
    public static Hashtable<Integer,String> id2note = new Hashtable<Integer,String>();
    
    public static Hashtable<Integer,Float[]> chordStartToneProbabilities = new Hashtable<Integer,Float[]>();
    public static Hashtable<Integer,Integer[]> chordToneIntensity = new Hashtable<Integer,Integer[]>();
    public static Hashtable<String,String[]> appropriateHomeTones = new Hashtable<String,String[]>();

    public ArrayList<int[]> allowedProgression = new ArrayList<int[]>();
    public ArrayList<float[]> allowedProgressionProb = new ArrayList<float[]>();
    
    public Hashtable<Integer,MidiMessage[]> messageSequence = new Hashtable<Integer, MidiMessage[]>();
    public Integer currentPosition = 0;		//Current position in the sequence
    public Integer sequenceLength = 64;		//Sequence length
    public Integer sixteenthDelay = 300;	//Delay between frames
    
    //Adds noteon and noteoff messages to the message sequence.
    public void putNote(String name, int position, int length){

    	if(messageSequence.containsKey(position)){
    		MidiMessage[] midiMessages = messageSequence.get(position);
    		MidiMessage[] newMM = Arrays.copyOf(midiMessages, midiMessages.length + 1);
    		newMM[newMM.length - 1] = onmm(name);
        	messageSequence.put(position, newMM);
    	}else{
    		MidiMessage[] midiMessages = new MidiMessage[]{ onmm(name)};
        	messageSequence.put(position, midiMessages);
    	}
    	
    	int endpos = position + length;
    	if(messageSequence.containsKey(endpos)){
    		MidiMessage[] midiMessages = messageSequence.get(endpos);
    		MidiMessage[] newMM = Arrays.copyOf(midiMessages, midiMessages.length + 1);
    		for (int i = (newMM.length - 2); i >= 0; i--) {                
    			newMM[i+1] = newMM[i];
    		}
    		newMM[0] = offmm(name);	//Sort noteOff events to the start of the frame's events
        	messageSequence.put(endpos, newMM);
    	}else{
    		MidiMessage[] midiMessages = new MidiMessage[]{ offmm(name)};
        	messageSequence.put(endpos, midiMessages);
    	}
    }
    
    //Hand split off point
    public static final int MAX_LEFT_HAND_NOTE = 55; //G3

    public static final int SIXTEENTH = 1;
    public static final int EIGTH = 2;
    public static final int QUARTER = 4;
    public static final int DOTTEDQUARTER = 6;
    public static final int HALFTONE = 8;
    public static final int WHOLETONE = 16;
    
    public int currentChord = 1;
    public String[] scale = new String[]{"0","C","D","E","F","G","A","H"};	//The "0" is there because chords operate on a 1 - 7 scale
    public ArrayList<String> chordProgression = new ArrayList<String>(); 
    
    //Get the appropriate third for the scale (minor, major,...)
    public String getThird(String tone){
    	int thisID = 0;
    	for(int i = 0; i < scale.length; i++){
    		String s = scale[i];
    		if(s.equals(tone)){
    			if( (i + 2) < scale.length ){
    				return scale[i+2];
    			}else{
    				return scale[(i+2) - (scale.length - 1)];	//The -1 is because of the "0" at id 0 in scale
    			}
    		}
    	}
    	return "0";
    }

    //Get the appropriate fifth for the scale (minor, major,...)
    public String getFifth(String tone){
    	int thisID = 0;
    	for(int i = 0; i < scale.length; i++){
    		String s = scale[i];
    		if(s.equals(tone)){
    			if( (i + 4) < scale.length ){
    				return scale[i+4];
    			}else{
    				return scale[(i+4) - (scale.length - 1)];	//The -1 is because of the "0" at id 0 in scale
    			}
    		}
    	}
    	return "0";
    }
    
    public void generateChordProgression(){
    	
    	/* Testing thirds
    	for(String s : scale){
    		System.out.println(s +" -> "+ getThird(s));
    	}*/
    	
    	chordProgression.add(scale[currentChord]);	//Start on the tonic!
		
    	for(int i = 0; i < 14; i++){
    		int[] allowedNextChords = allowedProgression.get(currentChord);
    		float[] allowedNextChordsProb = allowedProgressionProb.get(currentChord);
    		System.out.println("lengths = "+allowedNextChords.length+"; "+allowedNextChordsProb.length);
    		float rndnum = Math.abs(randomgen.nextFloat());	//Generate a random number between 0 and 1, check where it lands
    		float probsum = 0;
    		int nextChordId = 0;
    		for(float prob : allowedNextChordsProb){
    			probsum += prob;
    			System.out.println("is "+rndnum+" <= "+probsum);
    			if(rndnum <= probsum){
    				break;
    			}
				nextChordId++;
    		}
    		
    		int nextChord = allowedNextChords[nextChordId];
    		System.out.println(nextChord);
    		chordProgression.add(scale[nextChord]);
    		currentChord = nextChord;
    	}
    	
    }
    
    public int[] generateOpeningPhraseChordProgression(){
    	int[] r = new int[]{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};

    	//TRDITEV
    	
    	//Prvi takt
    	
    	//Prvi dve dobi
    	//Start on tonic
    	r[0] = 1;
    	r[1] = 1;

    	//Drugi dve dobi
    	int rnd = Math.abs(randomgen.nextInt()) % 100;
    	int cplx1 = Math.abs(randomgen.nextInt()) % 100;
    	int forceChord3 = 0;	//Used to allow for decorations - so decorations make sense.
    	
    	if(rnd < 60){
    		//Go to IV
        	r[2] = 4;
        	if(cplx1 < 95){
        		r[3] = 4;
        	}else{
        		//Decoration!
        		//Only IV -> ii -> vii is appropriate
    			r[3] = 2;
    			forceChord3 = 7;
        	}
    	}else if(rnd < 90){
    		//Go to V
        	r[2] = 5;
        	if(cplx1 < 95){
        		r[3] = 5;
        	}else{
        		//Decoration!
        		//Only V -> vi -> IV is appropriate
    			r[3] = 6;
    			forceChord3 = 4;
        	}
    	}else{
    		//Stay on I
        	r[2] = 1;
        	if(cplx1 < 89){
        		r[3] = 1;
        	}else{
        		//Decoration!
        		if(cplx1 < 90){
	        		//I -> vi -> IV is appropriate
	    			r[3] = 7;
	    			forceChord3 = 5;
        		}else if(cplx1 < 91){
	        		//I -> vi -> ii is appropriate
	    			r[3] = 6;
	    			forceChord3 = 2;
        		}else if(cplx1 < 92){
	        		//I -> vi -> IV is appropriate
	    			r[3] = 6;
	    			forceChord3 = 4;
        		}else if(cplx1 < 93){
	        		//I -> ii -> V is appropriate
	    			r[3] = 6;
	    			forceChord3 = 5;
        		}else if(cplx1 < 94){
	        		//I -> VI -> vi is appropriate
	    			r[3] = 5;
	    			forceChord3 = 6;
        		}else if(cplx1 < 95){
	        		//I -> VI -> IV is appropriate
	    			r[3] = 5;
	    			forceChord3 = 4;
        		}else if(cplx1 < 96){
	        		//I -> ii -> V is appropriate
	    			r[3] = 2;
	    			forceChord3 = 4;
        		}else if(cplx1 < 97){
	        		//I -> ii -> vii° is appropriate
	    			r[3] = 2;
	    			forceChord3 = 7;
        		}else if(cplx1 < 98){
	        		//I -> IV -> ii is appropriate
	    			r[3] = 4;
	    			forceChord3 = 2;
        		}else if(cplx1 < 99){
	        		//I -> IV -> VI is appropriate
	    			r[3] = 4;
	    			forceChord3 = 5;
        		}else if(cplx1 < 100){
	        		//I -> IV -> vii° is appropriate
	    			r[3] = 4;
	    			forceChord3 = 7;
        		} 
        	}
    	}
    	
    	//Drugi takt
    	
    	//tretji dve dobi
    	rnd = Math.abs(randomgen.nextInt()) % 100;
    	if(forceChord3 != 0){
    		r[4] = forceChord3;
    		r[5] = forceChord3;
    	}else{
    		if(r[2] == 1){
    			if(rnd < 10){
    	    		r[4] = 2;
    	    		r[5] = 2;
    			}else if(rnd < 60){
    	    		r[4] = 4;
    	    		r[5] = 4;
    			}else if(rnd < 80){
    	    		r[4] = 5;
    	    		r[5] = 5;
    			}else if(rnd < 90){
    	    		r[4] = 6;
    	    		r[5] = 6;
    			}else{
    	    		r[4] = 7;
    	    		r[5] = 7;
    			} 
    		}else if(r[2] == 4){
    			if(rnd < 30){
    	    		r[4] = 2;
    	    		r[5] = 2;
    			}else if(rnd < 70){
    	    		r[4] = 5;
    	    		r[5] = 5;
    			}else{
    	    		r[4] = 7;
    	    		r[5] = 7;
    	    	}
    		}else if(r[2] == 5){
    			if(rnd < 30){
    	    		r[4] = 4;
    	    		r[5] = 4;
    			}else{
    	    		r[4] = 6;
    	    		r[5] = 6;
    	    	}
    		} 
    	}
    	
    	//Cetri dve dobi
    	//End with either IV, V or vii°
    	rnd = Math.abs(randomgen.nextInt()) % 100;
    	switch(r[4]){
    	case 2:
    		if(rnd < 80){
	    		r[6] = 7;
	    		r[7] = 7;
    		}else{
	    		r[6] = 5;
	    		r[7] = 5;
    		}
    		break;
    	case 4:
    		if(rnd < 50){
	    		r[6] = 4;
	    		r[7] = 4;
    		}else if(rnd < 70){
	    		r[6] = 5;
	    		r[7] = 5;
    		}else{
	    		r[6] = 7;
	    		r[7] = 7;
    		}
    		break;
    	case 5:
    		if(rnd < 60){
	    		r[6] = 4;
	    		r[7] = 4;
    		}else{
	    		r[6] = 5;
	    		r[7] = 5;
    		}
    		break;
    	case 6:
    		if(rnd < 60){
	    		r[6] = 4;
	    		r[7] = 4;
    		}else{
	    		r[6] = 5;
	    		r[7] = 5;
    		}
    		break;
    	case 7:
    		if(rnd < 80){
	    		r[6] = 5;
	    		r[7] = 5;
    		}else{
	    		r[6] = 7;
	    		r[7] = 7;
    		}
    		break;
    	}
    	
    	//Tretji takt
    	
    	//Peti dve dobi
    	//Start on tonic
    	r[8] = 1;
    	r[9] = 1;

    	//Sesti dve dobi (I -> I/IV) - Not V because it ends in a half cadence, which ends on a V.
    	rnd = Math.abs(randomgen.nextInt()) % 100;
    	cplx1 = Math.abs(randomgen.nextInt()) % 100;
    	forceChord3 = 0;	//Used to allow for decorations - so decorations make sense.
    	
    	if(rnd < 70){
    		//Go to IV
        	r[10] = 4;
        	if(cplx1 < 95){
        		r[11] = 4;
        	}else{
        		//Decoration!
        		//Only IV -> ii -> vii is appropriate
    			r[11] = 2;
    			forceChord3 = 7;
        	}
    	}else{
    		//Stay on I
        	r[10] = 1;
        	if(cplx1 < 89){
        		r[11] = 1;
        	}else{
        		//Decoration!
        		if(cplx1 < 90){
	        		//I -> vi -> IV is appropriate
	    			r[11] = 7;
	    			forceChord3 = 5;
        		}else if(cplx1 < 91){
	        		//I -> vi -> ii is appropriate
	    			r[11] = 6;
	    			forceChord3 = 2;
        		}else if(cplx1 < 92){
	        		//I -> vi -> IV is appropriate
	    			r[11] = 6;
	    			forceChord3 = 4;
        		}else if(cplx1 < 93){
	        		//I -> ii -> V is appropriate
	    			r[11] = 6;
	    			forceChord3 = 5;
        		}else if(cplx1 < 94){
	        		//I -> VI -> vi is appropriate
	    			r[11] = 5;
	    			forceChord3 = 6;
        		}else if(cplx1 < 95){
	        		//I -> VI -> IV is appropriate
	    			r[11] = 5;
	    			forceChord3 = 4;
        		}else if(cplx1 < 96){
	        		//I -> ii -> V is appropriate
	    			r[11] = 2;
	    			forceChord3 = 4;
        		}else if(cplx1 < 97){
	        		//I -> ii -> vii° is appropriate
	    			r[11] = 2;
	    			forceChord3 = 7;
        		}else if(cplx1 < 98){
	        		//I -> IV -> ii is appropriate
	    			r[11] = 4;
	    			forceChord3 = 2;
        		}else if(cplx1 < 99){
	        		//I -> IV -> VI is appropriate
	    			r[11] = 4;
	    			forceChord3 = 5;
        		}else{
	        		//I -> IV -> vii° is appropriate
	    			r[11] = 4;
	    			forceChord3 = 7;
        		} 
        	}
    	}
    	
    	//Cetrti takt (polovicna kadenca ii/IV/vi -> ii/IV/vi/Vii° -> V)
    	//Sedmi dve dobi - kadenca
    	rnd = Math.abs(randomgen.nextInt()) % 100;
    	if(forceChord3 != 0){
    		r[12] = forceChord3;
    	}else{
    		switch(r[11]){
    		case 1:
    			// I -> ii/IV/vi -> ? -> V (cannot go I -> vii°, because vii° doesn't lead into anything that leads into V)
    			if(rnd < 30){
    				r[12] = 2;
    			}else if(rnd < 50){
    				r[12] = 4;
    			}else{
    				r[12] = 6;
    			}
    			break;
    		case 4:
    			// IV -> ii -> ? -> V (Cannot lead into vii°, because vii° doesn't lead into anything that leads into V)
    			r[12] = 2;
    			break;
    		}
    	}

    	rnd = Math.abs(randomgen.nextInt()) % 100;
		switch(r[12]){
		case 2:
			// ii -> vii° -> V
			r[13] = 7;
			break;
		case 4:
			// IV -> I/ii/vi/Vii° -> V
			if(rnd < 40){
				r[13] = 1;
			}else if(rnd < 60){
				r[13] = 2;
			}else if(rnd < 80){
				r[13] = 6;
			}else{
				r[13] = 7;
			}
			break;
		case 6:
			// ii/IV/vi -> IV/ii -> V
			if(rnd < 80){
				r[13] = 4;
			}else{
				r[13] = 2;
			}
			break;
		}
    	
		//Osmi dve dobi (konec kadence, V)
		r[14] = 5;
		r[15] = 5;
		
		//KONEC TRDITVE
		//ODGOVOR
		
		//Peti takt (ponovitev prvega takta)
		//Deveti dve dobi
		r[16] = r[0];
		r[17] = r[1];
		//Deseti dve dobi
		r[18] = r[2];
		r[19] = r[3];
		
		//Sesti takt (ponovitev drugega takta)
		//Enajsti dve dobi
		r[20] = r[4];
		r[21] = r[5];
		//Dvanajsti dve dobi
		r[22] = r[6];
		r[23] = r[7];
		
		//Sedmi takt
    	
    	//Trinajsti dve dobi
    	//Start on tonic
    	r[24] = 1;
    	r[25] = 1;

    	//Stirinajsti dve dobi
    	rnd = Math.abs(randomgen.nextInt()) % 100;
    	cplx1 = Math.abs(randomgen.nextInt()) % 100;
    	forceChord3 = 0;	//Used to allow for decorations - so decorations make sense.
    	
    	if(rnd < 60){
    		//Go to IV
        	r[26] = 4;
        	if(cplx1 < 95){
        		r[27] = 4;
        	}else{
        		//Decoration!
        		//Only IV -> ii -> vii is appropriate
    			r[27] = 2;
    			forceChord3 = 7;
        	}
    	}else{
    		//Stay on I
        	r[26] = 1;
        	if(cplx1 < 89){
        		r[27] = 1;
        	}else{
        		//Decoration! (V not allowed)
        		if(cplx1 < 91){
	        		//I -> vi -> ii is appropriate
	    			r[27] = 6;
	    			forceChord3 = 2;
        		}else if(cplx1 < 92){
	        		//I -> vi -> IV is appropriate
	    			r[27] = 6;
	    			forceChord3 = 4;
        		}else if(cplx1 < 94){
	        		//I -> VI -> vi is appropriate
	    			r[27] = 5;
	    			forceChord3 = 6;
        		}else if(cplx1 < 95){
	        		//I -> VI -> IV is appropriate
	    			r[27] = 5;
	    			forceChord3 = 4;
        		}else if(cplx1 < 96){
	        		//I -> ii -> V is appropriate
	    			r[27] = 2;
	    			forceChord3 = 4;
        		}else if(cplx1 < 97){
	        		//I -> ii -> vii° is appropriate
	    			r[27] = 2;
	    			forceChord3 = 7;
        		}else if(cplx1 < 98){
	        		//I -> IV -> ii is appropriate
	    			r[27] = 4;
	    			forceChord3 = 2;
        		}else if(cplx1 < 100){
	        		//I -> IV -> vii° is appropriate
	    			r[27] = 4;
	    			forceChord3 = 7;
        		} 
        	}
    	}
		
		//Osmi takt (popolna kadenca ii/IV/vi/vii° -> IV/V/Vii° -> I)
    	//Petnajsti dve dobi - kadenca
    	rnd = Math.abs(randomgen.nextInt()) % 100;
		switch(r[27]){
		case 1:
			// I -> ii/IV/vi/vii° -> ? -> I
			if(rnd < 30){
				r[28] = 2;
			}else if(rnd < 50){
				r[28] = 4;
			}else if(rnd < 80){
				r[28] = 6;
			}else{
				r[28] = 7;
			}
			break;
		case 4:
			// I -> ii/vii° -> ? -> I
			if(rnd < 70){
				r[28] = 2;
			}else{
				r[28] = 7;
			}
			break;
		case 5:
			// I -> vi -> IV -> I (vi must go into VI, as it's the only direct connection that goes to I)
			r[28] = 6;
			break;
		}
		
    	rnd = Math.abs(randomgen.nextInt()) % 100;
		switch(r[28]){
		case 2:
			// I -> ii/IV/vi/vii° -> ? -> I
			if(rnd < 40){
				r[29] = 4;
			}else if(rnd < 60){
				r[29] = 5;
			}else{
				r[29] = 7;
			}
			break;
		case 4:
			// I -> ii/vii° -> ? -> I
			if(rnd < 70){
				r[29] = 7;
			}else{
				r[29] = 5;
			}
			break;
		case 6:
			// I -> vi -> IV -> I (vi must go into VI, as it's the only direct connection that goes to I)
			r[29] = 4;
			break;
		case 7:
			// I -> vi -> IV -> I (vi must go into VI, as it's the only direct connection that goes to I)
			r[29] = 5;
			break;
		}
		
		//Stirinajsti dve dobi. Konec kadence, I
		r[30] = 1;
		r[31] = 1;
		
		//KONEC ODGOVORA
		
    	return r;
    }
    
    public int getRndChoiceId(float number, Float[] startToneProbabilities){
    	int r = 0;
    	float sum = 0;
    	for(int i = 0; i < startToneProbabilities.length; i++){
    		sum += startToneProbabilities[i];
    		if(sum < number){
    			continue;
    		}
    		return i;
    	}
    	return r;
    }
    
    public int[] generateMelody(int[] chordProgression){
    	
    	int position = 0;
    	int startingTone = chordProgression[0];
    	int tone = 0;
    	int toneArchive = 0;
    	int[] sequence = new int[chordProgression.length * QUARTER];
    	
    	for(int i = 0; i < chordProgression.length; i++){
    		if(i == 14){	//Half cadence first half
    			sequence[i*QUARTER] = 5;
        		System.out.println("CPY of ["+(i*QUARTER)+"] = "+sequence[i*QUARTER]);
    			sequence[i*QUARTER+1] = 5;
        		System.out.println("CPY of ["+(i*QUARTER+1)+"] = "+sequence[i*QUARTER+1]);
    			sequence[i*QUARTER+2] = 5;
        		System.out.println("CPY of ["+(i*QUARTER+2)+"] = "+sequence[i*QUARTER+2]);
    			sequence[i*QUARTER+3] = 5;
        		System.out.println("CPY of ["+(i*QUARTER+3)+"] = "+sequence[i*QUARTER+3]);
    			continue;
    		}
    		if(i == 15){	//Half cadence second half
    			sequence[i*QUARTER] = 5;
        		System.out.println("CPY of ["+(i*QUARTER)+"] = "+sequence[i*QUARTER]);
    			sequence[i*QUARTER+1] = 5;
        		System.out.println("CPY of ["+(i*QUARTER+1)+"] = "+sequence[i*QUARTER+1]);
    			sequence[i*QUARTER+2] = 0;
        		System.out.println("CPY of ["+(i*QUARTER+2)+"] = "+sequence[i*QUARTER+2]);
    			sequence[i*QUARTER+3] = 0;
        		System.out.println("CPY of ["+(i*QUARTER+3)+"] = "+sequence[i*QUARTER+3]);
    			continue;
    		}

    		if(i == 30){	//Perfect cadence first half
    			sequence[i*QUARTER] = 1;
        		System.out.println("CPY of ["+(i*QUARTER)+"] = "+sequence[i*QUARTER]);
    			sequence[i*QUARTER+1] = 1;
        		System.out.println("CPY of ["+(i*QUARTER+1)+"] = "+sequence[i*QUARTER+1]);
    			sequence[i*QUARTER+2] = 1;
        		System.out.println("CPY of ["+(i*QUARTER+2)+"] = "+sequence[i*QUARTER+2]);
    			sequence[i*QUARTER+3] = 1;
        		System.out.println("CPY of ["+(i*QUARTER+3)+"] = "+sequence[i*QUARTER+3]);
    			continue;
    		}
    		if(i == 31){	//Perfect cadence second half
    			sequence[i*QUARTER] = 1;
        		System.out.println("CPY of ["+(i*QUARTER)+"] = "+sequence[i*QUARTER]);
    			sequence[i*QUARTER+1] = 1;
        		System.out.println("CPY of ["+(i*QUARTER+1)+"] = "+sequence[i*QUARTER+1]);
    			sequence[i*QUARTER+2] = 0;
        		System.out.println("CPY of ["+(i*QUARTER+2)+"] = "+sequence[i*QUARTER+2]);
    			sequence[i*QUARTER+3] = 0;
        		System.out.println("CPY of ["+(i*QUARTER+3)+"] = "+sequence[i*QUARTER+3]);
    			continue;
    		}
    		
    		//Phrase repeat at start of second half
    		if(i >= 16 && i < 24){
    			sequence[i*QUARTER] = sequence[(i*QUARTER) - (16*QUARTER)];
        		System.out.println("CPY of ["+(i*QUARTER)+"] = "+sequence[i*QUARTER]);
    			sequence[i*QUARTER+1] = sequence[(i*QUARTER) - (16*QUARTER)+1];
        		System.out.println("CPY of ["+(i*QUARTER+1)+"] = "+sequence[i*QUARTER+1]);
    			sequence[i*QUARTER+2] = sequence[(i*QUARTER) - (16*QUARTER)+2];
        		System.out.println("CPY of ["+(i*QUARTER+2)+"] = "+sequence[i*QUARTER+2]);
    			sequence[i*QUARTER+3] = sequence[(i*QUARTER) - (16*QUARTER)+3];
        		System.out.println("CPY of ["+(i*QUARTER+3)+"] = "+sequence[i*QUARTER+3]);
    			continue;
    		}
    		
        	Float[] startToneProbabilities = chordStartToneProbabilities.get(chordProgression[i]);
        	float rnd = randomgen.nextFloat();
        	//Generate first tone from probabilities
        	tone = chordProgression[i] + getRndChoiceId(rnd, startToneProbabilities);
    		if(tone < 1){
    			tone += 7;
    		}
    		if(tone > 7){
    			tone -= 7;
    		}
    		sequence[i*4] = tone;
    		System.out.println("Cookie* of ["+(i*4)+"] = "+tone);
    		for(int j = 1; j < 4; j++){	//Generate next three tones
    			rnd = randomgen.nextFloat();
    			
    			if(rnd < 0.1f){
    				//Do nothing - stay on same note
    			}else if(rnd < 0.6f){
    				tone -= 1;
    			}else{
    				tone += 1;
    			}

        		//Normalize to scale
        		if(tone < 1){
        			tone += 7;
        		}
        		if(tone > 7){
        			tone -= 7;
        		}
        		
        		sequence[i*4 + j] = tone;
        		System.out.println("Cookie of ["+(i*4 + j)+"] = "+tone);
    		}
    		toneArchive = tone;
    	}

    	//Last two will be tonic
    	sequence[sequence.length-1] = 1;
    	sequence[sequence.length-2] = 1;
    	
    	return sequence;
    }
    
    public void generatePhrase(){
    	messageSequence = new Hashtable<Integer, MidiMessage[]>();
    	int[] chords = generateOpeningPhraseChordProgression();

    	//Apply chord progression
    	int chordNum = 0;
    	boolean skip = false;	//Used to skip two same in a row
    	for(int i = 0; i < chords.length; i++){
    		if(skip){
    			skip = false;
    			continue;
    		}
    		int chord = chords[i];
    		if( (i < (chords.length-1)) && (chords[i] == chords[i+1]) ){
        		String chordS = scale[chord];
        		putNote(chordS+"3", chordNum * QUARTER, HALFTONE);
        		putNote(getThird(chordS)+"3", chordNum * QUARTER, HALFTONE);
        		putNote(getFifth(chordS)+"3", chordNum * QUARTER, HALFTONE);
        		sequenceLength = chordNum * QUARTER + (2*HALFTONE);
        		chordNum += 2;
        		skip = true;
    		}else{
        		String chordS = scale[chord];
        		putNote(chordS+"3", chordNum * QUARTER, QUARTER);
        		putNote(getThird(chordS)+"3", chordNum * QUARTER, QUARTER);
        		putNote(getFifth(chordS)+"3", chordNum * QUARTER, QUARTER);
        		sequenceLength = chordNum * QUARTER + (2*QUARTER);
        		chordNum++;
    		}
    		//System.out.println ("C = "+chord);
    	}
    	
    	int[] melodySequence = generateMelody(chords);
    	int currentScale = 4;
    	int toneArchive = melodySequence[0];
    	int skipNum = 0;
    	
    	for(int i = 0; i < melodySequence.length; i++){
    		if(i == 120){
        		String toneS = scale[1];
    			putNote(toneS+""+currentScale, i * SIXTEENTH, DOTTEDQUARTER);
    			skipNum = WHOLETONE;
    			continue;
    		}
    		if(i == 56){
        		String toneS = scale[5];
    			putNote(toneS+""+currentScale, i * SIXTEENTH, DOTTEDQUARTER);
    			skipNum = DOTTEDQUARTER;
    			continue;
    		}
    		if(skipNum > 0){
    			skipNum--;
    			continue;
    		}
    		
    		int tone = melodySequence[i];
    		System.out.println("Boo of ["+i+"] = "+tone);
    		if(tone == 0){
    			//Pavza!
    			continue;
    		}
    		String toneS = scale[tone];
    		if(Math.abs(tone - toneArchive) > 4){
    			if (tone > toneArchive){	//Dovolimo lestvice 3 -> 5
    				if(currentScale > 3){
    					currentScale--;
    				}
    			}else{
    				if(currentScale < 4){
    					currentScale++;
    				}
    			}
    		}
    		float rnd = randomgen.nextFloat();
    		if(rnd < 0.6f){
    			putNote(toneS+""+currentScale, i * SIXTEENTH, SIXTEENTH);
    		}else{
    			if((i % 4) < 3){
	    			putNote(toneS+""+currentScale, i * SIXTEENTH, EIGTH);
	    			skipNum = 1;
    			}else{
    				putNote(toneS+""+currentScale, i * SIXTEENTH, SIXTEENTH);
    			}
    		}
    		toneArchive = tone;
    	}
    	
    }
    
    public void generateSequence(){
        Hashtable<Integer,MidiMessage[]> messageSequence = new Hashtable<Integer, MidiMessage[]>();
    	generateChordProgression();
    	
    	//Apply chord progression
    	int chordNum = 0;
    	for(String chord : chordProgression){
    		
    		putNote(chord+"3", chordNum * WHOLETONE, WHOLETONE);
    		putNote(getThird(chord)+"3", chordNum * WHOLETONE, WHOLETONE);
    		putNote(getFifth(chord)+"3", chordNum * WHOLETONE, WHOLETONE);
    		sequenceLength = chordNum * WHOLETONE + (2*WHOLETONE);
    		chordNum++;
    	}
    	
    	//MidiMessage[] midiMessages;

		/*
		putNote("C4", 0, 8);
		putNote("E4", 0, 8);
		putNote("G4", 0, 8);

		putNote("D4", 4, 8);

		putNote("C4", 8, 8);
		putNote("E4", 8, 8);
		putNote("G4", 8, 8);
		
		putNote("D4", 12, 8);

		putNote("C4", 16, 8);
		putNote("E4", 16, 8);
		putNote("G4", 16, 8);
		
		putNote("D4", 20, 8);

		putNote("C4", 24, 8);
		putNote("E4", 24, 8);
		putNote("G4", 24, 8);
		*/

		/*
		putNote("F4", 8, 8);
		putNote("A4", 8, 8);
		putNote("C5", 8, 8);

		putNote("A4", 16, 8);
		putNote("C5", 16, 8);
		putNote("E5", 16, 8);

		putNote("G4", 24, 8);
		putNote("H4", 24, 8);
		putNote("D5", 24, 8);
		*/
		
		/*
		midiMessages = new MidiMessage[]{ onmm("C4"), onmm("E4"), onmm("G4") };
    	messageSequence.put(new Integer(0), midiMessages);
    	
    	midiMessages = new MidiMessage[]{ offmm("C4"), offmm("E4"), offmm("G4") };
    	messageSequence.put(new Integer(7), midiMessages);
    	
		midiMessages = new MidiMessage[]{ onmm("F4"), onmm("A4"), onmm("C5") };
    	messageSequence.put(new Integer(8), midiMessages);
    	
    	midiMessages = new MidiMessage[]{ offmm("F4"), offmm("A4"), offmm("C5") };
    	messageSequence.put(new Integer(15), midiMessages);
    	
		midiMessages = new MidiMessage[]{ onmm("A4"), onmm("C5"), onmm("E5") };
    	messageSequence.put(new Integer(16), midiMessages);
    	
    	midiMessages = new MidiMessage[]{ offmm("A4"), offmm("C5"), offmm("E5") };
    	messageSequence.put(new Integer(23), midiMessages);
    	
		midiMessages = new MidiMessage[]{ onmm("G4"), onmm("H4"), onmm("D5") };
    	messageSequence.put(new Integer(24), midiMessages);
    	
    	midiMessages = new MidiMessage[]{ offmm("G4"), offmm("H4"), offmm("D5") };
    	messageSequence.put(new Integer(31), midiMessages);
    	*/
    }

    //Generates noteOn message for note name
    public MidiMessage onmm(String note){
    	return new MidiMessage(true,n2i(note));
    }
    //Generates noteOn message for note name
    public MidiMessage offmm(String note){
    	return new MidiMessage(false,n2i(note));
    }
    //note name to note id
    public int n2i(String name){
    	System.out.println("n = "+name);
    	return notes.get(name);
    }
    //note id to note name
    public String i2n(int id){
    	//System.out.println("id2note size = "+id2note.size()+"; i = "+id+"; note = "+id2note.get((Integer)id));
    	return id2note.get((Integer)id);
    }
    
    public void playSequence(){
    	currentPosition = 0;
    	try {
	    	while(currentPosition < sequenceLength){
	    		/*
	    		System.out.println("cur pos = "+currentPosition);
	    		System.out.println("len = "+messageSequence.size());
	    		Enumeration e = messageSequence.keys();
	    		while(e.hasMoreElements()){
	    			System.out.println(e.nextElement());
	    		}
	    		if(messageSequence.contains(currentPosition)){
	    			System.out.println("TRUE");
	    		}else{
	    			System.out.println("FALSE");
	    		}
	    		 */
		    	if(messageSequence.containsKey(currentPosition)){
		    		MidiMessage[] thisFrameMessages = messageSequence.get(currentPosition);
		    		for(MidiMessage mm : thisFrameMessages){
		    			snd(mm.noteID, mm.noteOn);
		    			//System.out.println("RO");
		    		}
		    	}
		    	Thread.sleep(sixteenthDelay);
		    	currentPosition++;
	    	}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	/*
    	try {
    	
    	
    		int delay = 500;
        	sendChord( notes.get("C4"), true, CHORD_KVINTAKORD_V, 0);
			Thread.sleep(delay);
	    	sendChord( notes.get("C4"), false, CHORD_KVINTAKORD_V, 0);
        	sendChord( notes.get("G4"), true, CHORD_KVINTAKORD_V, 1);
			Thread.sleep(delay);
	    	sendChord( notes.get("G4"), false, CHORD_KVINTAKORD_V, 1);
        	sendChord( notes.get("A3"), true, CHORD_KVINTAKORD_M, 0);
			Thread.sleep(delay);
	    	sendChord( notes.get("A3"), false, CHORD_KVINTAKORD_M, 0);
        	sendChord( notes.get("F4"), true, CHORD_KVINTAKORD_V, 2);
			Thread.sleep(delay);
	    	sendChord( notes.get("F4"), false, CHORD_KVINTAKORD_V, 2);
	    	
	    	
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
    }
    
    public void initialize(){
    	allowedProgression.add(new int[]{0});				//Id 0 unused
    	allowedProgression.add(new int[]{1,2,3,4,5,6,7});	//I
    	allowedProgression.add(new int[]{5,7});				//II
    	allowedProgression.add(new int[]{4,6});				//III
    	allowedProgression.add(new int[]{1,2,5,7});			//IV
    	allowedProgression.add(new int[]{1,6});				//V	
    	allowedProgression.add(new int[]{2,4,5});			//VI
    	allowedProgression.add(new int[]{1,5});				//VII

    	allowedProgressionProb.add(new float[]{1});				//Id 0 unused
    	allowedProgressionProb.add(new float[]{0.05f, 0.01f, 0.05f, 0.05f, 0.8f, 0.03f, 0.01f});	//I
    	allowedProgressionProb.add(new float[]{0.5f, 0.5f});										//II
    	allowedProgressionProb.add(new float[]{0.7f, 0.3f});										//III
    	allowedProgressionProb.add(new float[]{0.8f, 0.05f, 0.05f, 0.1f});							//IV
    	allowedProgressionProb.add(new float[]{0.2f, 0.8f});										//V	
    	allowedProgressionProb.add(new float[]{0.15f, 0.8f, 0.05f});								//VI
    	allowedProgressionProb.add(new float[]{0.8f, 0.2f});										//VII

    	appropriateHomeTones.put("C", new String[]{"C","E","G"});
    	appropriateHomeTones.put("Cs", new String[]{"Cs","Es","Gs"});
    	appropriateHomeTones.put("D", new String[]{"D","Fs","A"});
    	appropriateHomeTones.put("Ds", new String[]{"Ds","G","As"});
    	appropriateHomeTones.put("E", new String[]{"E","Gs","C"});
    	appropriateHomeTones.put("F", new String[]{"F","A","C"});
    	appropriateHomeTones.put("Fs", new String[]{"Fs","As","Gs"});
    	appropriateHomeTones.put("G", new String[]{"G","H","D"});
    	appropriateHomeTones.put("Gs", new String[]{"Gs","C","Ds"});	//Hs == C
    	appropriateHomeTones.put("A", new String[]{"A","Cs","E"});
    	appropriateHomeTones.put("As", new String[]{"As","D","F"});		//Fs == F
    	appropriateHomeTones.put("H", new String[]{"H","Cs","Fs"});

    	chordStartToneProbabilities.put(1, new Float[]{0.4f, 0.1f, 0.1f, 0f, 0.25f, 0.05f, 0.1f});	//Dur
    	chordStartToneProbabilities.put(2, new Float[]{0.4f, 0.1f, 0.1f, 0f, 0.25f, 0.05f, 0.1f});	//Mol
    	chordStartToneProbabilities.put(3, new Float[]{0.4f, 0.1f, 0.1f, 0f, 0.25f, 0.05f, 0.1f});	//Mol
    	chordStartToneProbabilities.put(4, new Float[]{0.4f, 0.1f, 0.1f, 0f, 0.25f, 0.05f, 0.1f});	//Dur
    	chordStartToneProbabilities.put(5, new Float[]{0.4f, 0.1f, 0.1f, 0f, 0.25f, 0.05f, 0.1f});	//Dur
    	chordStartToneProbabilities.put(6, new Float[]{0.4f, 0.1f, 0.1f, 0f, 0.25f, 0.05f, 0.1f});	//Mol
    	chordStartToneProbabilities.put(7, new Float[]{0.4f, 0.1f, 0.1f, 0f, 0.25f, 0.05f, 0.1f});	//Mol

    	chordToneIntensity.put(1, new Integer[]{0, 4, 3, 10, 2, 7, 7});	//Dur
    	chordToneIntensity.put(2, new Integer[]{0, 4, 3, 10, 2, 9, 7});	//Mol
    	chordToneIntensity.put(3, new Integer[]{0, 4, 3, 10, 2, 9, 7});	//Mol
    	chordToneIntensity.put(4, new Integer[]{0, 4, 3, 10, 2, 7, 7});	//Dur
    	chordToneIntensity.put(5, new Integer[]{0, 4, 3, 10, 2, 7, 7});	//Dur
    	chordToneIntensity.put(6, new Integer[]{0, 4, 3, 10, 2, 9, 7});	//Mol
    	chordToneIntensity.put(7, new Integer[]{0, 4, 3, 10, 2, 9, 7});	//Mol
    	
    	int toneID = C0;
    	
    	for(int oktava = 0; oktava < 8; oktava++){
    		for(int tonVOktavi = 0; tonVOktavi < noteNames.length; tonVOktavi++){
    			String imeTona = noteNames[tonVOktavi];
    			//System.out.println(imeTona+""+oktava+" - "+toneID);
    			notes.put(imeTona+""+oktava, (Integer)toneID);
    			id2note.put((Integer)toneID,imeTona+""+oktava);
    			toneID++;
    		}
    	}
    }
    
    //Akord
    //Type
    public void sendChord(int baseNoteVal, boolean noteOn, int type, int inversion){

    	int tone1 = 0;
    	int tone2 = 0;
    	int tone3 = 0;
    	int tone4 = 0;
    	
    	int chordTones = 0;
    	
    	switch(type){
    	case CHORD_SINGLE:
        	tone1 = baseNoteVal;
    		chordTones = 1;
        	break;
    	case CHORD_KVINTAKORD_V:
        	tone1 = baseNoteVal;
        	tone2 = baseNoteVal+4;
        	tone3 = baseNoteVal+7;
    		chordTones = 3;
        	break;
    	case CHORD_KVINTAKORD_M:
        	tone1 = baseNoteVal;
        	tone2 = baseNoteVal+3;
        	tone3 = baseNoteVal+7;
    		chordTones = 3;
        	break;
    	case CHORD_KVINTAKORD_ZV:
        	tone1 = baseNoteVal;
        	tone2 = baseNoteVal+4;
        	tone3 = baseNoteVal+8;
    		chordTones = 3;
        	break;
    	case CHORD_KVINTAKORD_ZM:
        	tone1 = baseNoteVal;
        	tone2 = baseNoteVal+3;
        	tone3 = baseNoteVal+6;
    		chordTones = 3;
        	break;
    	case CHORD_SEPTAKORD_V:
        	tone1 = baseNoteVal;
        	tone2 = baseNoteVal+4;
        	tone3 = baseNoteVal+7;
        	tone4 = baseNoteVal+11;
    		chordTones = 4;
        	break;
    	case CHORD_SEPTAKORD_M:
        	tone1 = baseNoteVal;
        	tone2 = baseNoteVal+3;
        	tone3 = baseNoteVal+7;
        	tone4 = baseNoteVal+10;
    		chordTones = 4;
        	break;
    	case CHORD_SEPTAKORD_DOM:
        	tone1 = baseNoteVal;
        	tone2 = baseNoteVal+4;
        	tone3 = baseNoteVal+7;
        	tone4 = baseNoteVal+10;
    		chordTones = 4;
        	break;
    	case CHORD_SEPTAKORD_PZM:
        	tone1 = baseNoteVal;
        	tone2 = baseNoteVal+4;
        	tone3 = baseNoteVal+6;
        	tone4 = baseNoteVal+10;
    		chordTones = 4;
        	break;
    	case CHORD_SEPTAKORD_ZM:
        	tone1 = baseNoteVal;
        	tone2 = baseNoteVal+3;
        	tone3 = baseNoteVal+6;
        	tone4 = baseNoteVal+9;
    		chordTones = 4;
        	break;
    	}

    	if(inversion == 1){
    		if(chordTones == 3){
    			tone2 -= OCTAVE;
    			tone3 -= OCTAVE;
    		}
    		if(chordTones == 4){
    			tone3 -= OCTAVE;
    			tone4 -= OCTAVE;
    		}
    	}
    	if(inversion == 2){
    		if(chordTones == 3){
    			tone3 -= OCTAVE;
    		}
    		if(chordTones == 4){
    			tone4 -= OCTAVE;
    		}
    	}
    	if(inversion == 3){
    		if(chordTones == 4){
    			tone2 -= OCTAVE;
    			tone3 -= OCTAVE;
    			tone4 -= OCTAVE;
    		}
    	}
    	
    	if(tone1 != 0){
    		snd(tone1, noteOn);
    	}
    	if(tone2 != 0){
    		snd(tone2, noteOn);
    	}
    	if(tone3 != 0){
    		snd(tone3, noteOn);
    	}
    	if(tone4 != 0){
    		snd(tone4, noteOn);
    	}
    }
}


class MidiMessage{
	boolean noteOn = true;
	int noteID = 0;

	public MidiMessage(boolean noteOn, int noteID){
		this.noteOn = noteOn;
		this.noteID = noteID;
	}
}