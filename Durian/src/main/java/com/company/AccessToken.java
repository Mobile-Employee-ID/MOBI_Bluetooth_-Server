package com.company;

import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

public class AccessToken {
    static  String getAccess_token(){
        Server.log("[Firebase access_token authentication] - [Start]");
        String tokenValue = null;
        Date tokenExpTime;
        FileInputStream serviceAccount = null;
        try {
            //--인증 json 파일(service account key JSON file) 불러오기--------------
            //개발툴
            //serviceAccount = new FileInputStream("c:/REST/mobi-2852c-firebase-adminsdk-qn9na-96c9db4d9a.json");
            serviceAccount = new FileInputStream("./src/main/java/com/company/mobi-2852c-firebase-adminsdk-qn9na-96c9db4d9a.json");
            //배포판
            //serviceAccount = new FileInputStream("./mobi-2852c-firebase-adminsdk-qn9na-96c9db4d9a.json");

            Server.log("-Load the service account key JSON file");
            //--서비스 계정으로 구글 증명 인증하기------------------

            GoogleCredentials googleCred = GoogleCredentials.fromStream(serviceAccount);
            Server.log("-Authenticate a Google credential with the service account");
            /*
            신버전 : https://github.com/googleapis/google-auth-library-java
            왜되는지 모르겠는데 GoogleCredential->GoogleCredentials로 바꾸도 작동함
            google-auth-library-java 사용함
            GoogleCredential 지원 끊김
            */

            //--Google 인증 정보에 필요한 범위 추가-----------------
            GoogleCredentials scoped = googleCred.createScoped(
                    Arrays.asList(
                            "https://www.googleapis.com/auth/firebase.database",
                            "https://www.googleapis.com/auth/userinfo.email"
                    ));
            Server.log("-Add the required scopes to the Google credential");

            //--Google 자격 증명을 사용하여 액세스 토큰 생성----------
            com.google.auth.oauth2.AccessToken token = scoped.refreshAccessToken();
            Server.log("-Use the Google credential to generate an access token");
            /*
            토큰 형식하고 받는 방식이 바뀜
            구버전 : https://firebase.google.com/docs/database/rest/auth?hl=ko#java
            scoped.refreshToken();
            String token = scoped.getAccessToken();

            신버전 : https://github.com/googleapis/google-auth-library-java
            AccessToken token = credentials.getAccessToken();
            OR
            AccessToken token = credentials.refreshAccessToken();<-- 이거사용함
            받으면
            json형식으로 나오는데
            access_token값이 tokenValue임
            .json?access_token=(tokenValue 값)
            이런식으로 먹이면 됨
            출처 :https://firebase.google.com/docs/database/rest/auth?hl=ko#java
             */
            tokenValue = token.getTokenValue();
            tokenExpTime = token.getExpirationTime();
            Server.log("-tokenExpTime: "+tokenExpTime);
            //Server.log("-tokenValue: "+tokenValue);
            Server.log("[Firebase access_token authentication] - [Completed]");
        } catch (FileNotFoundException ex) {
            Server.log("[Firebase access_token authentication] - [File error]");
        } catch (IOException e) {
            e.printStackTrace();
            Server.log("[Firebase access_token authentication] - [Unknown error]");
        }


        return tokenValue;
    }
}

