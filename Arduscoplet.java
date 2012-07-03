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
// for  the security stuff to access RXTX
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
import java.util.Enumeration;

public class Arduscoplet {

    private static final String PORT_NAMES[] = { 
                    "/dev/tty.usbserial-A9007UX1", // Mac OS X
                    "/dev/ttyUSB0", // Linux
                    "COM3", // Windows
                    };
    /** Milliseconds to block while waiting for port open */
    private static final int TIME_OUT = 2000;    

    private static final int DATA_RATE = 115200;  // Peter Malcolm: data rate for oscilloscope (PM)
    
    private SerialPort serialPort;
    
    
    //////////////////////////////////////////////////////////////////////////////
    /////////  CODE BELOW COURTESY OF HACKY'S BLOG: hacky.typepad.com  ///////////
    //////////////////////////////////////////////////////////////////////////////
    public SerialPort openPort(final String appName, final String wantedPortName,
                           final int baudrate, final int databits,
                           final int stopbits, final int parity) {
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
                createAndShowGUI(); 
            }
        });
    }

    private static void createAndShowGUI() {
        System.out.println("Created GUI on EDT? "+
        SwingUtilities.isEventDispatchThread());
        JFrame f = new JFrame("Arduino Oscilloscope, by MakeToLearn");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        f.add(new MyPanel());
        f.pack();
        f.setVisible(true);
    } 
}

class MyPanel extends JPanel {

    private int squareX = 50;
    private int squareY = 50;
    private int squareW = 20;
    private int squareH = 20;

    public MyPanel() {

        setBorder(BorderFactory.createLineBorder(Color.black));

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
        return new Dimension(450,400);
    }
    
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);       
        g.drawString("Arduino Oscilloscope, by MakeToLearn",10,20);
        // g.setColor(Color.RED);
        // g.fillRect(squareX,squareY,squareW,squareH);
        g.setColor(Color.BLACK);
        //g.drawRect(squareX,squareY,squareW,squareH);
        g.drawLine(0,420/2,450,420/2);
    }  
}