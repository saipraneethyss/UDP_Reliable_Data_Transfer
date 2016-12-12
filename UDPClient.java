package udpProject;

import java.io.BufferedReader;//  to read the data.txt file
import java.io.FileNotFoundException;//timeout exception type
import java.io.FileReader;// to read data.txt file
import java.net.DatagramPacket;// to transmit and receive data packets
import java.net.DatagramSocket;// to setup UDP socket and for packet transmission
import java.net.InetAddress;// to get the IP address
import java.util.Random; //to get a random number
import java.util.Scanner;// to get user input to know whether to retransmit or not

public class UDPClient
{
    private static int TIME_OUT_VALUE = 1000, TIME_OUT_COUNT = 0;
    public static void main(String[] args) throws Exception{
        while(true) {
            Random num = new Random();
            //generates a random integer value between 0 and 65535(range of unsigned 16-bit integer)
            //Setting the timeout value to 1 seconds. Count for the number of timeouts is stored in timeoutCount
            int requestId = num.nextInt(65535);
            //selecting a random measurementID from the data.txt file;
            String measurementId = fileReadClient();// comment this line and uncomment next line if Error Code3 is to be displayed
            //String measurementId = "300";
            //generates the request message by appending measurementId and requestId to the specified message format
            String requestMessage = clientRequest(measurementId, Integer.toString(requestId));
            int clientMessageLength = requestMessage.length();
            //converting String into byte array Stream to transmit data
            byte[] clientByteArray = requestMessage.getBytes();
            int serverPort = 9393;
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress server = InetAddress.getLocalHost(); //IP address of the SERVER Machine(Same Laptop).
            //Creating a DatagramPacket to send the clientRequest packet to the server
            DatagramPacket D1 = new DatagramPacket(clientByteArray, clientMessageLength, server, serverPort);
            //the client datagram socket is setup for initiating the request transmission
            int serverMessageLength = 113;
            boolean resend = false;
            //code block for receiving the server response
            byte[] receivedMessage = new byte[serverMessageLength];
            DatagramPacket dReceived = new DatagramPacket(receivedMessage, serverMessageLength);

            for (; TIME_OUT_COUNT <= 4; TIME_OUT_COUNT++) {
                if(resend){
                    System.out.println("sending data again"); //resend if the user chooses "y" when prompted to resend on Code 1 error
                }else{if(TIME_OUT_COUNT!=4){                   //to print the transmitted request on the screen
                    System.out.println("client request message:\n"+requestMessage);
                    clientSocket.send(D1);
                    System.out.println("Sending request to Sever");}}
                //code block for reception from server
                //condition to check the number of timeout events occurring. If it is less than 4
                // packet is to be resent again and count is incremented along with doubling timeout interval
                if (TIME_OUT_COUNT < 4) {
                    try {
                        clientSocket.setSoTimeout(TIME_OUT_VALUE);
                        System.out.println("Receiving response from the server\n");
                        clientSocket.receive(dReceived);
                        receivedMessage = dReceived.getData();//extracting the data from packet to a byte array
                        String serverMessage = new String(receivedMessage);//converting byte array to string
                        resend = responseCompare(serverMessage);//returns boolean true if user wants to
                        //send the packet which has code 1 error during client reception again
                        if (resend) {
                            int requestId2 = num.nextInt(65535);//to extend upto max value of 16-bit unsigned integer
                            String newRequestID = String.format("%5s", Integer.toString(requestId2)).replace(' ', '0');
                            String measurementID = String.format("%5s", (measurementId).replace(' ', '0'));
                            requestMessage = clientRequest(measurementID, newRequestID);//generating a random requestID for retransmission
                            clientMessageLength = requestMessage.length();
                            System.out.println("the request Message is:\n"+requestMessage);
                            //converting into byte array Stream
                            byte[] clientByteArray2 = requestMessage.getBytes();///converting string to byte array for transmission
                            //re-creating data packet to send request to the server based on user-response
                            DatagramPacket D2 = new DatagramPacket(clientByteArray2, clientMessageLength, server, serverPort);
                            clientSocket.send(D2);
                            //since this retransmission is not because of any time out event, we have to reset the time out count to 0
                            //otherwise if this packet doesn't receive any response it would be sent only 3 times instead 4 as per the protocol
                            TIME_OUT_COUNT--;
                            continue;
                        }
                        else{break;}
                    } catch (java.io.InterruptedIOException ex) { //throws exception on the event of timeout
                        TIME_OUT_VALUE = TIME_OUT_VALUE * 2;
                        System.out.println((TIME_OUT_COUNT+1) + "th transmission ends\n======================\n"); //prints the count of transmission
                    }
                } else {
                    System.out.println("Client Socket Communication Failure ");
                    //code block for reception from server
                    TIME_OUT_COUNT = 0;//resets the timeout count
                    TIME_OUT_VALUE = 1000;//re-initialises the timeout to 1 second again
                    System.exit(0);//exits the system
                }
            }
            clientSocket.close();//Socket Closed
        }//while(true) closes here
    }//main ends here

