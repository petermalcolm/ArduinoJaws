#define CHANNEL_A_PIN 0

void setup()
{
  Serial.begin(115200);
  // NOTE: try INTERNAL referencing for better resolution
  //
  // analogReference(DEFAULT);
  // analogReference(INTERNAL);
  // DEFAULT: the default analog reference of 5 volts 
  //     (on 5V Arduino boards) or 3.3 volts (on 3.3V 
  //     Arduino boards) 
  // INTERNAL: an built-in reference, equal to 1.1 
  //     volts on the ATmega168 or ATmega328 and 2.56 
  //     volts on the ATmega8 (not available on the 
  //     Arduino Mega) 
}

void loop()
{
  int value =
    analogRead(CHANNEL_A_PIN);
  // value = value & 0xFF;
  // value = (value >> 2) & 0xFF;  // Evil Genius says >> 2
  // value = (value >> 3) & 0xFF;
  value = ((value >> 3) - 64) & 0xFF;
  
  Serial.write(value);
  // delayMicroseconds(200);
  delayMicroseconds(23);  // sampling rate ~= 44KH
  // delay(1);  // milliseconds
}
