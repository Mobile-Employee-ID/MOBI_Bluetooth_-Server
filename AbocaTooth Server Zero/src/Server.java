import java.io.BufferedReader;
import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
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
        }
        catch (BluetoothStateException e2) {
        }

        log( "address: " + local.getBluetoothAddress() );
        log( "name: " + local.getFriendlyName() );

        Runnable r = new ServerRunable();
        Thread thread = new Thread(r);
        thread.start();
    }

    private static void log(String msg) {
        System.out.println("["+(new Date()) + "] " + msg);
    }
}


class ServerRunable implements Runnable{

    //UUID for SPP
    final UUID uuid = new UUID("0000110100001000800000805F9B34FB", false);
    final String CONNECTION_URL_FOR_SPP = "btspp://localhost:"
            + uuid +";name=SPP Server";

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


        log("Server is now running.");



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


                    //데이터 정의 실시
                    String url = "http://192.168.1.182:8083/gate/open";
                    String data = "userID="+recvMessage+"&gateNumber=11";

                    //메소드 호출 실시
                    String s = httpGetConnection(url, data);

                    //문자열 길이 구하기
                    int s_len = s.length();

                    //Timer
                    Timer timer = new Timer();
                    TimerTask task = new TimerTask() { //익명객체로 구현해야한다.
                        @Override
                        public void run() {
                            System.out.println("문이 닫혔습니다.\n");
                        }
                    };


                    //Gate On/Off
                    System.out.println("\n[게이트]");
                    if(s_len > 5) {
                        TimerTask stop = new TimerTask() {
                            int num = 3;
                            @Override
                            public void run() {
                                if(num > 0){
                                    System.out.println(num+".. ");
                                    num--; //실행횟수 증가
                                }
                                else{
                                    timer.cancel(); //타이머 종료
                                }
                            }
                        };
                        timer.schedule(stop, 1000, 1000); //실행 Task, 1초뒤 실행, 1초마다 반복

                        System.out.println("문이 열렸습니다.");
                        timer.schedule(task, 4000);
                    }

                    else
                        System.out.println("No Access\n");
                }

            } catch (IOException e) {

                log("Receiver closed" + e.getMessage());
            }
        }
        public String httpGetConnection(String UrlData, String ParamData) {

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
                System.out.println("");
                System.out.println("http 응답 데이터 : "+returnData);

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
    }

    private static void log(String msg) {
        System.out.println("["+(new Date()) + "] " + msg);
    }
}