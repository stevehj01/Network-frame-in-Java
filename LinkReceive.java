
import java.io.FileOutputStream;
import java.util.Scanner;
// LinkReceiver receives a message from LinkSender and replies.
// LinkReceiver needs to be started before LinkSender.

public class LinkReceive {
    
    
    static int senderPort = 3200;   // port number used by sender
    static int receiverPort = 3300; // port number used by receiver

    public static void main(String args[]) throws Exception {
        CRC8 crc8 = new CRC8();                 //object to hold checksum
        int lengthMessageToSend;
        int lengthMessageReceived =1, l=0;
        String messageToSend;
        String messageReceived;
        byte[] sendingBuffer = new byte[512];
        byte[] receivingBuffer = new byte[512];
        int sequence = 0;                       //counter for number of frames received
        boolean valid = true;                           
        
        Scanner user = new Scanner( System.in );        //scanner to receive user input
        String  outputFileName, trace ="";              //string to hold status response
        
        
        //prompt user until they respond yes or no for running trace
        while(valid){
        System.out.print("Do you want trace the receipt of frames?  Must type yes, or no: ");
        trace = user.nextLine();
        
        if(trace.equals("yes")||trace.equals("no"))
            valid = false;
            
        }

            // prompt user for the output file name 
            System.out.print("Output File Name: ");
            outputFileName = user.nextLine().trim();
       

        // Set up a link with source and destination ports
        // Any 4-digit number greater than 3000 should be fine. 
        Link myLink = new SimpleLink(receiverPort, senderPort);
        
        //run the receiving program until a message of zero length is received
        
        while (lengthMessageReceived > 0) {
            // Receive a message
            lengthMessageReceived = myLink.receiveFrame(receivingBuffer);
            
            l = lengthMessageReceived;      //if length is 0 break out of while loop
            if(l == 0)break;
       
            byte[] check = new byte[17];    //create byte array called check to calculate checksum
                
                for(int j = 2;j<19;j++)                 //add 16 bytes to the array, j counts through the array
                {
                    check[j-2]= receivingBuffer[j];
                }
     
            //calculate the checksum of the byte array 
         
            byte crc1 = crc8.checksum(check);
            
            //take the data in the frame received and translate it into a string
            messageReceived = new String(receivingBuffer, 2, lengthMessageReceived-3);
            
            //if the checksum is 0, the frame is ok, write the string to the output file, send a reply its ok
            if(crc1 == 0){
                FileOutputStream fos = new FileOutputStream(outputFileName, true);
                fos.write(messageReceived.getBytes());
                fos.close();
                // Prepare a message
                if(trace.equals("yes")){
                    messageToSend = "ok";}
                else {messageToSend = "1";}
            }
            else{
                if(trace.equals("yes")){
                    messageToSend = "error";}
                else {messageToSend = "0";}
                       //checksum indicates the frame is damaged, send a reply its in error    
            }
                //set parameters to support message transmission back to sender
            lengthMessageToSend = messageToSend.length();
            sendingBuffer = messageToSend.getBytes();
                //output results if trace is activated
            if(trace.equals("yes")){
            System.out.println("Frame: " + sequence + " received, "+messageToSend ); }  
            
            //increment count for frames received 
            sequence++;

            // Send the message
            myLink.sendFrame(sendingBuffer, lengthMessageToSend);

            // Close the connection
        }
        myLink.disconnect();
    }
}
