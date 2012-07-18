// for painting the screen
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
// for the Canvas
import java.awt.*;
import java.awt.image.BufferStrategy;
import javax.swing.Timer;
// for the Chrono's event handling
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
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
    private static final int TIME_OUT   = 2000;    

    private static final int DATA_RATE  = 115200;  // Peter Malcolm: data rate for oscilloscope (PM)

    // the height of the oscilloscope
    public static final int SCOPE_HEIGHT = 256;

    // used elsewhere to determine width of oscilloscope
    // public static final int SCOPE_WIDTH = 1280;
    public static final int SCOPE_WIDTH = 1200;

    public byte[] values = new byte[SCOPE_WIDTH];

    private int currentPosition = 0;
    
    private ArduFrame myAFrame    = null;
    private InputStream in        = null;
    private OutputStream out      = null;
    private SerialPort serialPort = null;
    
    
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

        // set up empty array of values:
        for(int i = 0; i < SCOPE_WIDTH; i++) {
            a.values[i] = 0;
        }

        a.myAFrame = new ArduFrame(a);                              // call constructor of LocalFrame
        
        
//        a.f = new JFrame("Arduino Oscilloscope, by MakeToLearn");
//        a.f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
//        a.f.pack();
//        a.f.setVisible(true);

        a.serialPort = a.openPort("oscilloscope",                               
                                  "COM3",DATA_RATE,
                                  SerialPort.DATABITS_8,
                                  SerialPort.STOPBITS_1,
                                  SerialPort.PARITY_NONE,
                                  a.TIME_OUT);
        if(null==a.serialPort){                                                 // failed to find device
            System.out.println("Error: Could not find attached Arduino");
            // a.p.showMessage("Could not find attached Arduino");
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

                    // System.out.print(Integer.toHexString(0xFF & chunk[0])); //  print this (PM) - stupid mistake, was 0xFFFF
                    // System.out.print(" ");                                  //  print this (PM)

                    for(int i = 0; i < available; i++) {
                        values[currentPosition] = chunk[i];         // put the data in the array
                        currentPosition++;                              // increment the pointer
                        if(currentPosition >= SCOPE_WIDTH) {            // wrap if necessary
                            currentPosition = 0;
                        }
                    }
                    

            } catch (Exception e) {
                    System.err.println(e.toString());
            }
        } else {
            System.out.println("Received unknown Serial Event Type ...");
        }
        // Ignore all the other eventTypes, but you should consider the other ones.
        
    }
	/**
	 * This should be called when you stop using the port.
	 * This will prevent port locking on platforms like Linux.
	 */
	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}
    
}

class ArduFrame extends JFrame {
    ArduFrame(Arduscoplet aScope) {
        // frame description
        super("Arduino Oscilloscope, by MakeToLearn");
        // our Canvas
        ArduCanvas canvas = new ArduCanvas(aScope);
        add(canvas, BorderLayout.CENTER);
        // set it's size and make it visible
        setSize(aScope.SCOPE_WIDTH, aScope.SCOPE_HEIGHT);
        setVisible(true);		
        // now that is visible we can tell it that we will use 2 buffers to do the repaint
        // befor being able to do that, the Canvas as to be visible
        canvas.createBufferStrategy(2);
        canvas.computationDone = true;
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
}

class ArduCanvas extends Canvas {
	// back color LightYellow
	Color backColor = new Color(255, 255, 150);
	// my Swing timer
	Timer timer;
	// for the computation of the postion and the repaint
	Dimension size;
    
	// a boolean to synchronize computation and drawing
	public boolean computationDone = false;
        
	// a boolean to ask the thread to stop
	boolean threadStop = false;
	
        Arduscoplet myAScopeGrandparent = null;
        
        byte[] myCopyOfData = null;
        
	// CONSTRUCTOR: this is a Canvas but I wont't let the system when to repaint it I will do it myself
	ArduCanvas(Arduscoplet aScope) {            
		super();
                // grab a pointer to the LocalScope that will be collecting the data
                myAScopeGrandparent = aScope;
                
                myCopyOfData = new byte[aScope.SCOPE_WIDTH];
                // initialize an entire array of zeroes:
                for(int i = 0; i < aScope.SCOPE_WIDTH; i++) {
                    myCopyOfData[i] = 0;
                }
                        
		// so ignore System's paint request I will handle them
		setIgnoreRepaint(true);

		// build Chrono that will call me 
		Chrono chrono = new Chrono(this);
		// ask the chrono to calll me every 60 times a second so every 16 ms (set say 15 for the time it takes to paint)
		timer = new Timer(16, chrono);
		timer.start();
	}
	
	// my own paint method that repaint off line and switch the displayed buffer
	// according to the VBL
	public synchronized void myRepaint() {
		// computation a lot longer than expected (more than 15ms)... ignore it
		if(!computationDone) {
			return;
		}
                // System.out.println("size="+size);
                int theWidth = myAScopeGrandparent.SCOPE_WIDTH;     // alias width
                int theHeight = myAScopeGrandparent.SCOPE_HEIGHT;     // alias width

		// ok doing the repaint on the not showed page
		BufferStrategy strategy = getBufferStrategy();
                // System.out.println(strategy);
		Graphics graphics = strategy.getDrawGraphics();
		// erase all what I had
		graphics.setColor(backColor);	
		graphics.fillRect(0, 0, theWidth, theHeight);
                // draw the line across the center of the scope
		graphics.setColor(Color.BLACK);	
                graphics.drawLine(0, (theHeight/2), theWidth, (theHeight/2));
                
                
                
                // CODE HERE TO COPY OVER A LOCAL VERSION OF THE CURRENT LOCAL-CANVAS DATA STATE
                for(int i = 0; i < theWidth; i++) { 
                    myCopyOfData[i] = myAScopeGrandparent.values[i];
                    // System.out.println(myCopyOfData[i]); // all zeroes...?
                }
                 
                // CODE HERE TO REDRAW ALL SCOPE LINES
                // RECOMMENDED:
                // LOOP THRU WIDTH OF CANVAS -- this will be the same as the size of the data-array
                // EACH TIME DRAWS A LINE ON GRAPHICS
                for(int i = 0; i < theWidth; i++) {
                    if(0==i){                                                   // wrap around at left side
                        graphics.drawLine(i, theHeight/2-(int)myCopyOfData[theWidth-1], i, theHeight/2-(int)myCopyOfData[0]);
                    } else {
                        graphics.drawLine(i, theHeight/2-(int)myCopyOfData[i-1], i, theHeight/2-(int)myCopyOfData[i]);
                    }
                }
                
		if(graphics != null)
			graphics.dispose();
		// show next buffer
		strategy.show();
		// synchronized the blitter page shown
		Toolkit.getDefaultToolkit().sync();
		// ok I can be called again
		// computationDone = false;
	}
    
}

class Chrono implements ActionListener {
	ArduCanvas ac;
	// constructor that receives the GameCanvas that we will repaint every 60 milliseconds
	Chrono(ArduCanvas ac) {
		this.ac = ac;
	}
	// calls the method to repaint the anim everytime I am called
	public void actionPerformed(ActionEvent e) {
		ac.myRepaint();
	}
    
}