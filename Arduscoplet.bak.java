// for painting the screen
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
// for event handling
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseMotionAdapter;
// for the security stuff to access RXTX
import java.security.AccessController;
import java.security.PrivilegedAction;
// for all of the serial communication:
import java.io.InputStream;
import java.io.OutputStream;
import gnu.io.CommPortIdentifier; 
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent; 
import gnu.io.SerialPortEventListener; 
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
// for enumerating ports
import java.io.IOException;
import java.util.Enumeration;
import java.util.TooManyListenersException;

public class Arduscoplet implements SerialPortEventListener{

    private static final String PORT_NAMES[] = { 
                    "/dev/tty.usbserial-A9007UX1", // Mac OS X
                    "/dev/ttyUSB0", // Linux
                    "COM3", // Windows
                    "COM4",
                    "COM5",
                    "COM6",
                    "COM7",
                    "COM8",
                    "COM9"
                    };
    /** Milliseconds to block while waiting for port open */
    private static final int TIME_OUT = 2000;    

    private static final int DATA_RATE = 115200;  // Peter Malcolm: data rate for oscilloscope (PM)
    
    private JFrame f = null;
    private MyPanel p = null;
    private InputStream in = null;
    private OutputStream out = null;
    private SerialPort serialPort;
    
    
    //////////////////////////////////////////////////////////////////////////////
    /////////  CODE BELOW COURTESY OF HACKY'S BLOG: hacky.typepad.com  ///////////
    //////////////////////////////////////////////////////////////////////////////
    public SerialPort openPort(final String appName, final String wantedPortName,
                           final int baudrate, final int databits,
                           final int stopbits, final int parity, final int timeout) {
        return (SerialPort) AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            SerialPort serialPort = null;
            // code to determine portId by enumerating the ports:
            CommPortIdentifier portId = null;
            Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

            // iterate through, looking for the port
            while (portEnum.hasMoreElements()) {
                    CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
                    for (String portName : PORT_NAMES) {
                            if (currPortId.getName().equals(portName)) {
                                    portId = currPortId;
                                    break;
                            }
                    }
            }

            if(null==portId){                                                   // Failed to find a plugged-in Arduino
                return null;
            }        
                    
            // assuming you have the portId now:
            try {
                serialPort = (SerialPort) portId.open(
                    appName,   // Name of the application asking for the serialPort
                    TIME_OUT   // Wait max. TIME_OUT to acquire serialPort
                );
            } catch(PortInUseException e) {
                System.err.println("serialPort already in use: " + e);
            }
            // Set all the serial params.  
            //
            try {
                serialPort.setSerialPortParams(baudrate,databits,stopbits,parity);
            }
            catch (UnsupportedCommOperationException ucox) { ucox.printStackTrace(); }
            return serialPort;
          }
        });
}
    //////////////////////////////////////////////////////////////////////////////
    
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setup(); 
            }
        });
    }

    private static void setup() {
        System.out.println("Created GUI on EDT? " + SwingUtilities.isEventDispatchThread());
        Arduscoplet a = new Arduscoplet();
        
        a.f = new JFrame("Arduino Oscilloscope, by MakeToLearn");
        a.p = new MyPanel();
        a.f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        a.f.add(a.p);
        a.f.pack();
        a.f.setVisible(true);

        a.serialPort = a.openPort("oscilloscope",                               
                                  "COM3",DATA_RATE,
                                  SerialPort.DATABITS_8,
                                  SerialPort.STOPBITS_1,
                                  SerialPort.PARITY_NONE,
                                  a.TIME_OUT);
        if(null==a.serialPort){                                                 // failed to find device
            System.out.println("Error: Could not find attached Arduino");
            a.p.showMessage("Could not find attached Arduino");
        } else {                                                                // success.  device found
            try {
                a.in = a.serialPort.getInputStream();
            } catch(java.io.IOException jioe){
                System.out.println("Failed to get input stream... \n" + jioe);
            }
            
            try {
                a.out = a.serialPort.getOutputStream();
            } catch(java.io.IOException jioe){
                System.out.println("Failed to get output stream... \n" + jioe);
            }
            
            // add event listeners
            try {
                a.serialPort.addEventListener(a);
            } catch(java.util.TooManyListenersException tmle) {
                System.out.println("Failed to add listener ... \n" + tmle);
            }
            a.serialPort.notifyOnDataAvailable(true);
            
        }
        
    }
    
    public synchronized void serialEvent(SerialPortEvent oEvent) {
        String eventType = "";
        switch (oEvent.getEventType()){
            case SerialPortEvent.BI : eventType = "break interrupt"; break;
            case SerialPortEvent.CD : eventType = "carrier detect"; break;
            case SerialPortEvent.CTS: eventType = "clear to send"; break;
            case SerialPortEvent.DATA_AVAILABLE : eventType = "data available"; break;
            case SerialPortEvent.DSR : eventType = "data set ready"; break;
            case SerialPortEvent.FE : eventType = "framing error"; break;
            case SerialPortEvent.OE : eventType = "overrun error"; break;
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY : eventType = "output buffer empty"; break;
            case SerialPortEvent.PE : eventType = "parity error"; break;
            case SerialPortEvent.RI : eventType = "ring indicator"; break;
        }
        // p.showMessage("got something... " + eventType);
        if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                    int available = in.available();
                    byte chunk[] = new byte[available];
                    in.read(chunk, 0, available);

                    // Displayed results are codepage dependent
                    // System.out.print(new String(chunk)); //  print this (PM)

                    System.out.print(Integer.toHexString(0xFF & chunk[0])); //  print this (PM) - stupid mistake, was 0xFFFF
                    System.out.print(" ");                                  //  print this (PM)

                    // p.showDataMessage(Integer.toHexString(0xFF & chunk[0])); // ANDing ruins the data (!?)
                    // p.showDataMessage(Integer.toHexString(chunk[0]));
                    p.setCurrentYPos((int)chunk[0]);
                    p.quickRepaint();

            } catch (Exception e) {
                    System.err.println(e.toString());
            }
        } else {
            System.out.println("Received unknown Serial Event Type ...");
        }
        // Ignore all the other eventTypes, but you should consider the other ones.
        
    }
    
}

