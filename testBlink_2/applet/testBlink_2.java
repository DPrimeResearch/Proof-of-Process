import processing.core.*; 
import processing.xml.*; 

import processing.serial.*; 
import oscP5.*; 
import netP5.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class testBlink_2 extends PApplet {

/* controller for four_dancing_tubes_1 arduino code
* this code receives servoNumber and degree number using OSC message
* this code then sends servoNumber and degree number to arduino
* this code then receives lightSensor reading from arduino and output OSC message

arduino: four_dancing_tubes_2

added support for tooTired
*/





OscP5 oscP5;
NetAddress myRemoteLocation;

boolean randomize = false; //0 for don't randomize, 1 for naturally randomizing

//algae can get tired
//should wait 5 mins then restart algae
//tired is mask, 0 = not tired, 1 = tired and wait
float tiredArray[] = {0,0,0,0};
int waitTime = 5 * 60 * 1000; //5 minutes

Serial myPort;

public void setup() {
  size (200,200);
  
  // connect to arduino
  println(Serial.list());
  myPort = new Serial(this, Serial.list()[1], 115200);
  myPort.bufferUntil('\n');
  
  // start oscP5, listening for incoming messages at port 12000
  oscP5 = new OscP5(this,12000);
  //myRemoteLocation = new NetAddress("127.0.0.1",32000);
  myRemoteLocation = new NetAddress("192.168.1.122",32000);
}

public void draw() {
  background(0);
  
  //self running, randomize
  if (randomize) {
    int servoNum = (int)random(1,2);
    int degree = (int)random(0,255); //0-255

    String degreeString = padDegree(degree); //make sure degree is 3 chars long
    sendOSC(servoNum, degree, degree);
    sendTest(servoNum, degree, degree);
    delay(5000);
  }
}

public void keyPressed() {
  OscMessage m;
  switch(key) {
    case('c'):
      /* connect to the broadcaster */
      m = new OscMessage("/server/connect",new Object[0]);
      oscP5.flush(m,myRemoteLocation);  
      break;
    case('d'):
      /* disconnect from the broadcaster */
      m = new OscMessage("/server/disconnect",new Object[0]);
      oscP5.flush(m,myRemoteLocation);  
      break;
     case('e'):
       /* send osc message to be broadcated */
      sendOSC(1,200, 200); 
      sendTest(1, 200,200);
      break;

  }  
}


public boolean checkTooTired(int servo) {
  servo--;
  println("inside checkTooTired");
  //can we reset tiredArray val?
  float currentTime = millis();
  if (tiredArray[servo] == 0) {
      return false;
  } else if (currentTime - tiredArray[servo] >= waitTime) {
      //not too tired, waited long enough
      //reset tiredArray[servo]
      tiredArray[servo] = 0;
      return false;
  }
  print("too tired: ");
  println(currentTime + " " + tiredArray[servo]);
  return true;
}

public void setTooTired(int servo) {
  servo--;
  println("inside setTooTired");
  float currentTime = millis();
  if (tiredArray[servo] > 0) {
    //do nothing
    //can we reset tiredArray?
    if (currentTime - tiredArray[servo] >= waitTime) {
      tiredArray[servo]=0;
    }
  } else {
    //set tooTiredArray
    tiredArray[servo] = millis();
  }
  println("servo: " + servo + " tiredArray: " + tiredArray[servo]);
}

public String padDegree(int degree) {
  String degreeString = degree + "";
  //need to make sure degree is 3 characters, pad with zeros
  if (degree < 10) {
  degreeString="00"+degree;
  } else if (degree < 100) {
  degreeString ="0"+degree;
  }
  return degreeString;
}

public String padDegree(String degree) {
  String degreeString = degree;
  //need to make sure degree is 3 characters, pad with zeros
  if (degree.length() == 1) {
  degreeString="00"+degree;
  } else if (degree.length() == 2) {
  degreeString ="0"+degree;
  }
  return degreeString;
}

public void mousePressed() {
  sendOSC((int)4, 255, 255); //change test values here
  sendTest((int)4, 255, 255); //test serial connection
  
}

public void sendTest(int servoNum, int blueVal, int greenVal) {
  String servoString = servoNum+"";
  String degString = padDegree((blueVal+""));  
  //testing serial data
  myPort.write(servoString); //always 1 digit
  myPort.write(degString); //0 to 360, always 3 digits
  myPort.write('\n');
  delay (3000); //configured to send info, wait, finish agitate, finish waiting, 
  println("done waiting");
  
}
public void sendOSC(int servoNum, int blueVal, int greenVal) {
  /* in the following different ways of creating osc messages are shown by example */
  OscMessage myMessage = new OscMessage("/frombiolesce"); //this message is isc
  
  myMessage.add(servoNum); /* add an int to the osc message */
  myMessage.add(blueVal); /* add a float to the osc message */
  myMessage.add(greenVal);
  
  /* send the message */
  oscP5.send(myMessage, myRemoteLocation); 
  print("sent OSC message: ");
  print(servoNum);
  print(" ");
  print(blueVal);
  print(" ");
  print(greenVal);
}


/* incoming osc message are forwarded to the oscEvent method. */
//from /fromsorrykeyboard
public void oscEvent(OscMessage theOscMessage) {
  /* print the address pattern and the typetag of the received OscMessage */
  print("### received an osc message from: " +theOscMessage.addrPattern());
  println(" typetag: "+theOscMessage.typetag());
  //println(theOscMessage.typetag());
  if (theOscMessage.checkAddrPattern("/fromsorrykeyboard")) {
    if(theOscMessage.checkTypetag("ii")) {
      println("### received a VALID osc message from SORRYKEYBOARD: ");
      int servo = theOscMessage.get(0).intValue(); //int
      int deg = theOscMessage.get(1).intValue(); //int
      
      //map diego's 21-108 to 1-4
      
      servo = PApplet.parseInt(map(servo, 21, 108, 1, 4));
      //println("inside sorrykeyboard: " + servo + "," + degString);
      
      if (!checkTooTired(servo)) {
        println("sending to arduino from sorrykeyboard");
        //check deg is 3 chars
        String degString = padDegree((deg+""));
        //send test digit
        String servoString = servo+"";
        //apparantly, myPort.write is only good for strings
        myPort.write(servoString); //always 1 digit
        myPort.write(degString); //0 to 360, always 3 digits
        myPort.write('\n');
        delay (3000); //configured to send info, wait, finish agitate, finish waiting, 
        println("done waiting for keyboard" + servoString + " " + degString);
      }
    }
  }
  int servoProtocol = 999;
  int degBiopoesis1 = 999;
  int degBiopoesis2 = 999;
  //fromprotocol int [30-200], map this to servo
  //frombriopoesis int [0-255],  map this to amount
  if (theOscMessage.checkAddrPattern("/fromprotocol")) {
    if(theOscMessage.checkTypetag("i")) {
      println("### received a VALID osc message from PROTOCOL: ");
      servoProtocol = theOscMessage.get(0).intValue(); //int
      
      servoProtocol = PApplet.parseInt(map(servoProtocol, 30, 200, 1, 4));
    }
  }
    if (theOscMessage.checkAddrPattern("/frombiopoiesis")) {
    if(theOscMessage.checkTypetag("iiii")) {
      println("### received a VALID osc message from biopoesis: ");
      degBiopoesis1 = theOscMessage.get(0).intValue(); //int
      degBiopoesis2 = theOscMessage.get(1).intValue(); //int
      //don't need to map anything here
      //degBiopoesis = int(map(servoProtocol, 30, 200, 1, 4));
    }
  }
  if (servoProtocol != 999 && degBiopoesis1 != 999 && degBiopoesis2 == 999) {//send to arduino

    if (!checkTooTired(servoProtocol)) {
      //check deg is 3 chars
      String degString = padDegree((degBiopoesis1+""));
      println("sending to arduino");
      //send test digit
      String servoString = servoProtocol+"";
      //apparantly, myPort.write is only good for strings
      myPort.write(servoString); //always 1 digit
      myPort.write(degString); //0 to 360, always 3 digits
      myPort.write('\n');
      delay (3000); //configured to send info, wait, finish agitate, finish waiting, 
      println("done waiting from protocol and biopoesis" + servoProtocol + " " +degBiopoesis1);
    }
  }
  if (servoProtocol == 999 && degBiopoesis1 != 999 && degBiopoesis2 != 999) {
      //map degBiopoesis1 1-4
      //map degBiopoesis2 0-255
      degBiopoesis1 = PApplet.parseInt(map(degBiopoesis1, 0, 255, 1, 4));

      
    if (!checkTooTired(degBiopoesis1) && degBiopoesis2 > 0) {
      //check deg is 3 chars
      String degString = padDegree((degBiopoesis2+""));
      println("sending to arduino");
      //send test digit
      String servoString = degBiopoesis1+"";
      degString = degBiopoesis2+"";
      //apparantly, myPort.write is only good for strings
      myPort.write(servoString); //always 1 digit
      myPort.write(degString); //0 to 360, always 3 digits
      myPort.write('\n');
      delay (3000); //configured to send info, wait, finish agitate, finish waiting, 
      println("done waiting from biopoesis" + degBiopoesis1 + " " + degBiopoesis2);
  }
  }
  delay(5000);//wait for next input
  
  
}

public void serialEvent (Serial myPort) { //wait for serial data and send out using OSC
  String myString = myPort.readStringUntil('\n');
  int outmessage = 0; //none are too tired, 1=servo1, etc.
    if (myString != null) {
      myString = trim(myString);
      println ("from arduino: " + myString);      
      
      if (!myString.equals("0000") && !myString.equals("000000")) {
              
        //myInts[0] = servo, myInts[1] = bioLight
        int myInts[] = PApplet.parseInt(split(myString, ','));
        
        //just making sure there is no zero from arduino
        if (myInts[0] != 0) {
          //if bioLight < 10, tiredArray keep track
          if (myInts[1] < 10) {
            setTooTired(myInts[0]);
                     
          } else {
            //output to Diego that servo is too tired
            outmessage = myInts[0]; 
            
            //output using OSC
            //OscMessage myMessage = new OscMessage("/frombiolesce");
            //myMessage.add(outmessage); //servo is too tired?
            
            //map biolight to value between blue and green, red is 0
            //map myInts[1] between 0 and 255
            //send blue
            //send green
            
            int colorVal = PApplet.parseInt(map(myInts[1], 0, 1023, 0, 255));
            float blueness = 0.70f;
            float greenness = 0.30f;
            
            //myMessage.add(int(blueness*colorVal)); //blue
            //myMessage.add(int(greenness*colorVal)); //green
            
            // send the message 
            //oscP5.send(myMessage, myRemoteLocation); 
            //println("sent OSC message: " + outmessage + " " + int(blueness*colorVal) + " "+ int(greenness*colorVal));
          sendOSC (outmessage, PApplet.parseInt(blueness*colorVal), PApplet.parseInt(greenness*colorVal));
          //sendTest (outmessage, int(blueness*colorVal), int(greenness*colorVal));
          }
        }
      } else {
        println("received SerialEvent message: " + myString);      //want to print out everything
      }
    }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#FFFFFF", "testBlink_2" });
  }
}
