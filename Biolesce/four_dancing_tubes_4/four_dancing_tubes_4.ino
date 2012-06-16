/* four_dancing_tubes_4
* inputs: 4 test tubes can be manipulated/swatted at triggering
*          4 servo motors to agitate bioluminescent material in 4 test tubes
*          4 photoresistor sensors monitor the light emitted 
*          from the bioluminescent material in 4 test tubes
*         (bioluminescent material glows when agitated)
* outputs: servo motor rotates attached test tubes
*          sends output serial stream

* different modes:
1. bioswitch mode: uses input from 4 test tubes when manipulated
2. not bioswitch mode: uses input from Processing (test_Blink_1)
* get numbers from Processing
* agitate tubes according to servoNum and degree

* processing: test_Blink_1

by Yin He, Tyler Fox, Carlos Castellanos 2012
*/

 
#include <Servo.h>

//variables
int degree = 300;
int waitTimeLong = 1000;
int waitTimeShort = 500;

int inByte = 0;
int servoNum = 0;

//ints attach servos to pwm pins
int servoPin1 = 3;
int servoPin2 = 5;
int servoPin3 = 6;
int servoPin4 = 9;


//array of LEDs attached to digital pins
//int lightSensor[] = {7,8,12,13};
int minLight = 400;

Servo servo1;  // create servo object to control a servo
Servo servo2;   // a maximum of eight servo objects can be created
Servo servo3;
Servo servo4;


int posServo1 = 0;    // variables to store the servo positions
int posServo2 = 0;
int posServo3 = 0;
int posServo4 = 0;

//bioswitches
int bioSwitch[] = {7, 8, 12, 13};
boolean bioSwitchMode = false;
boolean serialMode = false;

int pos = 0; //will be added to servo positions-may not need this

int logOut = 0; //0 is no output, 1 is verbose

void setup()
{
  servo1.attach(servoPin1);  // attaches the servo on int servoPinX (1,2,3, or 4) to the servo object
  servo2.attach(servoPin2);
  servo3.attach(servoPin3);
  servo4.attach(servoPin4);
  
  //init bioSwitch
  for (int i = 0; i < 4 ; i ++) {
    pinMode(bioSwitch[i], INPUT);
  }
  
  /*NO MORE LEDs
  //LED digital pins
  for (int i = 0; i < 4; i ++) {
    pinMode(lightSensor[i], OUTPUT);
  }*/
  
  Serial.begin(115200); //bluetooth
  establishContact(); //send dummy serial data
}

void loop() {
  //read bioswitch and serial data
  
  //check for bioSwitch data
  for (int i = 0 ; i < 4 ; i ++) {
    if (digitalRead(bioSwitch[i])) { //bioswitch agitated, debounce
    delay (50);
      if (digitalRead(bioSwitch[i])) {//if still high
        bioSwitchMode = true;
        servoNum = i+1;
        logOutput("bioSwitchMode: " + bioSwitchMode); //why sending out 0?
      }
    }
  }
  
  //check for serial data
  if (Serial.available() > 0) {
  inByte = Serial.parseInt();
    if (inByte != 0000) {
    servoNum = inByte / 1000;
    degree = inByte % 1000;
    
    logOutput("inByte: " + inByte);
    serialMode = true;
    }
  }
        
  if (bioSwitchMode) { //would keep reading forever, can't be 0
    logOutput ("inside bioSwitchMode");
    if (servoNum != 0) {
    //agitate the servo
    int degree = 180; //just cause
    logOutput("inside servoNum !=0");
    agitate(servoNum, 10);
    readLightSensor(servoNum);
    degree = degree - 10;
    agitate(servoNum, degree);
    delay (waitTimeLong);
    degree = degree + 10;
    agitateBack(servoNum, degree);
    delay (waitTimeLong);
    
    //set back bioswitchmode
    bioSwitchMode = false;
    }
 
  } else if (serialMode) {
    
    if (Serial.read() == '\n') {
      logOutput("inside read");
      if (degree < 10) {
      //agitate without reading
      agitate(servoNum, 10);
      delay(waitTimeShort);
      readLightSensor(servoNum);
      agitateBack(servoNum, degree);
      delay (waitTimeShort);
      //testOutput(servoNum, degree);
    
    } else {
      agitate(servoNum, 10);
      readLightSensor(servoNum);
      degree = degree - 10;
      logOutput("starting ...");
      agitate(servoNum, degree);
      delay (waitTimeLong);
      degree = degree + 10;
      agitateBack(servoNum, degree);
      delay (waitTimeLong);
      //testOutput(servoNum, degree);
      }
    //set back serialMode
    serialMode = false;  
    } //end if
  }
}

