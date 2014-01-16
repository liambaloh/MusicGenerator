/**
 * UDP Datagram Sockets: http://docs.oracle.com/javase/tutorial/networking/datagrams/index.html
 * 
 * Naloga:
 * 1. Ali poteka predvajanje v realnem času? Zakaj?
 * 2. Ko boste končali nalogo 1. spremenite metodo main(), tako da boste predvajali glasbo vašemu sosedu.
 *    Naslov IP pridobite tako, da odprete ukazno vrstico "cmd" in vpišete: "ipconfig"
 * 3. Ustvarite razred MIDIStreamer, tako da bo koda oddajnika napisana v objektno orientiranemu načinu.
 * 4. Ustvarite scenarij, da bosta dva različna instrumenta hkrati igrala lestivco C-dur. Za to boste
 *    potrebovali dve instanci MIDIStreamPlayer.
 * 
 */
package midistreamer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class MIDIStreamPlayer extends MIDIPlayer implements Callable {
    
    /**
     * Atributi so enaki MIDIStreamSimplePlayer
     */
    protected int receiverPort;
    protected DatagramSocket rsocket;
    protected byte[] rcvBuffer;
    protected DatagramPacket packet;
    
    protected ByteArrayInputStream bis;
    protected ObjectInputStream in;
    
    public MIDIStreamPlayer(int playerChannel, int playerProgram, int receiverPort) {
        
        super(playerChannel, playerProgram);
        
        this.receiverPort=receiverPort;
        try {
            this.rsocket = new DatagramSocket(receiverPort); // Ustvari Datagramski vtič
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        this.rcvBuffer=new byte[1024]; // Ustvari izravalnik
    }
    
    /**
     * 4. IPC via NETWORK COMMUNICATION: RECEIVER
     * V primerjavi z MIDIStreamSimplePlayer ustvarimo sedaj nit, ki bo opravljala to nalogo.
     * Identična procedura, ki jo lahko najdemo v MIDIFileSimplePlayer.rcv(), se sedaj
     * nahaja v metodi callback(). Nit, ki jo ustvarimo v tej metodi, kliče metodo callback()
     * 
     * To je tudi razlog, da (i) MIDIStreamPlayer v primerjavi z MIDIStreamSimplePlayer
     * implementira vmesnik Callable, (ii) nit ima referenco objekta MIDIStreamSimplePlayer podanega
     * kot argument in (iii) nit klice metodo objekta MIDIStreamSimplePlayer callback().
     */
    @Override
    public synchronized void rcv() {
        new WorkerThread(this).start();
    }
    
    /**
     * 5. To metodo dejansko klice nit, ki smo jo ustvarili v metodu rcv()
     */
    public synchronized void callback() {
        System.out.println("RECEIVER ST: START");
        while(true) {
            try {
                this.packet = new DatagramPacket(this.rcvBuffer,this.rcvBuffer.length);  // Ustvari Datagramski paket
                this.rsocket.receive(this.packet); // Pocakaj na novi paketi
                this.bis = new ByteArrayInputStream(this.rcvBuffer); // Dekapsuliraj in pridobi byte[]
                this.in = new ObjectInputStream(bis); // Deserializiraj objekt Ton
                try {
                    super.zapis.put((Ton) in.readObject()); // Zapisi v izravnalnik
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                } finally {
                    this.in.close();
                    this.bis.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        
        /**
         * 1. INIT
         */
        
        int DEFAULT_TRANSMITTER_PORT = 3400;
        int DEFAULT_RECEIVER_PORT = 3300;
        String DEFAULT_RECEIVER_ADDRESS = "127.0.0.1";
        /**
         * Ustvari MIDIDummyPlayer: kanal 0, električni klavir (6, http://en.wikipedia.org/wiki/General_MIDI) in
         * vrata datagramskega vtiča (kjer bo MIDIStreamSimplePlayer prevzemal datagramske pakete z zvočnim zapisom)
         */
        MIDIStreamPlayer msp = new MIDIStreamPlayer(0, 1, DEFAULT_RECEIVER_PORT);
        msp.start();

        /**
         * 2.Pocakaj 4000 ms
         */
        try {
            Thread.sleep(4000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        /**
         * 3. IPC via NETWORK COMMUNICATION: TRANSMITTER
         * V primerjavi z MIDIDummyPlayer imamo sedaj pravi tokovnik!
         */
        System.out.println("TRANSMITTER: START");
        DatagramSocket tsocket = null;
        try {
            tsocket = new DatagramSocket(DEFAULT_TRANSMITTER_PORT); // Odpri vtič na izbranih vratih
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        ObjectOutputStream out = null;
        try {
            for(int i=10; i<120; i++) {
                
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                out = new ObjectOutputStream(bos);
                out.writeObject(new Ton((Integer)i,(Integer)50,(Integer)100,(Integer)6,(Integer)1));
                byte[] sndBuffer = bos.toByteArray(); // Serializacija objekta v byte[]
                /**
                 * Tokovnik poslje datagramski paket:
                 * - na naslov IP InetAddress.getByName(DEFAULT_RECEIVER_ADDRESS) in vrata DEFAULT_RECEIVER_PORT.
                 * - z vsebino serializiranega objekta v podatkovni strukturi byte[]
                 */
                DatagramPacket packet = new DatagramPacket(sndBuffer, sndBuffer.length, InetAddress.getByName(DEFAULT_RECEIVER_ADDRESS), DEFAULT_RECEIVER_PORT);
                tsocket.send(packet);
                
                out.close();
                bos.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("TRANSMITTER: END");
        
    }
}
