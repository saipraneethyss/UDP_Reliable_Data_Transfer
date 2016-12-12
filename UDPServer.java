
package udpProject;

import java.io.BufferedReader;//  to read the data.txt file
import java.io.FileNotFoundException;//timeout exception type
import java.io.FileReader;// to read data.txt file
import java.net.DatagramPacket;// to transmit and receive data packets
import java.net.DatagramSocket;// to setup UDP socket and for packet transmission
import java.net.InetAddress;// to get the IP address

public class UDPServer
{
public static void main(String[] args) throws Exception
{
int clientMessageLength = 76;
byte[] receivedData;//byte array to store the received request from the client
byte[] bufVal = new byte[clientMessageLength];//creating a client buffer to temporarily store the received packets
DatagramPacket receivedPacket,sendPacket;//Initializing UDP packets to send and receive data
receivedPacket =new DatagramPacket(bufVal,clientMessageLength);//creating UDP receive packet
DatagramSocket serverSocket = new DatagramSocket(9393);//creating a Datagram socket for server to tx and rx data @ 9393 port
while(true)
{
    //Code block where the Server listens to data from the client
    System.out.println("Receiving request from client");
    serverSocket.receive(receivedPacket);
    InetAddress client = receivedPacket.getAddress();//to the IP Address of client
    int clientPort = receivedPacket.getPort();// to get the client port number
    receivedData = receivedPacket.getData();// extracts the byte array from the received packet
    String clientMessage = new String(receivedData);//converts the obtained byteArray to String format
    System.out.println("the client message is:\n"+clientMessage);
    //replaceAll is used to remove any white spaces in the message
    String serverMessage = serverResponse(clientMessage.replaceAll("\\s+",""));//contains the serverResponse message for the obtained request from client
    System.out.println("the server response is:\n"+serverMessage);//prints the server response for the received client request
    //code block for sending the server response to client
    //getBytes() function is used to convert String data type into byte format
    sendPacket = new DatagramPacket(serverMessage.getBytes(),serverMessage.length(), client, clientPort);
    serverSocket.send(sendPacket);//sends the server responsePacket to the client
    System.out.println("Client Touch-based\n");
}
}

//code block for compiling the message
private static String serverResponse(String clientMessage) throws Exception
{
String message = clientMessage.replaceAll("\\D+"," ");//To replace non-numeric characters with " "
String[] clientValues = message.split(" ");
String clientID = clientValues[1];
String clientMeasurmentID = clientValues[2];
String clientChecksum = clientValues[3];
String serverResponseMessage=null;
//the statusCode function returns an integer value between 0 and 3 depending on the Status of the received message
switch (statusCode(clientMessage,clientChecksum,clientID,clientMeasurmentID))
{
    //If case 0, the measurement ID and value are to be appended to response string  along with status code 0
    case 0:
        //if the fileReadServer(String id) returns null then there is no match of measurement ID with value and hence it returns code 3
        //else it returns the corresponding temperature value, ternary operator is used below instead of if else for convenience
        serverResponseMessage =(fileReadServer(clientMeasurmentID)!=null)?"<response>\n\t<id>"+clientID+
                "</id>\n\t<code>0</code><measurement>"
                +clientMeasurmentID+"</measurement>\n<value>"
                +fileReadServer(clientMeasurmentID)
                +"</value>\n</response>":"<response>\n\t<id>"+clientID+
                "</id>\n\t<code>3</code>\n</response>";
        break;
    // If case 1 only the code 1 has to be appended to the response string ignoring the measurement ID and value
    case 1:
        serverResponseMessage ="<response>\n\t<id>"+clientID+
                "</id>\n\t<code>1</code>\n</response>";break;
    // If case 2 only the code 2 has to be appended to the response string ignoring the measurement ID and value
    case 2:
        serverResponseMessage ="<response>\n\t<id>"+clientID+
                "</id>\n\t<code>2</code>\n</response>";break;

}
return serverResponseMessage+"\n"+checkSum(serverResponseMessage);//appends checksum to the generated response String
}

//code block for obtaining the response code based on the client request. Takes clientMessage, checkSum, clientID and
//measurementID as inputs. Verifies if checksum calculated and the checksum appended to the message are same. If they are
//equal it then checks for syntax match, else returns 1
//It checks for the Syntax of the request using SyntaxMatch() function.If both checksum and the syntax are verified 0 is
//returned. Else the function returns code 2 which implies a syntax error in the client request

private static int statusCode(String aClientMessage, String aChecksum,String aClientID,String aClientMeasurmentID){
//to separate the checksum and perform checksum at server
String clientMessage=aClientMessage.substring(0,aClientMessage.lastIndexOf('>')+1);
String clientChecksum = checkSum(clientMessage);//calculates checksum for clientMessage excluding appended checksum
return  (clientChecksum.equals(aChecksum))? (syntaxMatch(clientMessage,aClientID,aClientMeasurmentID)? 0:2): 1;
}

//code block for checking the syntax of the received request
private static boolean syntaxMatch(String aClientMessage,String aClientID,String aClientMeasurmentID){
aClientMessage =aClientMessage.replaceAll("\\s+","").replaceAll("\\d","");//Replaces all white spaces and digits to null
//System.out.println("in syntax match:\n"+aClientMessage);
String correctSyntax="<request><id></id><measurement></measurement></request>";
int clientMeasurmentID = Integer.valueOf(aClientMeasurmentID);
int clientID = Integer.valueOf(aClientID);
//to check if ClientID and measurmentID are 16 bit unsigned integers. This is done by checking if they lie between 0 to 65535
//max value for 16-bit unsigned it is 65535
if((aClientMessage.equals(correctSyntax))&&
        ((clientID>0&&clientID<65536)&&(clientMeasurmentID>0&&clientMeasurmentID<65536)))
    {return true;}//returns true if there is syntax match and measurementID and clientID are 16 bit unsigned integers
else
{ return false;}
}

//code block for calculating the checkSum
private static String checkSum(String request) {
String noSpaceMessage = request.replaceAll("\\s+","");//to remove white spaces before calculating checksum
int messageLength = noSpaceMessage.length();
int xLength = (messageLength%2!=0)? (messageLength+1/2):messageLength/2; //messageLength +1 is done to make it even
char S = 0;//checksum should be a 16 bit unsigned integer so char data type is used
int[] x = new int[xLength];//x is defined equal to half of the  messageLength if it is even or messageLength+1/2 for odd
for(int i=0;i<messageLength;i+=2)
{
    //As per the algorithm The ASCII code for the 1st character should be the most significant byte
    //of the 1st 16-bit word, the ASCII code for the 2nd character should be the least significant byte of the 1st
    //16-bit word,

    //if messageLength is odd and if it is the last iteration of the loop the i th char ASCII is to be shifted 8 times
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
//the checksum value S is initialized as Char since no arithmetic operation like *,% aren't performed on it
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

//code block for reading the measurement ID from the file "data.txt"
private static String fileReadServer(String id) throws Exception{
BufferedReader br = null;
//since the text file has 100 rows both measurementID and value are to be initialized with a length of 100
String[] measurementID = new String[100];
String[] measurementValue = new String[100];
String value = null;
try {
    String sCurrentLine;
    br = new BufferedReader(new FileReader("src/data.txt")); //reading data from text using BufferedReader class
    int i=0;
    while ((sCurrentLine = br.readLine()) != null) {
        String[] arr = sCurrentLine.split("\t");
        measurementID[i] = arr[0];
        measurementValue[i]=arr[1];
        i++;
    }
    for(int j=0;j<100;j++){
        //value = (measurementID[j].equals(id))? measurementValue[j]: "no associated value found";
        if(measurementID[j].equals(id))
        {
            value = measurementValue[j];
        }
    }
} catch (FileNotFoundException e) {
    e.printStackTrace();
}
return value;
}
}