void logOutput (String debug) {
  if (logOut) {
    Serial.println(debug);
  }
}

void testOutput(int servoN, int deg) {
  Serial.print(servoN);
  Serial.print(",");
  Serial.print(deg);
  Serial.print('\n');  
}

void readLightSensor(int servoN) {
  servoN--; //starts reading from 0
  /*NO MORE LED
  //we first need to light appropriate LED
  digitalWrite(lightSensor[servoN], HIGH);
  delay(10);*/
  //now we need to read amount of bio light
  int bioLight = 0;
  bioLight = analogRead(servoN);
  logOutput("biolight: " + bioLight);
  //if biolight is too weak
  if (bioLight < minLight || bioLight == 'null') {
    bioLight = (int)random(1023);
  }
  servoN++;
  Serial.print(servoN);
  Serial.print(",");
  Serial.println(bioLight);
  
  /*MO MORE LED
  //we will turn off light
  //servoN--;
  //digitalWrite(lightSensor[servoN], LOW);*/
}
  
  void agitate(int servoN, int deg) {
    logOutput("inside agitate...");
    logOutput(servoN + " " + deg);
    int pos;
  //servos turn independently based on seperate input
  switch (servoN) {
    case 0:
    logOutput("servoN is 0 but we are breaking");
    break;
    case 1:
    logOutput("servoN is 1");
      for(pos = 0; pos < deg; pos += 10) { // goes from 0 degrees to 180 degrees  
        servo1.write(pos);              // tell servo to go to position in variable 'pos'
        delay(15);                       // waits 15ms for the servo to reach the position
      }
      break;
    case 2:
    logOutput("servoN is 2");
      for(pos = 0; pos < deg; pos += 10) { // goes from 0 degrees to 180 degrees  
        servo2.write(pos);              // tell servo to go to position in variable 'pos'
        delay(15);                       // waits 15ms for the servo to reach the position
      }
      break;
    case 3:
      for(pos = 0; pos < deg; pos += 10) { // goes from 0 degrees to 180 degrees  
        servo3.write(pos);              // tell servo to go to position in variable 'pos'
        delay(15);                       // waits 15ms for the servo to reach the position
      }
      break;
    case 4:
      for(pos = 0; pos < deg; pos += 10) { // goes from 0 degrees to 180 degrees  
        servo4.write(pos);              // tell servo to go to position in variable 'pos'
        delay(15);                       // waits 15ms for the servo to reach the position
      }
      break;

    delay(30);
  }
}

void agitateBack (int servoN, int deg) {
  logOutput("done agitating");
  switch (servoN) {
    case 0:
    break;
    case 1:
      for(pos = deg; pos>=0; pos-= 10) {    // goes from 180 degrees to 0 degrees                
      servo1.write(pos);              // tell servo to go to position in variable ‘pos’
      delay(15);                       // waits 15ms for the servo to reach the position
      }
    break;
    case 2:
      for(pos = deg; pos>=0; pos-= 10) {    // goes from 180 degrees to 0 degrees                
      servo2.write(pos);              // tell servo to go to position in variable ‘pos’
      delay(15);                       // waits 15ms for the servo to reach the position
      }
    break;  
    case 3:
      for(pos = deg; pos>=0; pos-= 20) {    // goes from 180 degrees to 0 degrees                
      servo3.write(pos);              // tell servo to go to position in variable ‘pos’
      delay(15);                       // waits 15ms for the servo to reach the position
      }
    break;  
    case 4:
      for(pos = deg; pos>=0; pos-= 20) {    // goes from 180 degrees to 0 degrees                
      servo4.write(pos);              // tell servo to go to position in variable ‘pos’
      delay(15);                       // waits 15ms for the servo to reach the position
      }
    break;  
  }
}

void establishContact() {
  while (Serial.available() <= 0) {
  Serial.println("0000");   // send an initial string
  delay(300);
  }
}
