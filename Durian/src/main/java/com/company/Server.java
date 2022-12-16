package com.company;

import java.io.*;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class Server{


    public static void main(String[] args){


        log("Local Bluetooth device...\n");

        LocalDevice local = null;
        try {

            local = LocalDevice.getLocalDevice();
        } catch (BluetoothStateException e2) {

        }

        log( "address: " + local.getBluetoothAddress() );
        log( "name: " + local.getFriendlyName() );


        Runnable r = new ServerRunable();
        Thread thread = new Thread(r);
        thread.start();

    }


    static void log(String msg) {

        System.out.println("["+(new Date()) + "] " + msg);
    }



}




class ServerRunable implements Runnable{

    //UUID for SPP
    final UUID uuid = new UUID("0000110100001000800000805F9B34FB", false);
    final String CONNECTION_URL_FOR_SPP = "btspp://localhost:"
            + uuid +";name=SPP Server";
    String access_token =null;
    private StreamConnectionNotifier mStreamConnectionNotifier = null;
    private StreamConnection mStreamConnection = null;
    private int count = 0;


    @Override
    public void run() {

        try {

            mStreamConnectionNotifier = (StreamConnectionNotifier) Connector
                    .open(CONNECTION_URL_FOR_SPP);

            log("Opened connection successful.");
        } catch (IOException e) {

            log("Could not open connection: " + e.getMessage());
            return;
        }
        
//=========================================구글 인증 토큰 받기=======================================================

        log("Server is now running.");


        access_token = AccessToken.getAccess_token();//시작할때 미리 인증 값받기
        while(true){

            log("wait for client requests...");


            try {

                mStreamConnection = mStreamConnectionNotifier.acceptAndOpen();
            } catch (IOException e1) {

                log("Could not open connection: " + e1.getMessage() );
            }


            count++;
            log("현재 접속 중인 클라이언트 수: " + count);


            new Receiver(mStreamConnection).start();
        }

    }



    class Receiver extends Thread {

        private InputStream mInputStream = null;
        private OutputStream mOutputStream = null;
        private String mRemoteDeviceString = null;
        private StreamConnection mStreamConnection = null;

        Receiver(StreamConnection streamConnection){

            mStreamConnection = streamConnection;

            try {

                mInputStream = mStreamConnection.openInputStream();
                mOutputStream = mStreamConnection.openOutputStream();

                log("Open streams...");
            } catch (IOException e) {

                log("Couldn't open Stream: " + e.getMessage());

                Thread.currentThread().interrupt();
                return;
            }


            try {

                RemoteDevice remoteDevice
                        = RemoteDevice.getRemoteDevice(mStreamConnection);

                mRemoteDeviceString = remoteDevice.getBluetoothAddress();

                log("Remote device");
                log("address: "+ mRemoteDeviceString);

            } catch (IOException e1) {

                log("Found device, but couldn't connect to it: " + e1.getMessage());
                return;
            }

            log("Client is connected...");
        }


