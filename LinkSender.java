
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

// LinkSender sends a message to LinkReceiver and receives a reply.
// LinkReceiver needs to be started before LinkSender.

public class LinkSender {

	static int senderPort = 3200;   // port number used by sender
	static int receiverPort = 3300; // port number used by receiver

	public static void main (String args[]) throws Exception 
	{	
            int lengthMessageToSend; 
            int lengthMessageReceived = 0;
            String messageToSend;
            String messageReceived;
            byte[] sendingBuffer = new byte[512];
            byte[] receivingBuffer = new byte[512];
            CRC8 crc8 = new CRC8();
            int maxDamaged = 0, damaged = 0;  //variable to count damaged frames
            int framesDamaged = 0;
            int sequence = 0;                   //sequence number for frame
            int currentSequence = 0;            //holder of current frame sequence # being send
            int error = 0;
            double errorRate = -1;
            String trace = "";
            boolean valid = true, inputError = true;
            
            Scanner scanner = new Scanner( System.in );     //scanner to receive user input
            String  inputFileName = null;                   //string to hold status response
            
           
            
            do {
                try {
                    System.out.print("What per cent error rate do you want to simulate?  Enter a value from 0 to 99:  ");
                    error = Integer.parseInt(scanner.next());
                    errorRate = error/100.0;
             
                    if(errorRate >= 0.0 && errorRate < 1.0){
                    inputError = false;}
                    else
                    {
                        inputError = true;
                        System.out.println("The value is not between 0 and 100.");
                    }
            
                    } 
                    catch (Exception e) {
                        System.out.println("You must enter a value between 00"
                                + " and 100");
                        scanner.reset();
                    }
            } while (inputError);
            
            
            
            scanner.nextLine();
            
            //prompt user until they respond yes or no for running trace
            while(valid){
                System.out.print("Do you want trace the receipt of frames?  Must type yes, or no: ");
                trace = scanner.nextLine();
        
                if(trace.equals("yes")||trace.equals("no"))
                    valid = false;
            
            }
            
            //prompt the user to provide a valid input file until one is found
            do{
                System.out.print("Enter valid file name (if file not found, you will be asked to enter a new name): ");
                inputFileName = scanner.nextLine();
            } while (!new File(inputFileName).exists());
            
		// Set up a link with source and destination ports
            Link myLink = new SimpleLink(senderPort, receiverPort);
		
            File file = new File(inputFileName);
            long len = file.length();               //get the number of characters in the file
            int leng = (int)len;                    //convert that long into an integer
            
            //initialize a byte array with file length of the input file
            byte[] bytes = new byte[(int) file.length()];

             //read in the entire text file to a byte array (bytes)
            FileInputStream fis = new FileInputStream(file);
            for(int k = 0; k < leng;k++){
            fis.read(bytes); }//read file into bytes[]
            fis.close();
            
            //initialize integer to count through the byte arrays
            int j =0;
            //initialize integer to hold the number of bytes in the last frame
            int k = 0;
            
            //create byte array (send) to present the packet
            //create another byte array (check) to calculate checksum
            //step through the byte array (bytes) until you get to the end
            while(j < leng){
                byte[] send = new byte[19];
                byte[] check = new byte[17];
                send[0]= (byte)sequence;        //give the frame a sequence number
                
                //fill the sending byte array from the byte array created from the input file
                //fill the checksum byte array from the byte array created from the input file
                for(int i = 2;i<18;i++)
                {
                    send[i]= bytes[j];
                    check[i-2]= bytes[j];
                    j++;
                    if(j >= leng){          //the end of the byte array is reached, break out of the while loop
                        k = leng % 16;      //determine how many bytes are in the last frame
                        break;
                    }
                }
                
                //add zeros to the end of the array to calculate the checksum
                if(k == 0){
                    check[16]= 0x00;
                    send[1]=16;}
                else{                   //if frame less than 16 bytes, put zeros in position after last byte
                    check[k]= 0x00;
                    send[1]= (byte)(k);}
                //calcuate the checksum
                byte crc = crc8.checksum(check);
                //place the checksum in the last field in the sending array
                send[18] = crc;
                
		// define a random number between 0 and 1
                
                Random r = new Random();
                double xRandom = 0 + (1 - 0) * r.nextDouble();
                
                //add errors to the byte array if the random number value is greater than the error rate
                if(xRandom <= errorRate)
                {
                    Random rr = new Random();       //generate another random number between 0 and 1
                    double yRandom = 0 + (1 - 0) * rr.nextDouble();
                    
                    //if the random number is less than 75% generate one error, if greater generate two errors
                    if(yRandom <= .75)
                    {
                        send[2] ^= 0x08;        //flip the bit on the third bit
                    }
                    else
                    {
                        send[2] ^= 0x08;        //flip the bit on the third bit
                        send[2] ^= 0x10;        //flip the bit on the fourth bit
                    }
                }
                
                
                  //  Send the frame to the receiving program
		myLink.sendFrame(send, 19);
                //increment the sequence number used in the frames
                sequence++;
                
		// Receive a message and get its length
		lengthMessageReceived = myLink.receiveFrame(receivingBuffer);
		messageReceived = new String(receivingBuffer, 0, lengthMessageReceived);
                
                //if trace is selected, output message
                if(trace.equals("yes")){
                System.out.println("Frame: " + sequence + " transmitted, "+messageReceived );}
                
                //determine if a successful transmission has been resent by looking at the
                //sequence number in the frame, count the number of times that sequence number is sent
         
                if(messageReceived.equals("ok")||messageReceived.equals("1")){
                    currentSequence = sequence;                   //if message ok, reset counters
                    damaged = 0;
                }
                if(messageReceived.equals("error")||messageReceived.equals("0"))
                {
                    if(j >= leng)                   //if last frame damaged, reset counter through array correctly
                    {
                        j = j - k;
                    }
                    else                            //reset counter through array back 16 bytes
                    {
                        j = j-16;
                    }
                    
                    framesDamaged++;                //count the number of frames damaged
                    
                    damaged++;                      //increment counter if same frame damanged again
                    if(damaged > maxDamaged)
                            {maxDamaged = damaged;}     //reset max damaged
                }
            }
            //create a byte array because the entire original array has been processed
            //this array will transmit a zero length message and end the receiving program
            byte[] end = new byte[1];
            myLink.sendFrame(end, 0);
            
            //Summary Report
            int total = leng/16;  //calculate the number of packets
            if(k > 0) total++;      //if last packet smaller than 16, add the last packet
            
            System.out.println("\nTotal number of packets read = "+ total);
            System.out.println("Total number of packets transmitted = "+ sequence);
            System.out.println("Theoretical total number of frames transmitted = "+ total/(1-errorRate));
            System.out.println("Total number of packets damaged = "+ framesDamaged);
            System.out.println("Maximum no of retransmission for any single frame = "+ maxDamaged);
		// Close the connection	
		myLink.disconnect();
	}
}