    //code block for reading the measurement ID from the file "data.txt" and returning it as a String
    private static String fileReadClient() throws Exception{
        BufferedReader br;
        String[] measurementID = new String[100];
        Random num = new Random();
        int measurementIdIndex = num.nextInt(100);
        try {
            //read the data from file and split the data based on column
            String sCurrentLine;
            br = new BufferedReader(new FileReader("src/data.txt"));
            int i=0;
            while ((sCurrentLine = br.readLine()) != null) {
                String[] arr = sCurrentLine.split("\t");
                measurementID[i] = arr[0];
                i++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return measurementID[measurementIdIndex];
    }
    //Code block for fieldReadClient() ends here

    //code block for compiling the message. Appends measurementId and RequestId to the client request formatted string
    private static String clientRequest(String aMeasurementId,String aRequestId )
    {

        //String clientReadableString = "<request>\n\t<id>"+aRequestId+"</id>\n\t<measurement>"+aMeasurementId+"</measurement>\n</request>";
        //comment the above line and uncomment the below line for sending the wrong syntax --> this gives error code 2 output
        String clientReadableString = "<request>\n\t<id>"+aRequestId+"</id>\n\t<measurement>"+aMeasurementId+"\n</request>";
        return clientReadableString+"\n"+checkSum(clientReadableString);
    }

    //code block for displaying the status of the received message
    private static boolean responseCompare(String responseMessage)
    {
        String noChecksumResponse = responseMessage.substring(0,responseMessage.lastIndexOf('>')+1);
        String responseChecksum = responseMessage.substring(responseMessage.lastIndexOf('>')+2,responseMessage.length());
        responseChecksum = responseChecksum.replaceAll("\\D+","");//to obtain the checksum value and replacing anything expect numbers with null
        //if the calculated checksum of the response equals the appended checksum display status else the corresponding packet should be formed and sent again
        if((checkSum(noChecksumResponse)).equals(responseChecksum))
        {
            int code  = Integer.valueOf(getCode(noChecksumResponse).substring(0,getCode(noChecksumResponse).lastIndexOf('|')));
            switch (code)
            {
                case 0:
                    System.out.println("the temp value is: "
                            +getCode(noChecksumResponse).substring(getCode(noChecksumResponse).lastIndexOf('|')+1,getCode(noChecksumResponse).length())
                            +"\n==============================\n");
                    break;// lastIndexOf is used here to separate the code value and temperature value using the delimiter "|"
                case 1:
                    System.out.println("Error: " +
                            "integrity check failure. The request has one or more bit errors." +
                            " Do you want to re-send the message?(Yes = y/No= n): ");
                    Scanner data = new Scanner(System.in);
                    char userInput = data.next().charAt(0);//obtain user input to whether to retransmit or not
                    if(userInput=='y'){return true;}
                    else{System.exit(0);}
                case 2://error code 2 message
                    System.out.println("Error: malformed request. The syntax of the request message is not correct.");break;
                case 3: //error code 3 message
                    System.out.println("Error: non-existent measurement. The measurement with the requested measurement ID does not exist.");break;
            }
            return false;
        }
        else{return true;}
    }

    //code block for calculating the checkSum
    private static String checkSum(String request) {
        String noSpaceMessage = request.replaceAll("\\s+","");//to remove whitespaces before calculating checksum
        int messageLength = noSpaceMessage.length();
        int xLength = (messageLength%2!=0)? (messageLength+1/2):messageLength/2; //messageLength +1 is done to make it even
        char S = 0;//checksum should be a 16 bit unsigned integer so char data type is used
        int[] x = new int[xLength];//x is defined equal to half of the  messageLength if it is even or messageLength+1/2 for odd
        for(int i=0;i<messageLength;i+=2)
        {
            //As per the algorithm The ASCII code for the 1st character should be the most significant byte
            //of the 1st 16-bit word, the ASCII code for the 2nd character should be the least significant byte of the 1st
            //16-bit word,

            //if messageLength is odd and if it is the last iteration of the loop the ith char ASCII is to be shifted 8 times
            //leaving the Least Significant Bytes to zero
            //else the ith char ASCII is made Most Significant Bytes by shifting it by 8 times and the i+1th char ASCII value is
            //made the Least Significant Bytes
            x[i/2] = ((messageLength%2!=0)&&(i==messageLength-1))?
                    (Integer.parseInt(sixteenBitWord(noSpaceMessage.charAt(i)<<8),2)):
                    (Integer.parseInt(sixteenBitWord(noSpaceMessage.charAt(i)<<8),2))^(Integer.parseInt(sixteenBitWord(noSpaceMessage.charAt(i+1)),2));
            //x[i/2 is converted to 16 bit word format
            x[i/2] = Integer.parseInt(sixteenBitWord(x[i/2]),2);
        }
        //index is defined as int as we perform arithmetic operations and the 16 bit unsigned data type char can't be used for this
        //hence after the arithmetic operations the sign bit is removed by performing logical and with 0x0000ffff;
        //the checksum value S is initialised as Char since no arithmetic operation like *,% aren't performed on it
        for (int t:x) {int index = ((S^t)&0x0000ffff);S = (char)((7919*index)%65536);}
        //Converting Char to string so as to append it in the transmitting message
        return Integer.valueOf(S).toString();//return String representation of S i.e CheckSum
    }
    //code block that converts a integer input to a 16 bit word format by
    //using toBinaryString and replacing all spaces with "0"
    private static String sixteenBitWord(int input)
    {
        return String.format("%16s", Integer.toBinaryString(input)).replace(' ', '0');
    }
    //code block for Checksum ends

    //code block that returns the "code" from the received response
    private static String getCode(String aString){
        String temperature = null;
        aString = aString.replaceAll("\\D+"," ");//replacing all non-digits with " "
        String[] clientValues = aString.split(" ");
        String code = clientValues[2];
        //since the temperature value is decimal value the "." is replaced by " " too. To reconstruct the temperature value, the last two array values are
        //concatenated using "."
        if(clientValues.length==6){temperature = clientValues[4]+"."+clientValues[5];} //if is used because the response might not contain temperature value
        return code+"|"+temperature;
    }


}//UDPServer class ends here