        @Override
        public void run() {

            try {

                Reader mReader = new BufferedReader(new InputStreamReader
                        ( mInputStream, Charset.forName(StandardCharsets.UTF_8.name())));

                boolean isDisconnected = false;


                while(true){
                    StringBuilder stringBuilder = new StringBuilder();
                    int c = 0;


                    while ( '\n' != (char)( c = mReader.read()) ) {

                        if ( c == -1){

                            log("Client has been disconnected");

                            count--;
                            log("현재 접속 중인 클라이언트 수: " + count);

                            isDisconnected = true;
                            Thread.currentThread().interrupt();

                            break;
                        }

                        stringBuilder.append((char) c);
                    }

                    if ( isDisconnected ) break;

                    String recvMessage = stringBuilder.toString();
                    log( mRemoteDeviceString + ": " + recvMessage);


                    //================================데이터 정의 실시=========================================
                    //String url = "http://localhost:8083/gate/open";//구버전
                    int gate_number=13;
                    log("게이트 번호: "+gate_number);
                    String passCode="초기값";
                    String authority="초기값";
                    String user_name="초기값";
                    String fuser_name="초기값";
                    String[] recvMessages = recvMessage.split(" ");
                    //System.out.println("==============================="+recvMessages[0]);//데이터 확인용
                    //System.out.println("==============================="+recvMessages[1]);//데이터 확인용
                    String url2 = "https://mobi-2852c-default-rtdb.firebaseio.com/gatePassID/"+gate_number+"/"+recvMessages[0]+".json";

                    //String data = "userID="+recvMessage+"&gateNumber=11";//구버전
                    access_token = AccessToken.getAccess_token();
                    String data2 = "access_token="+access_token;
                    //메소드 호출 실시

                    //------------------GET요정------------------------------
                    //log("BT_RecvMessage: "+recvMessage);
                    log("[BT_RecvMessage]");
                    String s = httpGetConnection(url2, data2);//




                    //문자열 길이 구하기
                    //int s_len = s.length();//구버전

                    //Timer
                    Timer timer = new Timer();
                    TimerTask task = new TimerTask() { //익명객체로 구현해야한다.

                        @Override
                        public void run() {
                            log("문이 닫혔습니다.");
                            System.out.println("========================");
                            System.out.println("=           |          =");
                            System.out.println("=           |          =");
                            System.out.println("=           |          =");
                            System.out.println("=      []   |   []     =");
                            System.out.println("=           |          =");
                            System.out.println("=           |          =");
                            System.out.println("=           |          =");
                            System.out.println("========================");

                        }
                    };

                    //Gate On/Off
                    if(!s.equals("null"))
                    {
                        String[] array = s.split("\"");
                        //for(int i=0;i<array.length;i++) {System.out.println(i+" : "+array[i]);}//데이터 확인용
                        if(array.length>2){
                            if(array[3].equals("Approval")) {//문자열 같은지 확인

                                if(array.length>11) {
                                    user_name = array[7];
                                    if(array[11].equals(recvMessages[1])){
                                        log("= = 인증 완료 = =");
                                    }else{passCode="pid 오류";}

                                }else{passCode="수신 데이터 오류 : 11";}

                            }else{passCode="문자열 불일치 미승인";}

                        }else {passCode="수신 데이터 오류 : 3";}

                    }else {passCode="수신 데이터 오류 : null";}
                    
                    if(passCode.equals("초기값")){
                        authority="승인";

                    }else{
                        log("비정상적 접근 :"+passCode);
                        authority="거부";
                        String url3 = "https://mobi-2852c-default-rtdb.firebaseio.com/users/"+recvMessages[0]+"/name.json";
                        fuser_name = httpGetConnection(url3, data2);//이름 받아오기
                        //log(fuser_name);
                        if(fuser_name.equals("null")){
                            user_name="미등록자";
                        }else{
                            String[] farray = fuser_name.split("\"");
                            user_name=farray[1];//이름 등록
                        }


                    }


                    String url3 = "https://mobi-2852c-default-rtdb.firebaseio.com/gateLog/.json";//로그 저장 요청 주소

                    String payload = "{\n" +
                            "    \"name\": \""+user_name+"\",\n" +
                            "    \"id\": \""+recvMessages[0]+"\",\n" +
                            "    \"number\": \""+gate_number+"\",\n" +
                            "    \"success\": \""+authority+"\",\n" +
                            "    \"time\": {\".sv\": \"timestamp\"}\n" +
                            "}";

                    String s2 = httpPOSTConnection(url3, data2, payload);//요청구간

                    log("[게이트 번호]: "+gate_number+" / [이름]: "+user_name+" / [id]: "+recvMessages[0]+" / [상태]: "+authority);

                    if(authority.equals("승인")){
                       log("문이 열렸습니다.");
                        System.out.println("========================");
                        System.out.println("=|                    |=");
                        System.out.println("=|                    |=");
                        System.out.println("=|                    |=");
                        System.out.println("=|                    |=");
                        System.out.println("=|                    |=");
                        System.out.println("=|                    |=");
                        System.out.println("=|                    |=");
                        System.out.println("========================");
                        timer.schedule(task, 3000);
                    }else{
                        log("= = 접근거부 = = ");
                   }



                }

            } catch (IOException e) {

                log("Receiver closed" + e.getMessage());
            }
        }
        //--------------------------------------GET통신------------------------------------------------
        public String httpGetConnection(String UrlData, String ParamData) {

            //http 요청 시 url 주소와 파라미터 데이터를 결합하기 위한 변수 선언
            String totalUrl = "";
            if(ParamData != null && ParamData.length() > 0 &&
                    !ParamData.equals("") && !ParamData.contains("null")) { //파라미터 값이 널값이 아닌지 확인
                totalUrl = UrlData.trim().toString()+ "?" + ParamData.trim().toString();
            }
            else {
                totalUrl = UrlData.trim().toString();
            }

            //http 통신을 하기위한 객체 선언 실시
            URL url = null;
            HttpURLConnection conn = null;

            //http 통신 요청 후 응답 받은 데이터를 담기 위한 변수
            String responseData = "";
            BufferedReader br = null;
            StringBuffer sb = null;

            //메소드 호출 결과값을 반환하기 위한 변수
            String returnData = "";

            try {
                //파라미터로 들어온 url을 사용해 connection 실시
                url = new URL(totalUrl);
                conn = (HttpURLConnection) url.openConnection();

                //http 요청에 필요한 타입 정의 실시
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestMethod("GET");

                //http 요청 실시
                conn.connect();

                //http 요청 후 응답 받은 데이터를 버퍼에 쌓는다
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                sb = new StringBuffer();
                while ((responseData = br.readLine()) != null) {
                    sb.append(responseData); //StringBuffer에 응답받은 데이터 순차적으로 저장 실시
                }

                //메소드 호출 완료 시 반환하는 변수에 버퍼 데이터 삽입 실시
                returnData = sb.toString();

                //http 요청 응답 코드 확인 실시
                String responseCode = String.valueOf(conn.getResponseCode());
                log("유저 데이터 확인중");
                //log("GET 응답 데이터 : "+returnData);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //http 요청 및 응답 완료 후 BufferedReader를 닫아줍니다
                try {
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return returnData;
        }
        //----------------------------------------------------------------------------------------------------

        //-------------------------------------POST통신-----------------------------------------------------------
        public String httpPOSTConnection(String UrlData, String ParamData,String payload) {
            
            //http 요청 시 url 주소와 파라미터 데이터를 결합하기 위한 변수 선언
            String totalUrl = "";
            if(ParamData != null && ParamData.length() > 0 &&
                    !ParamData.equals("") && !ParamData.contains("null")) { //파라미터 값이 널값이 아닌지 확인
                totalUrl = UrlData.trim().toString() + "?" + ParamData.trim().toString();
            }
            else {
                totalUrl = UrlData.trim().toString();
            }

            //http 통신을 하기위한 객체 선언 실시
            URL url = null;
            HttpURLConnection conn = null;

            //http 통신 요청 후 응답 받은 데이터를 담기 위한 변수
            String responseData = "";
            BufferedReader br = null;
            StringBuffer sb = null;

            //메소드 호출 결과값을 반환하기 위한 변수
            String returnData = "";

            try {
                //파라미터로 들어온 url을 사용해 connection 실시
                url = new URL(totalUrl);
                conn = (HttpURLConnection) url.openConnection();

                //http 요청에 필요한 타입 정의 실시, 서버 응답을 json타입으로 요청
                conn.setRequestProperty("Accept", "application/json");
                // 타입설정(application/json) 형식으로 전송 (Request Body 전달시 application/json로 서버에 전달.)
                conn.setRequestProperty("Content-Type", "application/json");
                //POST 또는 PUT 요청과 같이 request body를 보내려면 ( output ) true로 설정 필요
                conn.setDoOutput(true);
                //요청방식 선택
                conn.setRequestMethod("POST");
                //request body 내용

                OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(),"UTF-8");
                osw.write(payload);
                osw.flush();
                osw.close();
                //http 요청 실시
                conn.connect();

                //http 요청 후 응답 받은 데이터를 버퍼에 쌓는다
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                sb = new StringBuffer();
                while ((responseData = br.readLine()) != null) {
                    sb.append(responseData); //StringBuffer에 응답받은 데이터 순차적으로 저장 실시
                }

                //메소드 호출 완료 시 반환하는 변수에 버퍼 데이터 삽입 실시
                returnData = sb.toString();

                //http 요청 응답 코드 확인 실시
                String responseCode = String.valueOf(conn.getResponseCode());
                log("gateLog 등록");
                //log("POST 응답 데이터 : "+returnData);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //http 요청 및 응답 완료 후 BufferedReader를 닫아줍니다
                try {
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return returnData;
        }
        //------------------------------------------------------------------------------------------------
    }


    private static void log(String msg) {

        System.out.println("["+(new Date()) + "] " + msg);
    }
}