class MyPanel extends JPanel {

    private int squareX = 50;
    private int squareY = 50;
    private int squareW = 20;
    private int squareH = 20;
    private String message = "";
    private String dataMessage = "";
    private Color currentColor = Color.BLACK;
    
    private Boolean isDataRepaint = false;
    private int currentXPos = 0;
    private int currentYPos = 0;
    private int formerYPos = 0;
    
    private final int PANEL_WIDTH = 450;
    private final int SCOPE_HEIGHT = 256;
    private final int SCOPE_OFFSET = 80;
    
    public MyPanel() {

        setBorder(BorderFactory.createLineBorder(Color.black));
        // setBackground(Color.WHITE);
        /*
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                moveSquare(e.getX(),e.getY());
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                moveSquare(e.getX(),e.getY());
            }
        });
         * 
         */
        
    }
    
    public void setIsDataRepaint(Boolean newIsDataRepaint){
        isDataRepaint = newIsDataRepaint;
    }
    
    public void setCurrentYPos(int newCurrentYPos){
        formerYPos = currentYPos;
        currentYPos = newCurrentYPos;
    }
    
    private void moveSquare(int x, int y) {
        int OFFSET = 1;
        if ((squareX!=x) || (squareY!=y)) {
            repaint(squareX,squareY,squareW+OFFSET,squareH+OFFSET);
            squareX=x;
            squareY=y;
            repaint(squareX,squareY,squareW+OFFSET,squareH+OFFSET);
        } 
    }
    

    public Dimension getPreferredSize() {
        return new Dimension(PANEL_WIDTH, SCOPE_HEIGHT+SCOPE_OFFSET);
    }
    
    public void showMessage(String inMessage){
        message = inMessage;
        repaint();
    }
    
    public synchronized void showDataMessage(String inMessage) {
        if(dataMessage.length() < 50 ) {
            currentColor = Color.GRAY;                                          // erase the original message
            repaint();
            
            dataMessage = ": " + inMessage + dataMessage;                       // update and draw new message
//          dataMessage = ": " + inMessage.length() + dataMessage;            // this demonstrated that only one character is being sent...            
            currentColor = Color.BLACK;
            repaint();
        }
    }
    
    public void quickRepaint(){
        isDataRepaint = true;
        repaint(currentXPos, SCOPE_OFFSET, currentXPos, SCOPE_OFFSET+SCOPE_HEIGHT);
        // repaint();
        currentXPos++;
        if(currentXPos > PANEL_WIDTH) {                                         // wrap to far-left again
            currentXPos = 0;
        }
             
    }
    
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(!isDataRepaint) {                                                    // initially, this is *not* a repaint
            g.drawString("Arduino Oscilloscope, by MakeToLearn",10,20);
            g.drawString(message, 10, 38);
            g.drawString(dataMessage, 10, 56);
            // g.setColor(Color.RED);
            // g.fillRect(squareX,squareY,squareW,squareH);
            g.setColor(Color.BLACK);
            //g.drawRect(squareX,squareY,squareW,squareH);
            g.drawLine(0, (SCOPE_HEIGHT/2+SCOPE_OFFSET), PANEL_WIDTH, (SCOPE_HEIGHT/2+SCOPE_OFFSET));
        } else {                                                                // now this *is* a repaint
            g.setColor(Color.WHITE);                                             // wash out the previous data ...
            // g.drawLine(currentXPos, SCOPE_OFFSET, currentXPos, SCOPE_OFFSET+SCOPE_HEIGHT);  // with a gray line
            g.setColor(Color.BLACK);                                            // draw stuff in black now
            g.drawLine(currentXPos-1, (SCOPE_HEIGHT/2+SCOPE_OFFSET), currentXPos+1, (SCOPE_HEIGHT/2+SCOPE_OFFSET)); // redraw center line
            g.drawLine(currentXPos, (SCOPE_HEIGHT/2+SCOPE_OFFSET-formerYPos), currentXPos, (SCOPE_HEIGHT/2+SCOPE_OFFSET-currentYPos));
        }
        
    }  
}